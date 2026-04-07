package org.fdroid.updates

import android.content.Context
import android.content.pm.PackageInfo
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.fdroid.NotificationManager
import org.fdroid.database.AppCheckResult
import org.fdroid.database.AppIssue
import org.fdroid.database.AppOverviewItem
import org.fdroid.database.AppVersion
import org.fdroid.database.AvailableAppWithIssue
import org.fdroid.database.DbAppChecker
import org.fdroid.database.KnownVulnerability
import org.fdroid.database.NoCompatibleSigner
import org.fdroid.database.NotAvailable
import org.fdroid.database.Repository
import org.fdroid.database.UnavailableAppWithIssue
import org.fdroid.database.UpdatableApp
import org.fdroid.database.UpdateInOtherRepo
import org.fdroid.download.PackageName
import org.fdroid.index.RepoManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.settings.SettingsManager
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val PACKAGE_NAME = "our.package.name"

@OptIn(ExperimentalCoroutinesApi::class)
internal class UpdatesManagerTest {

  private val context: Context = mockk(relaxed = true)
  private val dbAppChecker: DbAppChecker = mockk()
  private val settingsManager: SettingsManager = mockk()
  private val repoManager: RepoManager = mockk()
  private val installedAppsCache: InstalledAppsCache = mockk()
  private val notificationManager: NotificationManager = mockk()
  private val updateInstaller: UpdateInstaller = mockk(relaxed = true)

  private val testScope = TestScope()
  private val installedAppsFlow = MutableStateFlow<Map<String, PackageInfo>>(emptyMap())
  private val scope =
    CoroutineScope(
      testScope.backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScope.testScheduler)
    )

  @Before
  fun setUp() {
    // Mock calls into WorkManager which are used at UpdatesManager construction
    mockkObject(RepoUpdateWorker)
    every { RepoUpdateWorker.getAutoUpdateWorkInfo(any()) } returns flowOf(null)
    mockkObject(AppUpdateWorker)
    every { AppUpdateWorker.getAutoUpdateWorkInfo(any()) } returns flowOf(null)

    every { installedAppsCache.installedApps } returns installedAppsFlow
    every { context.packageName } returns PACKAGE_NAME
  }

  @After
  fun tearDown() {
    unmockkObject(RepoUpdateWorker)
    unmockkObject(AppUpdateWorker)
  }

  private fun createUpdatesManager(): UpdatesManager {
    return UpdatesManager(
      context = context,
      dbAppChecker = dbAppChecker,
      settingsManager = settingsManager,
      repoManager = repoManager,
      installedAppsCache = installedAppsCache,
      notificationManager = notificationManager,
      updateInstaller = updateInstaller,
      coroutineScope = scope,
    )
  }

  @Test
  fun `init collects installedApps, skips empty and re-loads on new installed apps`() =
    testScope.runTest {
      mockLoadUpdates()
      val updatesManager = createUpdatesManager()

      // initial empty value must not reach dbAppChecker
      verify(exactly = 0) { dbAppChecker.getApps(any()) }

      val installedApps1 = installedApps("app1")
      installedAppsFlow.value = installedApps1
      advanceUntilIdle()
      advanceTimeBy( 2000)
      verify(exactly = 1) { dbAppChecker.getApps(installedApps1) }
      assertNotNull(updatesManager.updates.value) // populated after first real emission

      val installedApps2 = installedApps("app2")
      installedAppsFlow.value = installedApps2
      advanceUntilIdle()
      verifyOrder {
        dbAppChecker.getApps(installedApps1)
        dbAppChecker.getApps(installedApps2)
      }
    }

  @Test
  fun `loadUpdates skips empty map, then loads updates and updates numUpdates`() =
    testScope.runTest {
      val updatesManager = createUpdatesManager()

      // empty map, no installed apps, yet, so return early and assert flows are unchanged
      updatesManager.loadUpdates(emptyMap())
      advanceUntilIdle()
      assertNull(updatesManager.updates.value)
      verify(exactly = 0) { dbAppChecker.getApps(any()) }

      // now we have one installed app that has an update
      val repo = makeRepository()
      val ver = makeAppVersion(versionName = "3.5.1", versionCode = 351)
      val u = makeUpdatableApp(update = ver, installedVersionName = "2.1.0")
      mockLoadUpdates(AppCheckResult(listOf(u), emptyList()))
      every { repoManager.getRepository(1L) } returns repo
      updatesManager.loadUpdates(installedApps("com.example.app"))
      advanceUntilIdle()
      updatesManager.updates.test {
        val list = awaitNonNull()
        assertEquals(1, list.size)
        val item = list[0]
        assertEquals("com.example.app", item.packageName)
        assertEquals("Example App", item.name)
        assertEquals("2.1.0", item.installedVersionName)
        assertEquals(1L, item.repoId)
        assertEquals("3.5.1", item.update.versionName)
        assertEquals(351L, item.update.versionCode)
      }
      // one update available, so numUpdates is 1
      assertEquals(1, updatesManager.numUpdates.value)
    }

  @Test
  fun `loadUpdates with null name, repo and trimmed whatsNew`() =
    testScope.runTest {
      every { repoManager.getRepository(1L) } returns makeRepository()

      // null name used "Unknown app"
      mockLoadUpdates(AppCheckResult(listOf(makeUpdatableApp(name = null)), emptyList()))
      createUpdatesManager().also { updatesManager ->
        updatesManager.loadUpdates(installedApps("com.example.app"))
        advanceUntilIdle()
        updatesManager.updates.test { assertEquals("Unknown app", awaitNonNull()[0].name) }
      }

      // whatsNew gets trimmed and null whatsNew stays null
      val trimmedVer = makeAppVersion().also { every { it.getWhatsNew(any()) } returns "  foo  " }
      val nullVer = makeAppVersion().also { every { it.getWhatsNew(any()) } returns null }
      mockLoadUpdates(
        AppCheckResult(
          listOf(
            makeUpdatableApp(packageName = "pkg.a", update = trimmedVer),
            makeUpdatableApp(packageName = "pkg.b", update = nullVer),
          ),
          emptyList(),
        )
      )
      createUpdatesManager().also { updatesManager ->
        updatesManager.loadUpdates(
          mapOf("pkg.a" to makePackageInfo("pkg.a"), "pkg.b" to makePackageInfo("pkg.b"))
        )
        advanceUntilIdle()
        updatesManager.updates.test {
          val items = awaitNonNull()
          assertEquals("foo", items.first { it.packageName == "pkg.a" }.whatsNew)
          assertNull(items.first { it.packageName == "pkg.b" }.whatsNew)
        }
      }

      // null repo gets handled
      mockLoadUpdates(AppCheckResult(listOf(makeUpdatableApp()), emptyList()))
      every { repoManager.getRepository(1L) } returns null
      createUpdatesManager().also { updatesManager ->
        updatesManager.loadUpdates(installedApps("com.example.app"))
        advanceUntilIdle()
        updatesManager.updates.test {
          // no DownloadRequest as fallback, since repo is null
          assertNull((awaitNonNull()[0].iconModel as PackageName).iconDownloadRequest)
        }
      }
    }

  @Test
  fun `loadUpdates uses installedAppsCache by default and updates state on later calls`() =
    testScope.runTest {
      every { repoManager.getRepository(1L) } returns makeRepository()

      mockLoadUpdates(
        AppCheckResult(listOf(makeUpdatableApp(packageName = "a1", name = "A1")), emptyList())
      )
      installedAppsFlow.value = installedApps("a1")
      val updatesManager = createUpdatesManager()
      updatesManager.loadUpdates() // uses installedAppsCache.installedApps.value
      advanceUntilIdle()
      assertEquals("A1", updatesManager.updates.value!![0].name)
      assertEquals(1, updatesManager.numUpdates.value)

      // second call replaces first
      mockLoadUpdates(
        AppCheckResult(listOf(makeUpdatableApp(packageName = "a2", name = "A2")), emptyList())
      )
      updatesManager.loadUpdates(installedApps("a2"))
      advanceUntilIdle()
      assertEquals("A2", updatesManager.updates.value!![0].name)

      // third call produces empty list
      mockLoadUpdates()
      updatesManager.loadUpdates(installedApps("a2"))
      advanceUntilIdle()
      assertTrue(updatesManager.updates.value!!.isEmpty())
      assertEquals(0, updatesManager.numUpdates.value)
    }

  @Test
  fun `loadUpdates updates notification or cancels when empty`() =
    testScope.runTest {
      every { repoManager.getRepository(1L) } returns makeRepository()

      // notification is showing, but we have no (more) updates, so cancel notification
      mockLoadUpdates(isNotificationShowing = true)
      every { notificationManager.cancelAppUpdatesAvailableNotification() } just runs
      val updatesManager = createUpdatesManager()
      updatesManager.loadUpdates(installedApps("app"))
      advanceUntilIdle()
      verify(exactly = 1) { notificationManager.cancelAppUpdatesAvailableNotification() }
      verify(exactly = 0) { notificationManager.showAppUpdatesAvailableNotification(any()) }

      // notification is showing, and we have updates, so update notification
      val ver = makeAppVersion(versionName = "2.5")
      val u = makeUpdatableApp(name = "My App", installedVersionName = "1.0", update = ver)
      mockLoadUpdates(AppCheckResult(listOf(u), emptyList()), isNotificationShowing = true)
      every { notificationManager.showAppUpdatesAvailableNotification(any()) } just runs
      updatesManager.loadUpdates(installedApps("com.example.app"))
      advanceUntilIdle()
      verify(exactly = 1) { notificationManager.showAppUpdatesAvailableNotification(any()) }
      updatesManager.notificationStates.getBigText().let { bigText ->
        assertTrue(bigText.contains("My App"))
        assertTrue(bigText.contains("1.0"))
        assertTrue(bigText.contains("2.5"))
      }

      // notification is not showing, so we don't call into NotificationManager
      mockLoadUpdates(
        AppCheckResult(listOf(makeUpdatableApp()), emptyList()),
        isNotificationShowing = false,
      )
      updatesManager.loadUpdates(installedApps("com.example.app"))
      advanceUntilIdle()
      verify(exactly = 1) { // still 1 from above
        notificationManager.showAppUpdatesAvailableNotification(any())
      }
      verify(exactly = 1) { // still 1 from above
        notificationManager.cancelAppUpdatesAvailableNotification()
      }
    }

  @Test
  fun `loadUpdates finds app issue`() =
    testScope.runTest {
      // full field mapping
      val issue = KnownVulnerability(fromPreferredRepo = true)
      val overview = makeAppOverviewItem(lastUpdated = 42L, name = "Vulnerable App")
      mockLoadUpdates(
        AppCheckResult(emptyList(), listOf(AvailableAppWithIssue(overview, "1.0", 10, issue)))
      )
      every { repoManager.getRepository(1L) } returns makeRepository()
      createUpdatesManager().also { updatesManager ->
        updatesManager.loadUpdates(installedApps("com.example.app"))
        advanceUntilIdle()
        updatesManager.appsWithIssues.test {
          val item = awaitNonNull()[0]
          assertEquals("com.example.app", item.packageName)
          assertEquals("Vulnerable App", item.name)
          assertEquals("1.0", item.installedVersionName)
          assertEquals(10L, item.installedVersionCode)
          assertEquals(issue, item.issue)
          assertEquals(42L, item.lastUpdated)
          assertNull((item.iconModel as PackageName).iconDownloadRequest)
        }
      }
    }

  @Test
  fun `loadUpdates finds UnavailableAppWithIssue`() =
    testScope.runTest {
      mockLoadUpdates(
        AppCheckResult(
          emptyList(),
          listOf(UnavailableAppWithIssue("com.removed", "Removed App", "3.0", 30)),
        )
      )
      createUpdatesManager().also { updatesManager ->
        updatesManager.loadUpdates(installedApps("com.removed"))
        advanceUntilIdle()
        updatesManager.appsWithIssues.test {
          val item = awaitNonNull()[0]
          assertEquals("com.removed", item.packageName)
          assertEquals("Removed App", item.name)
          assertEquals("3.0", item.installedVersionName)
          assertEquals(30L, item.installedVersionCode)
          assertEquals(NotAvailable, item.issue)
          assertEquals(-1L, item.lastUpdated)
          assertNull((item.iconModel as PackageName).iconDownloadRequest)
        }
      }
    }

  @Test
  fun `loadUpdates maps all AppIssue subtypes correctly`() =
    testScope.runTest {
      every { repoManager.getRepository(1L) } returns null
      data class Case(val issue: AppIssue, val pkg: String)
      listOf(
          Case(KnownVulnerability(fromPreferredRepo = false), "pkg.vuln"),
          Case(NoCompatibleSigner(repoIdWithCompatibleSigner = 2L), "pkg.signer"),
          Case(UpdateInOtherRepo(repoIdWithUpdate = 5L), "pkg.other"),
        )
        .forEach { (issue, packageName) ->
          mockLoadUpdates(
            AppCheckResult(
              emptyList(),
              listOf(
                AvailableAppWithIssue(
                  makeAppOverviewItem(packageName = packageName),
                  "1.0",
                  10,
                  issue,
                )
              ),
            )
          )
          createUpdatesManager().also { updatesManager ->
            updatesManager.loadUpdates(installedApps(packageName))
            advanceUntilIdle()
            updatesManager.appsWithIssues.test { assertEquals(issue, awaitNonNull()[0].issue) }
          }
        }
    }

  @Test
  fun `loadUpdates doesn't return ignored issues and no issues when still firstStart`() =
    testScope.runTest {
      val kept = UnavailableAppWithIssue("com.kept", "K", "1.0", 10)
      val ign1 = UnavailableAppWithIssue("com.ign1", "I1", "1.0", 10)
      val ign2 = UnavailableAppWithIssue("com.ign2", "I2", "1.0", 10)

      // two apps are ignored, so the result only has one
      mockLoadUpdates(
        AppCheckResult(emptyList(), listOf(ign1, kept, ign2)),
        ignoredAppIssues = mapOf("com.ign1" to 10L, "com.ign2" to 10L),
      )
      createUpdatesManager().also { updatesManager ->
        updatesManager.loadUpdates(
          mapOf(
            "com.ign1" to makePackageInfo("com.ign1"),
            "com.kept" to makePackageInfo("com.kept"),
            "com.ign2" to makePackageInfo("com.ign2"),
          )
        )
        advanceUntilIdle()
        updatesManager.appsWithIssues.test {
          val items = awaitNonNull()
          assertEquals(1, items.size)
          assertEquals("com.kept", items[0].packageName)
        }
      }

      // now we only find an issue that is ignored, so we get an empty issues result
      mockLoadUpdates(
        AppCheckResult(emptyList(), listOf(ign1)),
        ignoredAppIssues = mapOf("com.ign1" to 10L),
      )
      createUpdatesManager().also { updatesManager ->
        updatesManager.loadUpdates(installedApps("com.ign1"))
        advanceUntilIdle()
        updatesManager.appsWithIssues.test { assertTrue(awaitNonNull().isEmpty()) }
      }

      // now isFirstStart=true, so we only get updates, but no issues
      every { repoManager.getRepository(1L) } returns makeRepository()
      mockLoadUpdates(AppCheckResult(listOf(makeUpdatableApp()), listOf(kept)), isFirstStart = true)
      createUpdatesManager().also { updatesManager ->
        updatesManager.loadUpdates(installedApps("com.example.app"))
        advanceUntilIdle()
        assertEquals(1, updatesManager.updates.value?.size)
        assertNull(updatesManager.appsWithIssues.value)
      }
    }

  @Test
  fun `loadUpdates handles exceptions in proxyConfig and dbAppChecker gracefully`() =
    testScope.runTest {
      val updatesManager = createUpdatesManager()

      // exception in proxyConfig → flows unchanged
      every { settingsManager.proxyConfig } throws RuntimeException("Proxy error")
      updatesManager.loadUpdates(installedApps("com.example.app"))
      advanceUntilIdle()
      assertNull(updatesManager.updates.value)
      assertNull(updatesManager.appsWithIssues.value)

      // exception in getApps → flows unchanged
      every { settingsManager.proxyConfig } returns null
      every { dbAppChecker.getApps(any()) } throws RuntimeException("DB error")
      updatesManager.loadUpdates(installedApps("com.example.app"))
      advanceUntilIdle()
      assertNull(updatesManager.updates.value)
      assertEquals(0, updatesManager.numUpdates.value)
      assertNull(updatesManager.appsWithIssues.value)
    }

  @Test
  fun `loadUpdates with both updates and issues also updates both flows`() =
    testScope.runTest {
      mockLoadUpdates(
        AppCheckResult(
          listOf(makeUpdatableApp()),
          listOf(
            AvailableAppWithIssue(
              makeAppOverviewItem(packageName = "com.issue"),
              "2.0",
              20,
              KnownVulnerability(false),
            )
          ),
        )
      )
      every { repoManager.getRepository(1L) } returns makeRepository()
      val updatesManager = createUpdatesManager()
      updatesManager.loadUpdates(
        mapOf(
          "com.example.app" to makePackageInfo("com.example.app"),
          "com.issue" to makePackageInfo("com.issue"),
        )
      )
      advanceUntilIdle()

      updatesManager.updates.test { assertEquals(1, awaitNonNull().size) }
      updatesManager.appsWithIssues.test { assertEquals(1, awaitNonNull().size) }
    }

  @Test
  fun `notificationStates and numUpdates update when app updates change`() =
    testScope.runTest {
      every { repoManager.getRepository(1L) } returns makeRepository()
      val updatesManager = createUpdatesManager()

      // null or empty updates just have empty text
      assertEquals("", updatesManager.notificationStates.getBigText())
      mockLoadUpdates()
      updatesManager.loadUpdates(installedApps("app"))
      advanceUntilIdle()
      assertEquals("", updatesManager.notificationStates.getBigText())

      // now collect changes from 0 to 2 and back to 0 updates
      updatesManager.numUpdates.test {
        assertEquals(0, awaitItem())

        val v1 = makeAppVersion(packageName = "a1", versionName = "2.0")
        val v2 = makeAppVersion(packageName = "a2", versionName = "4.0")
        val u1 =
          makeUpdatableApp(
            packageName = "a1",
            name = "One",
            installedVersionName = "1.0",
            update = v1,
          )
        val u2 =
          makeUpdatableApp(
            packageName = "a2",
            name = "Two",
            installedVersionName = "3.0",
            update = v2,
          )
        mockLoadUpdates(AppCheckResult(listOf(u1, u2), emptyList()))
        updatesManager.loadUpdates(
          mapOf("a1" to makePackageInfo("a1"), "a2" to makePackageInfo("a2"))
        )
        advanceUntilIdle()
        // now we got two updates and notification text was updated
        assertEquals(2, awaitItem())
        updatesManager.notificationStates.getBigText().also { bigText ->
          assertTrue(bigText.contains("One"))
          assertTrue(bigText.contains("Two"))
          assertTrue(bigText.contains("1.0 → 2.0"))
          assertTrue(bigText.contains("3.0 → 4.0"))
        }

        // back to 0 updates
        mockLoadUpdates()
        updatesManager.loadUpdates(installedApps("app"))
        advanceUntilIdle()
        assertEquals(0, awaitItem())
      }
    }

  @Test
  fun `updateAll delegates to UpdateInstaller when updates are loaded`() =
    testScope.runTest {
      val updatesManager = createUpdatesManager()

      // No updates yet -> no delegation
      updatesManager.updateAll(canAskPreApprovalNow = true)
      advanceUntilIdle()
      coVerify(exactly = 0) { updateInstaller.updateAll(any(), any()) }

      val ver = makeAppVersion(versionName = "2.0", versionCode = 20)
      val update = makeUpdatableApp(update = ver)
      mockLoadUpdates(AppCheckResult(listOf(update), emptyList()))
      every { repoManager.getRepository(1L) } returns makeRepository()

      updatesManager.loadUpdates(installedApps("com.example.app"))
      advanceUntilIdle()

      updatesManager.updateAll(canAskPreApprovalNow = true)
      advanceUntilIdle()

      coVerify(exactly = 1) {
        updateInstaller.updateAll(
          match { it.size == 1 && it[0].packageName == "com.example.app" },
          true,
        )
      }
    }

  // Helpers

  private fun makePackageInfo(packageName: String): PackageInfo =
    spyk(PackageInfo()).also {
      it.packageName = packageName
      every { it.toString() } returns "PackageInfo($packageName)"
    }

  private fun makeAppVersion(
    repoId: Long = 1L,
    packageName: String = "com.example.app",
    versionName: String = "2.0",
    versionCode: Long = 20,
    added: Long = 1000L,
  ): AppVersion = mockk {
    every { this@mockk.repoId } returns repoId
    every { this@mockk.packageName } returns packageName
    every { this@mockk.versionName } returns versionName
    every { this@mockk.versionCode } returns versionCode
    every { this@mockk.added } returns added
    every { this@mockk.size } returns 1024L
    every { getWhatsNew(any()) } returns "Bug fixes"
  }

  private fun makeUpdatableApp(
    repoId: Long = 1L,
    packageName: String = "com.example.app",
    name: String? = "Example App",
    installedVersionName: String = "1.0",
    update: AppVersion = makeAppVersion(repoId, packageName),
  ): UpdatableApp = mockk {
    every { this@mockk.repoId } returns repoId
    every { this@mockk.packageName } returns packageName
    every { this@mockk.name } returns name
    every { this@mockk.installedVersionName } returns installedVersionName
    every { this@mockk.update } returns update
    every { getIcon(any()) } returns null
  }

  private fun makeRepository(repoId: Long = 1L): Repository = mockk {
    every { this@mockk.repoId } returns repoId
    every { address } returns "https://f-droid.org/repo"
    every { getMirrors() } returns emptyList()
    every { username } returns null
    every { password } returns null
  }

  private fun makeAppOverviewItem(
    repoId: Long = 1L,
    packageName: String = "com.example.app",
    lastUpdated: Long = 100L,
    name: String = "App",
  ): AppOverviewItem = mockk {
    every { this@mockk.repoId } returns repoId
    every { this@mockk.packageName } returns packageName
    every { this@mockk.lastUpdated } returns lastUpdated
    every { getName(any()) } returns name
    every { getIcon(any()) } returns null
  }

  private fun mockLoadUpdates(
    appCheckResult: AppCheckResult = AppCheckResult(emptyList(), emptyList()),
    isNotificationShowing: Boolean = false,
    isFirstStart: Boolean = false,
    ignoredAppIssues: Map<String, Long> = emptyMap(),
  ) {
    every { settingsManager.proxyConfig } returns null
    every { settingsManager.isFirstStart } returns isFirstStart
    every { settingsManager.ignoredAppIssues } returns ignoredAppIssues
    every { notificationManager.isAppUpdatesAvailableNotificationShowing } returns
      isNotificationShowing
    every { dbAppChecker.getApps(any()) } returns appCheckResult
  }

  private fun installedApps(packageName: String) =
    mapOf(packageName to makePackageInfo(packageName))

  /**
   * Awaits the first non-null item from a nullable StateFlow inside a Turbine [test] block.
   * StateFlows emit their initial null value to new collectors before the real value arrives.
   */
  private suspend fun <T : Any> ReceiveTurbine<T?>.awaitNonNull(): T {
    var item = awaitItem()
    if (item == null) item = awaitItem()
    return checkNotNull(item)
  }
}
