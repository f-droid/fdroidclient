package org.fdroid.database

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
internal class DbUpdateCheckerTest : DbTest() {

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
        updateChecker = DbUpdateChecker(db, packageManager)
    }

    @Test
    fun testSuggestedVersion() {
        val repoId = streamIndexV2IntoDb("index-min-v2.json")
        every {
            packageManager.getPackageInfo(packageInfo.packageName, any())
        } returns packageInfo
        val appVersion = updateChecker.getSuggestedVersion(packageInfo.packageName)
        val expectedVersion = TestDataMinV2.version.toVersion(
            repoId = repoId,
            packageId = packageInfo.packageName,
            versionId = TestDataMinV2.version.file.sha256,
            isCompatible = true,
        )
        assertEquals(appVersion!!.version, expectedVersion)
    }

    @Test
    fun testSuggestedVersionRespectsReleaseChannels() {
        streamIndexV2IntoDb("index-mid-v2.json")
        every { packageManager.getPackageInfo(packageInfo.packageName, any()) } returns null

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
    fun testGetUpdatableApps() {
        streamIndexV2IntoDb("index-min-v2.json")
        every { packageManager.getInstalledPackages(any()) } returns listOf(packageInfo)

        val appVersions = updateChecker.getUpdatableApps()
        assertEquals(1, appVersions.size)
        assertEquals(0, appVersions[0].installedVersionCode)
        assertEquals(TestDataMinV2.packageName, appVersions[0].packageId)
        assertEquals(TestDataMinV2.version.file.sha256, appVersions[0].upgrade.version.versionId)
    }

}
