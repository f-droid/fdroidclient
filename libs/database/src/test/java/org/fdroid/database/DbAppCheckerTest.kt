package org.fdroid.database

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat.getLongVersionCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.fdroid.CompatibilityChecker
import org.fdroid.UpdateChecker
import org.fdroid.index.IndexUtils.getPackageSigner
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val PACKAGE_NAME_OUR = "org.fdroid.fdroid"

@RunWith(RobolectricTestRunner::class)
internal class DbAppCheckerTest {

  val db: FDroidDatabase = mockk()
  val context: Context = mockk()
  val packageManager: PackageManager = mockk()
  val compatibilityChecker: CompatibilityChecker = mockk()
  val updateChecker: UpdateChecker = mockk()
  val appDao: AppDaoInt = mockk()
  val versionDao: VersionDaoInt = mockk()
  val appPrefsDao: AppPrefsDaoInt = mockk()

  private val checker: DbAppChecker

  private val repoId = 1L
  private val packageName = "org.example.app"
  private val appName = "foo bar app"

  init {
    every { context.packageManager } returns packageManager
    every { context.packageName } returns PACKAGE_NAME_OUR
    every { db.getAppDao() } returns appDao
    every { db.getVersionDao() } returns versionDao
    every { db.getAppPrefsDao() } returns appPrefsDao

    checker = DbAppChecker(db, context, compatibilityChecker, updateChecker)
  }

  @Test
  fun `empty packageInfoMap returns empty result`() {
    every { versionDao.getVersions(emptyList()) } returns emptyList()
    every { appPrefsDao.getPreferredRepos(emptyList()) } returns emptyMap()

    val result = checker.getApps(emptyMap())

    assertTrue(result.updates.isEmpty())
    assertTrue(result.issues.isEmpty())
  }

  // unavailable apps

  @Test
  fun `system app no longer in repos is ignored`() {
    val packageInfo = makePackageInfo(isSystemApp = true)
    every { versionDao.getVersions(listOf(packageName)) } returns emptyList()
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertTrue(result.updates.isEmpty())
    assertTrue(result.issues.isEmpty())
  }

  @Test
  fun `non-system app no longer in repos installed by someone else is ignored`() {
    val packageInfo = makePackageInfo()
    every { versionDao.getVersions(listOf(packageName)) } returns emptyList()
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns emptyMap()
    mockInstallSource("org.other.installer")

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertTrue(result.issues.isEmpty())
    assertTrue(result.updates.isEmpty())
  }

  @Test
  fun `non-system app no longer in repos that we installed is flagged as unavailable`() {
    val packageInfo = makePackageInfo()
    every { versionDao.getVersions(listOf(packageName)) } returns emptyList()
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns emptyMap()
    mockInstallSource(PACKAGE_NAME_OUR)
    every { appDao.getAppOverviewItem(any(), packageName) } returns null

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertEquals(1, result.issues.size)
    val issue = result.issues[0] as UnavailableAppWithIssue
    assertEquals(packageName, issue.packageName)
    assertEquals(appName, issue.name)
    assertEquals(packageInfo.versionName, issue.installVersionName)
    assertEquals(packageInfo.longVersionCode, issue.installVersionCode)
    assertTrue(result.updates.isEmpty())
  }

  /**
   * If an app ignores all updates, [VersionDaoInt.getVersions] returns an empty list, so we need to
   * ensure that such an app isn't falsely flagged as unavailable.
   */
  @Test
  fun `non-system app still in repos but with no versions is not flagged as unavailable`() {
    val packageInfo = makePackageInfo()
    every { versionDao.getVersions(listOf(packageName)) } returns emptyList()
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    mockInstallSource(PACKAGE_NAME_OUR)
    every { appDao.getAppOverviewItem(repoId, packageName) } returns makeAppOverviewItem()

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertTrue(result.issues.isEmpty())
  }

  // updatable apps without issues

  @Test
  fun `no updates from checker gets passed on`() {
    val packageInfo = makePackageInfo()
    val version = makeVersion()

    every { versionDao.getVersions(listOf(packageName)) } returns listOf(version)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates<Version>(
        versions = any(),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns emptySequence()

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertTrue(result.updates.isEmpty())
    assertTrue(result.issues.isEmpty())
  }

  @Test
  fun `app with compatible update from preferred repo is considered`() {
    val signerBytes = byteArrayOf(0xAB.toByte())
    val signerHash = getPackageSigner(signerBytes)
    val packageInfo = makePackageInfo(versionCode = 1L, signerBytes = signerBytes)
    val version = makeVersion(versionCode = 2L, signer = signerHash)
    val appOverview = makeAppOverviewItem()
    val appVersion = mockk<AppVersion>()
    val versionedStrings = makeVersionedStrings()

    every { versionDao.getVersions(listOf(packageName)) } returns listOf(version)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates(
        versions = listOf(version),
        allowedSignersGetter = null,
        installedVersionCode = 1L,
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(version)
    every { versionDao.getVersionedStrings(repoId, packageName, any()) } returns versionedStrings
    every { version.toAppVersion(versionedStrings) } returns appVersion
    every { appDao.getAppOverviewItem(repoId, packageName) } returns appOverview

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertEquals(1, result.updates.size)
    assertEquals(packageName, result.updates[0].packageName)
    assertTrue(result.issues.isEmpty())
  }

  // apps that have updates, but there are issues

  @Test
  fun `app with known vulnerability gets flagged`() {
    val signerBytes = byteArrayOf(0xAB.toByte())
    val signerHash = getPackageSigner(signerBytes)
    val packageInfo = makePackageInfo(signerBytes = signerBytes)
    val version = makeVersion(knownVuln = true, signer = signerHash)
    val appOverview = makeAppOverviewItem()

    every { versionDao.getVersions(listOf(packageName)) } returns listOf(version)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates<Version>(
        versions = any(),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(version)
    every { appDao.getAppOverviewItem(repoId, packageName) } returns appOverview

    val result = checker.getApps(mapOf(packageName to packageInfo))

    val issue = result.issues.filterIsInstance<AvailableAppWithIssue>().first()
    assertTrue(issue.issue is KnownVulnerability)
  }

  @Test
  fun `update from non-preferred repo older than 7 days gets flagged`() {
    val otherRepoId = 2L
    val signerBytes = byteArrayOf(0xAB.toByte())
    val signerHash = getPackageSigner(signerBytes)
    val packageInfo = makePackageInfo(signerBytes = signerBytes)
    val oldAdded = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)
    val version = makeVersion(repo = otherRepoId, signer = signerHash, added = oldAdded)
    val appOverview = makeAppOverviewItem()

    every { versionDao.getVersions(listOf(packageName)) } returns listOf(version)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates<Version>(
        versions = any(),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(version)
    every { appDao.getAppOverviewItem(repoId, packageName) } returns appOverview

    val result = checker.getApps(mapOf(packageName to packageInfo))

    val issue = result.issues.filterIsInstance<AvailableAppWithIssue>().first()
    assertTrue(issue.issue is UpdateInOtherRepo)
    assertEquals(otherRepoId, issue.issue.repoIdWithUpdate)
  }

  @Test
  fun `update from non-preferred repo newer than 7 days is not flagged`() {
    val otherRepoId = 2L
    val signerBytes = byteArrayOf(0xAB.toByte())
    val signerHash = getPackageSigner(signerBytes)
    val packageInfo = makePackageInfo(signerBytes = signerBytes)
    val recentAdded = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)
    val version = makeVersion(repo = otherRepoId, signer = signerHash, added = recentAdded)
    val appOverview = makeAppOverviewItem()

    every { versionDao.getVersions(listOf(packageName)) } returns listOf(version)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates<Version>(
        versions = any(),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(version)
    every { appDao.getAppOverviewItem(repoId, packageName) } returns appOverview

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertTrue(result.issues.isEmpty())
  }

  @Test
  fun `an app with all versions having incompatible signer gets flagged`() {
    val packageInfo = makePackageInfo(signerBytes = byteArrayOf(0xAB.toByte()))
    val version = makeVersion(repo = repoId, signer = "incompatible_signer_hash")
    val appOverview = makeAppOverviewItem()

    every { versionDao.getVersions(listOf(packageName)) } returns listOf(version)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates<Version>(
        versions = any(),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(version)
    every { appDao.getAppOverviewItem(repoId, packageName) } returns appOverview
    // we flag incompatible apps, if we installed them or the installer is gone
    mockInstallSource(if (Random.nextBoolean()) PACKAGE_NAME_OUR else null)

    val result = checker.getApps(mapOf(packageName to packageInfo))

    val issue = result.issues.filterIsInstance<AvailableAppWithIssue>().first()
    assertTrue(issue.issue is NoCompatibleSigner)
    assertEquals(null, issue.issue.repoIdWithCompatibleSigner)
  }

  @Test
  fun `app with incompatible signer but installed by other app does not get flagged`() {
    val packageInfo = makePackageInfo(signerBytes = byteArrayOf(0xAB.toByte()))
    val version = makeVersion(repo = repoId, signer = "incompatible_signer_hash")
    val appOverview = makeAppOverviewItem()

    every { versionDao.getVersions(listOf(packageName)) } returns listOf(version)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates<Version>(
        versions = any(),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(version)
    every { appDao.getAppOverviewItem(repoId, packageName) } returns appOverview
    mockInstallSource("com.other.installer")

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertTrue(result.issues.isEmpty())
    assertTrue(result.updates.isEmpty())
  }

  @Test
  fun `app with incompatible update but compatible version in preferred repo doesn't get flagged`() {
    val signerBytes = byteArrayOf(0xAB.toByte())
    val compatibleSigner = getPackageSigner(signerBytes)
    val packageInfo = makePackageInfo(versionCode = 1L, signerBytes = signerBytes)

    // update version has incompatible signer
    val incompatibleVersion =
      makeVersion(repo = repoId, versionCode = 2L, signer = "incompatible_signer_hash")
    // but there's an older compatible version in the preferred repo
    val compatibleVersion = makeVersion(repo = repoId, versionCode = 1L, signer = compatibleSigner)

    every { versionDao.getVersions(listOf(packageName)) } returns
      listOf(incompatibleVersion, compatibleVersion)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates<Version>(
        versions = any(),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(incompatibleVersion)
    every { appDao.getAppOverviewItem(repoId, packageName) } returns makeAppOverviewItem()
    mockInstallSource(PACKAGE_NAME_OUR)

    val result = checker.getApps(mapOf(packageName to packageInfo))

    assertTrue(result.issues.isEmpty())
    assertTrue(result.updates.isEmpty())
  }

  @Test
  fun `incompatible signer in preferred repo but compatible in other repo gets flagged with repoId`() {
    val otherRepoId = 2L
    val signerBytes = byteArrayOf(0xAB.toByte())
    val compatibleSigner = getPackageSigner(signerBytes)
    val packageInfo = makePackageInfo(signerBytes = signerBytes)
    val incompatibleVersion = makeVersion(repo = repoId, signer = "bad_signer")
    val compatibleVersion = makeVersion(repo = otherRepoId, signer = compatibleSigner)
    val appOverview = makeAppOverviewItem()

    every { versionDao.getVersions(listOf(packageName)) } returns
      listOf(incompatibleVersion, compatibleVersion)
    every { appPrefsDao.getPreferredRepos(listOf(packageName)) } returns
      mapOf(packageName to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName) } returns null
    every {
      updateChecker.getUpdates<Version>(
        versions = any(),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(incompatibleVersion, compatibleVersion)
    every { appDao.getAppOverviewItem(repoId, packageName) } returns appOverview

    val result = checker.getApps(mapOf(packageName to packageInfo))

    val issue = result.issues.filterIsInstance<AvailableAppWithIssue>().first()
    val noSigner = issue.issue as NoCompatibleSigner
    assertEquals(otherRepoId, noSigner.repoIdWithCompatibleSigner)
  }

  @Test
  fun `two apps get processed independently, returns one update and one issue`() {
    val packageName1 = "org.example.one"
    val packageName2 = "org.example.two"
    val signerBytes = byteArrayOf(0xAB.toByte())
    val signerHash = getPackageSigner(signerBytes)

    // app1 has a compatible update
    val info1 =
      makePackageInfo(packageName = packageName1, versionCode = 1L, signerBytes = signerBytes)
    val version1 =
      makeVersion(packageName = packageName1, repo = repoId, versionCode = 2L, signer = signerHash)
    val appVersion1 = mockk<AppVersion>()
    val versionedStrings1 = makeVersionedStrings()

    // app2 has a known vulnerability
    val info2 = makePackageInfo(packageName = packageName2, signerBytes = signerBytes)
    val version2 =
      makeVersion(packageName = packageName2, repo = repoId, signer = signerHash, knownVuln = true)

    every { versionDao.getVersions(listOf(packageName1, packageName2)) } returns
      listOf(version1, version2)
    every { appPrefsDao.getPreferredRepos(listOf(packageName1, packageName2)) } returns
      mapOf(packageName1 to repoId, packageName2 to repoId)
    every { appPrefsDao.getAppPrefsOrNull(packageName1) } returns null
    every { appPrefsDao.getAppPrefsOrNull(packageName2) } returns null
    every {
      updateChecker.getUpdates(
        versions = listOf(version1),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(version1)
    every {
      updateChecker.getUpdates(
        versions = listOf(version2),
        allowedSignersGetter = null,
        installedVersionCode = any(),
        allowedReleaseChannels = null,
        includeKnownVulnerabilities = true,
        preferencesGetter = any(),
      )
    } returns sequenceOf(version2)
    every { versionDao.getVersionedStrings(repoId, packageName1, any()) } returns versionedStrings1
    every { version1.toAppVersion(versionedStrings1) } returns appVersion1
    every { appDao.getAppOverviewItem(repoId, packageName1) } returns
      makeAppOverviewItem(packageName1)
    every { appDao.getAppOverviewItem(repoId, packageName2) } returns
      makeAppOverviewItem(packageName2)

    val result = checker.getApps(mapOf(packageName1 to info1, packageName2 to info2))

    assertEquals(1, result.updates.size)
    assertEquals(packageName1, result.updates[0].packageName)
    assertEquals(1, result.issues.size)
    val issue = result.issues.filterIsInstance<AvailableAppWithIssue>().first()
    assertTrue(issue.issue is KnownVulnerability)
  }

  // mock helpers

  private fun makePackageInfo(
    packageName: String = this@DbAppCheckerTest.packageName,
    versionName: String = "1.0",
    versionCode: Long = 1L,
    isSystemApp: Boolean = false,
    signerBytes: ByteArray = byteArrayOf(1, 2, 3),
  ): PackageInfo {
    val appInfo =
      spyk(ApplicationInfo()).also {
        it.flags = if (isSystemApp) ApplicationInfo.FLAG_SYSTEM else 0
      }
    every { appInfo.loadLabel(packageManager) } returns appName

    val sig = mockk<android.content.pm.Signature>()
    every { sig.toByteArray() } returns signerBytes

    val packageInfo =
      spyk(PackageInfo()).also {
        it.packageName = packageName
        it.versionName = versionName
        it.applicationInfo = appInfo
        @Suppress("DEPRECATION")
        it.signatures = arrayOf(sig)
      }
    every { getLongVersionCode(packageInfo) } returns versionCode
    return packageInfo
  }

  private fun makeVersion(
    packageName: String = this@DbAppCheckerTest.packageName,
    repo: Long = repoId,
    versionCode: Long = 2L,
    signer: String = "abc123",
    knownVuln: Boolean = false,
    added: Long = System.currentTimeMillis(),
  ): Version =
    mockk<Version>().also {
      every { it.packageName } returns packageName
      every { it.repoId } returns repo
      every { it.versionCode } returns versionCode
      every { it.versionId } returns "$packageName-$versionCode"
      every { it.hasKnownVulnerability } returns knownVuln
      every { it.added } returns added
      every { it.signer } returns mockk { every { sha256 } returns listOf(signer) }
    }

  @Suppress("DEPRECATION")
  private fun makeAppOverviewItem(packageName: String = this.packageName): AppOverviewItem =
    mockk<AppOverviewItem>().also {
      every { it.packageName } returns packageName
      every { it.name } returns appName
      every { it.summary } returns "summary"
      every { it.localizedIcon } returns null
    }

  private fun mockInstallSource(installerPackageName: String?) {
    every { @Suppress("DEPRECATION") packageManager.getInstallerPackageName(any()) } returns
      installerPackageName
    if (Build.VERSION.SDK_INT >= 30) {
      val installSourceInfo = mockk<InstallSourceInfo>()
      every { installSourceInfo.initiatingPackageName } returns installerPackageName
      every { installSourceInfo.installingPackageName } returns installerPackageName
      if (Build.VERSION.SDK_INT >= 34) {
        every { installSourceInfo.updateOwnerPackageName } returns installerPackageName
      }
      every { packageManager.getInstallSourceInfo(any()) } returns installSourceInfo
    }
  }

  private fun makeVersionedStrings(): List<VersionedString> =
    listOf(
      VersionedString(
        repoId = repoId,
        packageName = packageName,
        versionId = "foo bar versionId",
        type = VersionedStringType.PERMISSION,
        name = "android.permission.INTERNET",
      )
    )
}
