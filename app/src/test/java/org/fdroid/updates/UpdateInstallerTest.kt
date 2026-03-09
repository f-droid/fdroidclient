package org.fdroid.updates

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.fdroid.database.App
import org.fdroid.database.AppDao
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.download.PackageName
import org.fdroid.index.RepoManager
import org.fdroid.install.AppInstallManager
import org.fdroid.ui.apps.AppUpdateItem
import org.junit.Before
import org.junit.Test

private const val OWN_PACKAGE_NAME = "our.package.name"

@OptIn(ExperimentalCoroutinesApi::class)
internal class UpdateInstallerTest {

  private val context: Context = mockk(relaxed = true)
  private val db: FDroidDatabase = mockk()
  private val appDao: AppDao = mockk()
  private val repoManager: RepoManager = mockk()
  private val appInstallManager: AppInstallManager = mockk()

  private val testScope = TestScope()
  private val scope =
    CoroutineScope(
      testScope.backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScope.testScheduler)
    )

  @Before
  fun setUp() {
    every { context.packageName } returns OWN_PACKAGE_NAME
    every { db.getAppDao() } returns appDao
  }

  private fun createUpdateInstaller(): UpdateInstaller {
    return UpdateInstaller(
      context = context,
      db = db,
      repoManager = repoManager,
      appInstallManager = appInstallManager,
      coroutineScope = scope,
    )
  }

  @Test
  fun `updateAll returns early when updates list is empty`() =
    testScope.runTest {
      val installer = createUpdateInstaller()

      installer.updateAll(emptyList(), canAskPreApprovalNow = false)
      advanceUntilIdle()

      coVerify(exactly = 0) { appInstallManager.install(any(), any(), any(), any(), any(), any()) }
      verify(exactly = 0) { appInstallManager.setWaitingState(any(), any(), any(), any(), any()) }
    }

  @Test
  fun `updateAll preApproval is true for single app and forced false for multiple`() =
    testScope.runTest {
      every { repoManager.getRepository(1L) } returns makeRepository()
      coEvery { appInstallManager.install(any(), any(), any(), any(), any(), any()) } returns
        mockk()

      // single app + canAsk=true -> canAskPreApprovalNow=true
      val ver = makeAppVersion(versionName = "5.0", versionCode = 50)
      val app = makeApp()
      every { appDao.getApp(1L, "com.example.app") } returns app
      val oneUpdate =
        listOf(
          makeAppUpdateItem(
            packageName = "com.example.app",
            installedVersionName = "4.0",
            update = ver,
          )
        )

      createUpdateInstaller().updateAll(oneUpdate, canAskPreApprovalNow = true)
      advanceUntilIdle()

      coVerify(exactly = 1) {
        appInstallManager.install(
          appMetadata =
            match { metadata ->
              metadata.packageName == "com.example.app" && metadata.repoId == 1L
            },
          version = ver,
          currentVersionName = "4.0",
          repo = any(),
          iconModel = any(),
          canAskPreApprovalNow = true,
        )
      }

      // multiple apps + canAsk=true -> forced to false
      every { appDao.getApp(1L, "a1") } returns makeApp(packageName = "a1")
      every { appDao.getApp(1L, "a2") } returns makeApp(packageName = "a2")
      val twoUpdates =
        listOf(makeAppUpdateItem(packageName = "a1"), makeAppUpdateItem(packageName = "a2"))

      createUpdateInstaller().updateAll(twoUpdates, canAskPreApprovalNow = true)
      advanceUntilIdle()

      coVerify(exactly = 2) {
        appInstallManager.install(
          appMetadata = any(),
          version = any(),
          currentVersionName = any(),
          repo = any(),
          iconModel = any(),
          canAskPreApprovalNow = false,
        )
      }
    }

  @Test
  fun `updateAll updates own app last and sets waiting state`() =
    testScope.runTest {
      val otherPkg = "com.example.other"
      every { repoManager.getRepository(1L) } returns makeRepository()
      coEvery { appInstallManager.install(any(), any(), any(), any(), any(), any()) } returns
        mockk()

      val ownVersion =
        makeAppVersion(packageName = OWN_PACKAGE_NAME, versionName = "3.0", added = 9999L)
      every { appDao.getApp(1L, OWN_PACKAGE_NAME) } returns makeApp(packageName = OWN_PACKAGE_NAME)
      every { appDao.getApp(1L, otherPkg) } returns makeApp(packageName = otherPkg)
      every {
        appInstallManager.setWaitingState(
          packageName = OWN_PACKAGE_NAME,
          name = any(),
          versionName = any(),
          currentVersionName = any(),
          lastUpdated = any(),
        )
      } just runs

      val updates =
        listOf(
          makeAppUpdateItem(
            packageName = OWN_PACKAGE_NAME,
            installedVersionName = "2.0",
            update = ownVersion,
          ),
          makeAppUpdateItem(packageName = otherPkg),
        )

      createUpdateInstaller().updateAll(updates, canAskPreApprovalNow = false)
      advanceUntilIdle()

      verify(exactly = 1) {
        appInstallManager.setWaitingState(
          packageName = OWN_PACKAGE_NAME,
          name = any(),
          versionName = "3.0",
          currentVersionName = "2.0",
          lastUpdated = 9999L,
        )
      }

      coVerifyOrder {
        appInstallManager.install(
          appMetadata = match { metadata -> metadata.packageName == otherPkg },
          version = any(),
          currentVersionName = any(),
          repo = any(),
          iconModel = any(),
          canAskPreApprovalNow = any(),
        )
        appInstallManager.install(
          appMetadata = match { metadata -> metadata.packageName == OWN_PACKAGE_NAME },
          version = any(),
          currentVersionName = any(),
          repo = any(),
          iconModel = any(),
          canAskPreApprovalNow = any(),
        )
      }
    }

  @Test
  fun `updateApp continues with null values if app missing in DB or repo missing`() =
    testScope.runTest {
      val ver = makeAppVersion(versionName = "5.0", versionCode = 50)
      val updates =
        listOf(
          makeAppUpdateItem(
            packageName = "com.example.app",
            installedVersionName = "4.0",
            update = ver,
          )
        )
      coEvery { appInstallManager.install(any(), any(), any(), any(), any(), any()) } returns
        mockk()

      // repo is null
      every { repoManager.getRepository(1L) } returns null
      every { appDao.getApp(1L, "com.example.app") } returns makeApp()
      createUpdateInstaller().updateAll(updates, canAskPreApprovalNow = false)
      advanceUntilIdle()
      coVerify(exactly = 1) { appInstallManager.install(any(), any(), any(), null, any(), any()) }

      // app is null
      every { repoManager.getRepository(1L) } returns makeRepository()
      every { appDao.getApp(1L, "com.example.app") } returns null
      createUpdateInstaller().updateAll(updates, canAskPreApprovalNow = false)
      advanceUntilIdle()
      coVerify(exactly = 1) { appInstallManager.install(null, any(), any(), any(), any(), any()) }
    }

  private fun makeAppUpdateItem(
    repoId: Long = 1L,
    packageName: String = "com.example.app",
    installedVersionName: String = "1.0",
    update: AppVersion = makeAppVersion(repoId = repoId, packageName = packageName),
  ): AppUpdateItem {
    return AppUpdateItem(
      repoId = repoId,
      packageName = packageName,
      name = "Example App",
      installedVersionName = installedVersionName,
      update = update,
      whatsNew = null,
      iconModel = PackageName(packageName, null),
    )
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
  }

  private fun makeRepository(repoId: Long = 1L): Repository = mockk {
    every { this@mockk.repoId } returns repoId
    every { address } returns "https://f-droid.org/repo"
    every { getMirrors() } returns emptyList()
    every { username } returns null
    every { password } returns null
  }

  private fun makeApp(repoId: Long = 1L, packageName: String = "com.example.app"): App = mockk {
    every { metadata } returns
      AppMetadata(
        repoId = repoId,
        packageName = packageName,
        added = 0L,
        lastUpdated = 0L,
        isCompatible = true,
        name = mapOf("en-US" to "Example App"),
      )
  }
}
