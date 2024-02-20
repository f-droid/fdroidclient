package org.fdroid.database

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.SignerV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
internal class DbUpdateCheckerTest : AppTest() {

    private lateinit var updateChecker: DbUpdateChecker
    private val packageManager: PackageManager = mockk()
    private val compatChecker: (PackageVersionV2) -> Boolean = { true }

    private val packageInfo = PackageInfo().apply {
        packageName = TestDataMinV2.packageName
        versionCode = 0
    }

    @Before
    override fun createDb() {
        super.createDb()
        every { packageManager.systemAvailableFeatures } returns emptyArray()
        updateChecker = DbUpdateChecker(db, packageManager) { true }
    }

    @Test
    fun testSuggestedVersion() {
        val repoId = streamIndexV2IntoDb("index-min-v2.json")
        every {
            packageManager.getPackageInfo(packageInfo.packageName, any<Int>())
        } returns packageInfo
        val appVersion = updateChecker.getSuggestedVersion(packageInfo.packageName)
        val expectedVersion = TestDataMinV2.version.toVersion(
            repoId = repoId,
            packageName = packageInfo.packageName,
            versionId = TestDataMinV2.version.file.sha256,
            isCompatible = true,
        )
        assertEquals(appVersion!!.version, expectedVersion)
    }

    @Test
    fun testSuggestedVersionRespectsReleaseChannels() {
        streamIndexV2IntoDb("index-mid-v2.json")
        every { packageManager.getPackageInfo(packageInfo.packageName, any<Int>()) } returns null

        // no suggestion version, because all beta
        val appVersion1 = updateChecker.getSuggestedVersion(packageInfo.packageName)
        assertNull(appVersion1)

        // now suggests only available version
        val appVersion2 = updateChecker.getSuggestedVersion(
            packageName = packageInfo.packageName,
            releaseChannels = listOf(RELEASE_CHANNEL_BETA),
            preferredSigner = TestDataMidV2.version1_2.signer!!.sha256[0],
        )
        assertEquals(TestDataMidV2.version1_2.versionCode, appVersion2!!.version.versionCode)
    }

    @Test
    fun testSuggestedVersionRespectsPreferredSigner() {
        // insert one app into the repo
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1.copy(), locales)

        // two version have two different signers (correct format doesn't matter here)
        val signer1 = SignerV2(listOf("foo", "bar"))
        val signer2 = SignerV2(listOf("justOneSigner"))

        // add two versions with the same version code, but different signers
        val packageVersion1 = getRandomPackageVersionV2(versionCode = 42)
        val packageVersion2 = getRandomPackageVersionV2(versionCode = 42)
        val versionId1 = packageVersion1.file.sha256
        val versionId2 = packageVersion2.file.sha256
        val version1 = packageVersion1.copy(
            manifest = packageVersion1.manifest.copy(signer = signer1),
            releaseChannels = emptyList(),
        ).toVersion(repoId, packageName, versionId1, true)
        val version2 = packageVersion2.copy(
            manifest = packageVersion2.manifest.copy(signer = signer2),
            releaseChannels = emptyList(),
        ).toVersion(repoId, packageName, versionId2, true)
        versionDao.insert(version1)
        versionDao.insert(version2)

        // nothing is currently installed
        every { packageManager.getPackageInfo(packageName, any<Int>()) } returns null

        // if signer of first version is preferred first version is suggested as update
        assertEquals(
            version1,
            updateChecker.getSuggestedVersion(
                packageName = packageName,
                preferredSigner = signer1.sha256[0]
            )?.version,
        )

        // if second signer of first version is preferred first version is suggested as update
        assertEquals(
            version1,
            updateChecker.getSuggestedVersion(
                packageName = packageName,
                preferredSigner = signer1.sha256[1]
            )?.version,
        )

        // if signer of second version is preferred second version is suggested as update
        assertEquals(
            version2,
            updateChecker.getSuggestedVersion(
                packageName = packageName,
                preferredSigner = signer2.sha256[0]
            )?.version,
        )
    }

    @Test
    fun testSuggestedVersionOnlyFromPreferredRepo() {
        // insert the same app into two repos
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageName, app1, locales)
        appDao.insert(repoId2, packageName, app2, locales)

        // every app has a compatible version
        val packageVersion1 = mapOf(
            "1" to getRandomPackageVersionV2(2, null).copy(releaseChannels = emptyList())
        )
        val packageVersion2 = mapOf(
            "2" to getRandomPackageVersionV2(1, null).copy(releaseChannels = emptyList())
        )
        versionDao.insert(repoId1, packageName, packageVersion1, compatChecker)
        versionDao.insert(repoId2, packageName, packageVersion2, compatChecker)

        // nothing is installed
        every {
            packageManager.getPackageInfo(packageName, any<Int>())
        } throws NameNotFoundException()

        // without preferring repos, version with highest version code gets returned
        updateChecker.getSuggestedVersion(packageName).also { appVersion ->
            assertNotNull(appVersion)
            assertEquals(repoId1, appVersion.repoId)
            assertEquals(2, appVersion.manifest.versionCode)
        }

        // now we want versions only from preferred repo and get the one with highest weight
        updateChecker.getSuggestedVersion(packageName, onlyFromPreferredRepo = true)
            .also { appVersion ->
                assertNotNull(appVersion)
                assertEquals(repoId2, appVersion.repoId)
                assertEquals(1, appVersion.manifest.versionCode)
            }

        // now we allow all repos, but explicitly prefer repo 1, getting same result as above
        appPrefsDao.update(AppPrefs(packageInfo.packageName, preferredRepoId = repoId1))
        updateChecker.getSuggestedVersion(packageName).also { appVersion ->
            assertNotNull(appVersion)
            assertEquals(repoId1, appVersion.repoId)
            assertEquals(2, appVersion.manifest.versionCode)
        }

        // now we prefer repo 2 and only want versions from preferred repo
        appPrefsDao.update(AppPrefs(packageInfo.packageName, preferredRepoId = repoId2))
        updateChecker.getSuggestedVersion(packageName, onlyFromPreferredRepo = true)
            .also { appVersion ->
                assertNotNull(appVersion)
                assertEquals(repoId2, appVersion.repoId)
                assertEquals(1, appVersion.manifest.versionCode)
            }

        // now we have version 1 already installed
        every {
            packageManager.getPackageInfo(packageName, any<Int>())
        } returns PackageInfo().apply {
            packageName = this@DbUpdateCheckerTest.packageName
            versionCode = 1
        }

        // preferred repos don't have suggested versions
        updateChecker.getSuggestedVersion(packageName, onlyFromPreferredRepo = true)
            .also { appVersion ->
                assertNull(appVersion)
            }

        // but other repos still have
        updateChecker.getSuggestedVersion(packageName).also { appVersion ->
            assertNotNull(appVersion)
            assertEquals(repoId1, appVersion.repoId)
            assertEquals(2, appVersion.manifest.versionCode)
        }
    }

    @Test
    fun testGetUpdatableApps() {
        streamIndexV2IntoDb("index-min-v2.json")
        every { packageManager.getInstalledPackages(any<Int>()) } returns listOf(packageInfo)

        val appVersions = updateChecker.getUpdatableApps()
        assertEquals(1, appVersions.size)
        assertEquals(0, appVersions[0].installedVersionCode)
        assertEquals(TestDataMinV2.packageName, appVersions[0].packageName)
        assertEquals(TestDataMinV2.version.file.sha256, appVersions[0].update.version.versionId)
    }

    @Test
    fun testGetUpdatableAppsOnlyFromPreferredRepo() {
        // insert the same app into three repos
        val repoId3 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageInfo.packageName, app1, locales)
        appDao.insert(repoId2, packageInfo.packageName, app2, locales)
        appDao.insert(repoId3, packageInfo.packageName, app3, locales)

        // every app has a compatible update (versionCode greater than 0)
        val packageVersion1 = mapOf(
            "1" to getRandomPackageVersionV2(13, null).copy(releaseChannels = emptyList())
        )
        val packageVersion2 = mapOf(
            "2" to getRandomPackageVersionV2(12, null).copy(releaseChannels = emptyList())
        )
        val packageVersion3 = mapOf(
            "3" to getRandomPackageVersionV2(10, null).copy(releaseChannels = emptyList())
        )
        versionDao.insert(repoId1, packageInfo.packageName, packageVersion1, compatChecker)
        versionDao.insert(repoId2, packageInfo.packageName, packageVersion2, compatChecker)
        versionDao.insert(repoId3, packageInfo.packageName, packageVersion3, compatChecker)

        // app is installed with version code 0
        assertEquals(0, packageInfo.versionCode)
        every { packageManager.getInstalledPackages(any<Int>()) } returns listOf(packageInfo)

        // without preferring repos, version with highest version code gets returned
        updateChecker.getUpdatableApps().also { appVersions ->
            assertEquals(1, appVersions.size)
            assertEquals(repoId1, appVersions[0].repoId)
            assertEquals(13, appVersions[0].update.manifest.versionCode)
            assertFalse(appVersions[0].isFromPreferredRepo) // preferred repo is 3 per weight
        }

        // now we want versions only from preferred repo and get the one with highest weight
        updateChecker.getUpdatableApps(onlyFromPreferredRepo = true).also { appVersions ->
            assertEquals(1, appVersions.size)
            assertEquals(repoId3, appVersions[0].repoId)
            assertEquals(10, appVersions[0].update.manifest.versionCode)
            assertTrue(appVersions[0].isFromPreferredRepo) // preferred repo is 3 due to weight
        }

        // now we allow all repos, but explicitly prefer repo 1, isFromPreferredRepo becomes true
        appPrefsDao.update(AppPrefs(packageInfo.packageName, preferredRepoId = repoId1))
        updateChecker.getUpdatableApps().also { appVersions ->
            assertEquals(1, appVersions.size)
            assertEquals(repoId1, appVersions[0].repoId)
            assertEquals(13, appVersions[0].update.manifest.versionCode)
            assertTrue(appVersions[0].isFromPreferredRepo) // preferred repo is 1 now
        }

        // now we prefer repo 2 and only want versions from preferred repo
        appPrefsDao.update(AppPrefs(packageInfo.packageName, preferredRepoId = repoId2))
        updateChecker.getUpdatableApps(onlyFromPreferredRepo = true).also { appVersions ->
            assertEquals(1, appVersions.size)
            assertEquals(repoId2, appVersions[0].repoId)
            assertEquals(12, appVersions[0].update.manifest.versionCode)
            assertTrue(appVersions[0].isFromPreferredRepo) // preferred repo is 2 now
        }
    }

}
