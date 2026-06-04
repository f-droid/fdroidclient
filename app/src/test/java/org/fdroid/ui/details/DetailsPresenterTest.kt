package org.fdroid.ui.details

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.Signature
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.content.pm.PackageInfoCompat.getLongVersionCode
import androidx.lifecycle.MutableLiveData
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.fdroid.CompatibilityChecker
import org.fdroid.UpdateChecker
import org.fdroid.database.App
import org.fdroid.database.AppDao
import org.fdroid.database.AppIssue
import org.fdroid.database.AppManifest
import org.fdroid.database.AppPrefs
import org.fdroid.database.AppPrefsDao
import org.fdroid.database.AppVersion
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.NotAvailable
import org.fdroid.database.Repository
import org.fdroid.database.VersionDao
import org.fdroid.download.NetworkState
import org.fdroid.index.IndexFormatVersion
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.RepoManager
import org.fdroid.index.v2.FileV1
import org.fdroid.index.v2.SignerV2
import org.fdroid.index.v2.UsesSdkV2
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstallState
import org.fdroid.repo.RepoPreLoader
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.apps.AppWithIssueItem
import org.fdroid.ui.utils.testApp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34]) // needed for oldTargetSdk assertion
@RunWith(RobolectricTestRunner::class)
internal class DetailsPresenterTest {

  private val packageName = testApp.app.packageName
  private val repoId = testApp.app.repoId

  private val db: FDroidDatabase = mockk()
  private val appDao: AppDao = mockk()
  private val versionDao: VersionDao = mockk()
  private val appPrefsDao: AppPrefsDao = mockk()
  private val repoManager: RepoManager = mockk()
  private val repoPreLoader: RepoPreLoader = mockk()
  private val settingsManager: SettingsManager = mockk()
  private val appInstallManager: AppInstallManager = mockk()
  private val viewModel: AppDetailsViewModel = mockk(relaxed = true)
  private val compatibilityChecker: CompatibilityChecker = mockk()
  private val updateChecker = UpdateChecker(compatibilityChecker)

  private val repository =
    Repository(
      repoId = repoId,
      address = "https://example.org/fdroid/repo",
      timestamp = 123L,
      formatVersion = IndexFormatVersion.TWO,
      certificate = "abcd",
      version = 1L,
      weight = 100,
      lastUpdated = 456L,
    )

  private val app: App = mockk()
  private val version: AppVersion = mockk()
  private val versionCode = 42L

  init {
    every { db.getAppDao() } returns appDao
    every { db.getVersionDao() } returns versionDao
    every { db.getAppPrefsDao() } returns appPrefsDao
    every { repoManager.getRepository(repoId) } returns repository
    every { repoPreLoader.defaultRepoAddresses } returns emptySet()
    every { settingsManager.proxyConfig } returns null
    every { appInstallManager.getAppFlow(packageName) } returns flowOf(InstallState.Unknown)

    every { appDao.getApp(repoId, packageName) } returns app
    every { app.metadata } returns testApp.app
    every { app.repoId } returns repoId
    every { app.authorName } returns null
    every { app.packageName } returns packageName
    every { app.getDescription(any()) } returns testApp.description
    every { app.getIcon(any()) } returns null
    every { app.getFeatureGraphic(any()) } returns null
    every { app.getPhoneScreenshots(any()) } returns emptyList()

    every { versionDao.getAppVersions(repoId, packageName) } returns
      MutableLiveData(listOf(version))
    every { version.versionCode } returns versionCode
    every { version.versionName } returns "1.0"
    every { version.file } returns FileV1(name = "test.apk", sha256 = "abcd", size = 123L)
    every { version.added } returns 300L
    every { version.size } returns 123L
    every { version.signer } returns null
    every { version.releaseChannels } returns emptyList()
    every { version.packageManifest } returns AppManifest(versionName = "1.0", versionCode = 42L)
    every { version.hasKnownVulnerability } returns false
    every { version.isCompatible } returns true
    every { version.getWhatsNew(any()) } returns "Bug fixes"
    every { version.antiFeatureKeys } returns emptyList()

    every { appDao.getRepositoryIdsForApp(packageName) } returns listOf(repoId)
    every { appPrefsDao.getAppPrefs(packageName) } returns MutableLiveData(AppPrefs(packageName))
    every { compatibilityChecker.isCompatible(any()) } returns true
  }

  private val appInfoFlow = MutableStateFlow(AppInfo(packageName = packageName))
  private val currentRepoIdFlow = MutableStateFlow<Long?>(repoId)
  private val showAntiFeaturesOnboardingFlow = MutableStateFlow(false)
  private val appsWithIssuesFlow = MutableStateFlow<List<AppWithIssueItem>?>(emptyList())
  private val networkStateFlow = MutableStateFlow(NetworkState(isOnline = true, isMetered = false))

  val presenterFlow =
    moleculeFlow(RecompositionMode.Immediate) {
      DetailsPresenter(
        db = db,
        dispatcher = Dispatchers.Unconfined,
        repoManager = repoManager,
        repoPreLoader = repoPreLoader,
        updateChecker = updateChecker,
        settingsManager = settingsManager,
        appInstallManager = appInstallManager,
        viewModel = viewModel,
        packageInfoFlow = appInfoFlow,
        currentRepoIdFlow = currentRepoIdFlow,
        showAntiFeaturesOnboardingFlow = showAntiFeaturesOnboardingFlow,
        appsWithIssuesFlow = appsWithIssuesFlow,
        networkStateFlow = networkStateFlow,
      )
    }

  // Not found cases

  @Test
  fun emitsNotFoundWhenAppIsNull() = runTest {
    // App does not exist in the database for this repo
    every { appDao.getApp(repoId, packageName) } returns null

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<NotFoundAppDetailsItem>(item)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun emitsNotFoundWhenRepoIsNull() = runTest {
    // Repo has been removed (unlikely here)
    every { repoManager.getRepository(repoId) } returns null

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<NotFoundAppDetailsItem>(item)

      cancelAndPrintRemainingEvents()
    }
  }

  // App that can be installed

  @Test
  fun emitsInstallableLoadedItem() = runTest {
    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertEquals(MainButtonState.INSTALL, item.mainButtonState)
      assertNotNull(item.versions)

      assertEquals(packageName, item.app.packageName)
      assertEquals(testApp.name, item.name)
      assertEquals(testApp.summary, item.summary)
      assertEquals(getHtmlDescription(testApp.description), item.description)
      assertEquals(repoId, item.preferredRepoId)
      assertEquals(InstallState.Unknown, item.installState)
      assertEquals(MainButtonState.INSTALL, item.mainButtonState)
      assertEquals(version, item.suggestedVersion)
      assertFalse(item.showAntiFeaturesOnboarding)

      val versionItem = item.versions.single()
      assertNotNull(versionItem)
      assertEquals(version, versionItem.version)
      assertTrue(versionItem.isSuggested)
      assertTrue(versionItem.isCompatible)
      assertTrue(versionItem.isSignerCompatible)
      assertFalse(versionItem.isInstalled)
      assertTrue(versionItem.showInstallButton)

      assertEquals(NetworkState(isOnline = true, isMetered = false), item.networkState)
      assertNull(item.installedVersionCode)
      assertNull(item.installedVersion)
      assertNull(item.installedSigner)
      assertEquals("Bug fixes", item.whatsNew)
      assertTrue(item.showDonate)
      assertTrue(item.showAuthorContact)
      assertEquals(testApp.liberapayUri, item.liberapayUri)
      assertEquals(testApp.openCollectiveUri, item.openCollectiveUri)
      assertEquals(testApp.bitcoinUri, item.bitcoinUri)
      assertEquals(testApp.litecoinUri, item.litecoinUri)
      assertFalse(item.showWarnings)
      assertFalse(item.ignoresCurrentUpdate)
      assertFalse(item.ignoresAllUpdates)
      assertFalse(item.allowsBetaVersions)
      assertFalse(item.oldTargetSdk)
      assertEquals(emptyList(), item.categories)

      cancelAndPrintRemainingEvents()
    }
  }

  // MainButtonState

  @Test
  fun showsLoadingStateWhenVersionsNotYetAvailable() = runTest {
    setupInstalledApp(versionCode)

    // Return a LiveData with no initial value so versions stay null in the presenter
    every { versionDao.getAppVersions(repoId, packageName) } returns MutableLiveData()

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertEquals(MainButtonState.LOADING, item.mainButtonState)
      assertEquals(null, item.versions)
      assertEquals(versionCode, item.installedVersionCode)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun showsLoadingStateWhenNotInstalledAndVersionsNotYetAvailable() = runTest {
    // similar as above, but with no installed version, so different code path
    every { versionDao.getAppVersions(repoId, packageName) } returns MutableLiveData()

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertEquals(MainButtonState.LOADING, item.mainButtonState)
      assertNull(item.versions)
      assertNull(item.installedVersionCode)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun showsProgressStateWhenInstallationIsInProgress() = runTest {
    val progressState =
      InstallState.Starting(name = testApp.name, versionName = "1.0", lastUpdated = 300L)
    every { appInstallManager.getAppFlow(packageName) } returns flowOf(progressState)

    presenterFlow.test {
      // after first load state is still unknown
      val item1 = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item1)
      assertEquals(InstallState.Unknown, item1.installState)

      // then second emission reflects proper progress state
      val item2 = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item2)
      assertEquals(MainButtonState.PROGRESS, item2.mainButtonState)
      assertEquals(progressState, item2.installState)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun emitsUpdateButtonWhenInstalledAndUpdateAvailable() = runTest {
    setupInstalledApp(versionCode = versionCode - 1)

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertEquals(MainButtonState.UPDATE, item.mainButtonState)
      assertNotNull(item.versions)
      assertEquals(packageName, item.app.packageName)
      assertNotNull(item.installedSigner)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun emitsOpenButtonWhenInstalledWithNoUpdate() = runTest {
    setupInstalledApp(versionCode = versionCode, isInstalled = true)

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertEquals(MainButtonState.NONE, item.mainButtonState)
      assertTrue(item.showOpenButton)
      assertNotNull(item.versions)
      assertEquals(version, item.installedVersion)
      assertEquals(versionCode, item.installedVersionCode)
      assertEquals("0.1", item.installedVersionName)

      val versionItem = item.versions.single()
      assertNotNull(versionItem)
      assertTrue(versionItem.isInstalled)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun suggestedVersionIsStillSetWhenInstalledVersionMatchesSuggestedVersion() = runTest {
    // Even if the installed version code equals the suggested version's code, the presenter
    // must still expose a non-null suggestedVersion
    setupInstalledApp(versionCode = versionCode, isInstalled = true)

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      // The app is up to date, so no update button, but suggestedVersion must still be populated
      assertEquals(MainButtonState.NONE, item.mainButtonState)
      assertNotNull(item.suggestedVersion)
      assertEquals(version, item.suggestedVersion)
      assertEquals(versionCode, item.installedVersionCode)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun emitsNoneButtonWhenNoCompatibleVersions() = runTest {
    // all versions are not compatible with this device
    every { version.isCompatible } returns false
    every { compatibilityChecker.isCompatible(any()) } returns false

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertEquals(MainButtonState.NONE, item.mainButtonState)
      assertTrue(item.isIncompatible)
      assertTrue(item.showWarnings)
      assertTrue(item.versions?.all { !it.isCompatible } ?: false)
      assertFalse(item.showOpenButton)

      cancelAndPrintRemainingEvents()
    }
  }

  // Signer compatibility

  @Test
  fun nullSignerVersionFoundWhenMultipleVersionsShareVersionCode() = runTest {
    // The presenter finds the installed version by signer when multiple versions share the same
    // version code. Versions without a signer are explicitly allowed by F-Droid, so a
    // null signer version must still be recognized as the installed one.
    every { version.signer } returns null

    val version2: AppVersion = mockk()
    every { version2.versionCode } returns versionCode // same code as default version
    every { version2.signer } returns SignerV2(listOf("a_different_signer_hash"))
    every { version2.isCompatible } returns true
    every { version2.antiFeatureKeys } returns emptyList()
    every { versionDao.getAppVersions(repoId, packageName) } returns
      MutableLiveData(listOf(version, version2))

    // Install at the same version code so both versions appear in installedVersions
    setupInstalledApp(versionCode = versionCode, isInstalled = true)

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      // version (null signer, first in list) is matched, version2 (mismatched signer) is skipped
      assertEquals(version, item.installedVersion)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun signerMismatchHidesInstallButtonForVersion() = runTest {
    // When the installed app's signer does not match a version's signer, the version must
    // not show an installation button even if its version code is higher than the installed one.
    every { version.signer } returns SignerV2(listOf("a_different_signer_hash"))

    setupInstalledApp(versionCode = versionCode - 1)

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      val versionItem = item.versions?.single()
      assertNotNull(versionItem)
      assertFalse(versionItem.isSignerCompatible)
      assertFalse(versionItem.showInstallButton) // hidden because of signer mismatch
      // Since the signer doesn't match allowedSigners, no update is suggested either
      assertNull(item.suggestedVersion)
      assertEquals(MainButtonState.NONE, item.mainButtonState)

      cancelAndPrintRemainingEvents()
    }
  }

  // Misc

  @Test
  fun showsAntiFeaturesOnboardingWhenFlagIsSet() = runTest {
    showAntiFeaturesOnboardingFlow.value = true

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertTrue(item.showAntiFeaturesOnboarding)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun showsWarningWhenIssueIsPresent() = runTest {
    val issue: AppIssue = NotAvailable
    appsWithIssuesFlow.value =
      listOf(
        AppWithIssueItem(
          packageName = packageName,
          name = testApp.name,
          installedVersionName = "1.0",
          installedVersionCode = versionCode,
          issue = issue,
          lastUpdated = 200L,
        )
      )

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertEquals(issue, item.issue)
      assertTrue(item.showWarnings)
      assertFalse(item.isIncompatible)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun showsWarningWhenTargetSdkIsTooOld() = runTest {
    // targetSdk 28 on SDK 34 means isAutoUpdateSupported() = false, so oldTargetSdk = true
    every { version.packageManifest } returns
      AppManifest(
        versionName = "1.0",
        versionCode = versionCode,
        usesSdk = UsesSdkV2(minSdkVersion = 21, targetSdkVersion = 28),
      )

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertTrue(item.oldTargetSdk)
      assertTrue(item.showWarnings)
      assertFalse(item.isIncompatible)
      assertNull(item.issue)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun hidesAuthorContactWhenNoEmailNorWebSite() = runTest {
    every { app.metadata } returns testApp.app.copy(authorEmail = null, authorWebSite = null)

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertFalse(item.showAuthorContact)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun showsAuthorHasMoreThanOneApp() = runTest {
    val authorName = "Test Dev"
    every { app.authorName } returns authorName
    every { appDao.hasAuthorMoreThanOneApp(authorName) } returns MutableLiveData(true)

    presenterFlow.test {
      // First non-null item has the initial produceState value of false
      val item1 = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item1)
      assertFalse(item1.authorHasMoreThanOneApp)

      // Second emission reflects the actual DB result
      val item2 = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item2)
      assertTrue(item2.authorHasMoreThanOneApp)

      cancelAndPrintRemainingEvents()
    }
  }

  // Repository visibility

  @Test
  fun hidesRepositoriesWhenAppIsInDefaultRepoOnly() = runTest {
    // The app's repo address is listed as a default address -> repo chooser should stay hidden
    every { repoPreLoader.defaultRepoAddresses } returns setOf(repository.address)

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertTrue(item.repositories.isEmpty())

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun showsRepositoriesWhenAppIsInNonDefaultRepo() = runTest {
    // The repo address is NOT a default address -> repo chooser must be shown
    every { repoPreLoader.defaultRepoAddresses } returns emptySet()

    presenterFlow.test {
      // Initially repositories are empty because of async load
      val item1 = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item1)
      assertEquals(emptyList(), item1.repositories)

      // After the async load the single non-default repo becomes visible
      val item2 = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item2)
      assertEquals(repository, item2.repositories.single())

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun showsMultipleRepositoriesWhenAppInMultipleRepos() = runTest {
    // set up a second repo this app is in
    val repoId2 = 2L
    val repo2 =
      Repository(
        repoId = repoId2,
        address = "https://example.com/second/repo",
        timestamp = 200L,
        formatVersion = IndexFormatVersion.TWO,
        certificate = "abcde",
        version = 2L,
        weight = 50,
        lastUpdated = 500L,
      )
    every { appDao.getRepositoryIdsForApp(packageName) } returns listOf(repoId, repoId2)
    every { repoManager.getRepository(repoId2) } returns repo2

    presenterFlow.test {
      // repos are loaded async, so the list is empty by default (don't show repo chooser)
      val item1 = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item1)
      assertEquals(emptyList(), item1.repositories)

      // now the app is in two repos
      val item2 = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item2)
      assertEquals(2, item2.repositories.size)

      cancelAndPrintRemainingEvents()
    }
  }

  // App preferences

  @Test
  fun usesPreferredRepoIdFromAppPrefs() = runTest {
    // When the user has chosen a preferred repo, preferredRepoId must reflect that choice
    // rather than defaulting to the app's own repoId.
    val customPreferredRepoId = 99L
    every { appPrefsDao.getAppPrefs(packageName) } returns
      MutableLiveData(AppPrefs(packageName, preferredRepoId = customPreferredRepoId))

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertEquals(customPreferredRepoId, item.preferredRepoId)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun ignoresAllUpdatesFromAppPrefs() = runTest {
    // When the user ignores all updates, ignoresAllUpdates must be true and there should be
    // no UPDATE button, but still a suggested version.
    setupInstalledApp(versionCode = versionCode - 1)
    every { appPrefsDao.getAppPrefs(packageName) } returns
      MutableLiveData(AppPrefs(packageName).toggleIgnoreAllUpdates())

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertTrue(item.ignoresAllUpdates)
      // The repo still has a suggested version, it must be shown as suggested
      assertNotNull(item.suggestedVersion)
      assertEquals(version, item.suggestedVersion)
      // No update button because updates are ignored
      assertEquals(MainButtonState.NONE, item.mainButtonState)
      // The user must be able to toggle ignoreAllUpdates back off
      assertNotNull(item.actions.ignoreAllUpdates)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun ignoringAllUpdatesAfterUninstallingApp() = runTest {
    // If the user set ignoreAllUpdates while the app was installed, then uninstalled the app,
    // the AppPrefs entry with ignoreVersionCodeUpdate = Long.MAX_VALUE persists in the DB.
    every { appPrefsDao.getAppPrefs(packageName) } returns
      MutableLiveData(AppPrefs(packageName).toggleIgnoreAllUpdates())

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertTrue(item.ignoresAllUpdates)
      assertNull(item.installedVersionCode) // not installed
      // The repo still has a version, it must be suggested for fresh installation
      assertNotNull(item.suggestedVersion)
      assertEquals(version, item.suggestedVersion)
      // Install button must be shown even though updates were previously ignored
      assertEquals(MainButtonState.INSTALL, item.mainButtonState)
      // The user should see ignoreAllUpdates toggle even without the app installed,
      // so they can undo it
      assertNotNull(item.actions.ignoreAllUpdates)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun ignoresCurrentUpdateFromAppPrefs() = runTest {
    setupInstalledApp(versionCode = versionCode - 1)
    every { appPrefsDao.getAppPrefs(packageName) } returns
      MutableLiveData(AppPrefs(packageName, ignoreVersionCodeUpdate = versionCode))

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertNotNull(item.suggestedVersion)
      // possibleUpdate exists, but ignored, so no update button
      assertEquals(MainButtonState.NONE, item.mainButtonState)
      assertNotNull(item.actions.ignoreThisUpdate) // there is a version to un-ignore
      assertTrue(item.ignoresCurrentUpdate)
      assertFalse(item.ignoresAllUpdates)

      cancelAndPrintRemainingEvents()
    }
  }

  @Test
  fun allowsBetaVersionsFromAppPrefs() = runTest {
    // When the user opts into beta versions, allowsBetaVersions must be reflected in the item.
    every { appPrefsDao.getAppPrefs(packageName) } returns
      MutableLiveData(AppPrefs(packageName).toggleReleaseChannel(RELEASE_CHANNEL_BETA))

    presenterFlow.test {
      val item = awaitNonNullItem()
      assertIs<LoadedAppDetailsItem>(item)
      assertTrue(item.allowsBetaVersions)

      cancelAndPrintRemainingEvents()
    }
  }

  private fun setupInstalledApp(versionCode: Long, isInstalled: Boolean = false) {
    val signature = mockk<Signature>()
    every { signature.toByteArray() } returns byteArrayOf(0xAB.toByte(), 0xCD.toByte())

    val packageInfo =
      spyk(PackageInfo()).also {
        it.packageName = packageName
        it.versionName = "0.1"
        @Suppress("DEPRECATION")
        it.signatures = arrayOf(signature)
      }
    mockkStatic(PackageInfoCompat::getLongVersionCode)
    every { getLongVersionCode(packageInfo) } returns versionCode

    appInfoFlow.value =
      AppInfo(
        packageName = packageName,
        packageInfo = packageInfo,
        launchIntent = if (isInstalled) Intent() else null,
      )
  }

  private suspend fun ReceiveTurbine<AppDetailsItem?>.awaitNonNullItem(): AppDetailsItem {
    var item: AppDetailsItem? = null
    var count = 0
    while (item == null) {
      item = awaitItem()
      count++
    }
    println("Received non-null item after $count emissions")
    return item
  }

  private suspend fun ReceiveTurbine<AppDetailsItem?>.cancelAndPrintRemainingEvents() {
    val lastItems = cancelAndConsumeRemainingEvents()
    if (!lastItems.isEmpty()) println("Received additional items after cancellation")
    lastItems.forEach { item -> println("  $item") }
  }
}
