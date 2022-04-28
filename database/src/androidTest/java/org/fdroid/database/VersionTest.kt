package org.fdroid.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.test.TestUtils.getOrAwaitValue
import org.fdroid.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
internal class VersionTest : DbTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val packageId = getRandomString()
    private val versionId = getRandomString()

    @Test
    fun insertGetDeleteSingleVersion() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageId, getRandomMetadataV2())
        val packageVersion = getRandomPackageVersionV2()
        val isCompatible = Random.nextBoolean()
        versionDao.insert(repoId, packageId, versionId, packageVersion, isCompatible)

        val appVersions = versionDao.getAppVersions(repoId, packageId)
        assertEquals(1, appVersions.size)
        val appVersion = appVersions[0]
        assertEquals(versionId, appVersion.version.versionId)
        val version = packageVersion.toVersion(repoId, packageId, versionId, isCompatible)
        assertEquals(version, appVersion.version)
        val manifest = packageVersion.manifest
        assertEquals(manifest.usesPermission.toSet(), appVersion.usesPermission?.toSet())
        assertEquals(manifest.usesPermissionSdk23.toSet(), appVersion.usesPermissionSdk23?.toSet())
        assertEquals(
            manifest.features.map { it.name }.toSet(),
            appVersion.version.manifest.features?.toSet()
        )

        val versionedStrings = versionDao.getVersionedStrings(repoId, packageId)
        val expectedSize = manifest.usesPermission.size + manifest.usesPermissionSdk23.size
        assertEquals(expectedSize, versionedStrings.size)

        versionDao.deleteAppVersion(repoId, packageId, versionId)
        assertEquals(0, versionDao.getAppVersions(repoId, packageId).size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageId).size)
    }

    @Test
    fun insertGetDeleteTwoVersions() {
        // insert two versions along with required objects
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageId, getRandomMetadataV2())
        val packageVersion1 = getRandomPackageVersionV2()
        val packageVersion2 = getRandomPackageVersionV2()
        val version1 = getRandomString()
        val version2 = getRandomString()
        val isCompatible1 = Random.nextBoolean()
        val isCompatible2 = Random.nextBoolean()
        versionDao.insert(repoId, packageId, version1, packageVersion1, isCompatible1)
        versionDao.insert(repoId, packageId, version2, packageVersion2, isCompatible2)

        // get app versions from DB and assign them correctly
        val appVersions = versionDao.getAppVersions(packageId).getOrAwaitValue() ?: fail()
        assertEquals(2, appVersions.size)
        val appVersion = if (version1 == appVersions[0].version.versionId) {
            appVersions[0]
        } else appVersions[1]
        val appVersion2 = if (version2 == appVersions[0].version.versionId) {
            appVersions[0]
        } else appVersions[1]

        // check first version matches
        val exVersion1 = packageVersion1.toVersion(repoId, packageId, version1, isCompatible1)
        assertEquals(exVersion1, appVersion.version)
        val manifest = packageVersion1.manifest
        assertEquals(manifest.usesPermission.toSet(), appVersion.usesPermission?.toSet())
        assertEquals(manifest.usesPermissionSdk23.toSet(), appVersion.usesPermissionSdk23?.toSet())
        assertEquals(
            manifest.features.map { it.name }.toSet(),
            appVersion.version.manifest.features?.toSet()
        )

        // check second version matches
        val exVersion2 = packageVersion2.toVersion(repoId, packageId, version2, isCompatible2)
        assertEquals(exVersion2, appVersion2.version)
        val manifest2 = packageVersion2.manifest
        assertEquals(manifest2.usesPermission.toSet(), appVersion2.usesPermission?.toSet())
        assertEquals(manifest2.usesPermissionSdk23.toSet(),
            appVersion2.usesPermissionSdk23?.toSet())
        assertEquals(
            manifest.features.map { it.name }.toSet(),
            appVersion.version.manifest.features?.toSet()
        )

        // delete app and check that all associated data also gets deleted
        appDao.deleteAppMetadata(repoId, packageId)
        assertEquals(0, versionDao.getAppVersions(repoId, packageId).size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageId).size)
    }

}
