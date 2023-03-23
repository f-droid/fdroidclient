package org.fdroid.database

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.v2.SignerV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
internal class DbUpdateCheckerTest : AppTest() {

    private lateinit var updateChecker: DbUpdateChecker
    private val packageManager: PackageManager = mockk()

    private val packageInfo = PackageInfo().apply {
        packageName = TestDataMinV2.packageName
        @Suppress("DEPRECATION")
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
            updateChecker.getSuggestedVersion(packageName,
                preferredSigner = signer2.sha256[0]
            )?.version,
        )
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

}
