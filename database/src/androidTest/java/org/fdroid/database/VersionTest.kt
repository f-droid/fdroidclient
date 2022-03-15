package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.database.test.TestRepoUtils.getRandomRepo
import org.fdroid.database.test.TestUtils.getRandomString
import org.fdroid.database.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class VersionTest : DbTest() {

    private val packageId = getRandomString()
    private val versionId = getRandomString()

    @Test
    fun insertGetDeleteSingleVersion() {
        val repoId = repoDao.insert(getRandomRepo())
        appDao.insert(repoId, packageId, getRandomMetadataV2())
        val packageVersion = getRandomPackageVersionV2()
        versionDao.insert(repoId, packageId, versionId, packageVersion)

        val appVersions = versionDao.getAppVersions(repoId, packageId)
        assertEquals(1, appVersions.size)
        val appVersion = appVersions[0]
        assertEquals(versionId, appVersion.version.versionId)
        assertEquals(packageVersion.toVersion(repoId, packageId, versionId), appVersion.version)
        val manifest = packageVersion.manifest
        assertEquals(manifest.usesPermission.toSet(), appVersion.usesPermission?.toSet())
        assertEquals(manifest.usesPermissionSdk23.toSet(), appVersion.usesPermissionSdk23?.toSet())
        assertEquals(manifest.features.toSet(), appVersion.features?.toSet())

        val versionedStrings = versionDao.getVersionedStrings(repoId, packageId)
        val expectedSize =
            manifest.usesPermission.size + manifest.usesPermissionSdk23.size + manifest.features.size
        assertEquals(expectedSize, versionedStrings.size)

        versionDao.deleteAppVersion(repoId, packageId, versionId)
        assertEquals(0, versionDao.getAppVersions(repoId, packageId).size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageId).size)
    }

    @Test
    fun insertGetDeleteTwoVersions() {
        // insert two versions along with required objects
        val repoId = repoDao.insert(getRandomRepo())
        appDao.insert(repoId, packageId, getRandomMetadataV2())
        val packageVersion1 = getRandomPackageVersionV2()
        val version1 = getRandomString()
        versionDao.insert(repoId, packageId, version1, packageVersion1)
        val packageVersion2 = getRandomPackageVersionV2()
        val version2 = getRandomString()
        versionDao.insert(repoId, packageId, version2, packageVersion2)

        // get app versions from DB and assign them correctly
        val appVersions = versionDao.getAppVersions(repoId, packageId)
        assertEquals(2, appVersions.size)
        val appVersion = if (version1 == appVersions[0].version.versionId) {
            appVersions[0]
        } else appVersions[1]
        val appVersion2 = if (version2 == appVersions[0].version.versionId) {
            appVersions[0]
        } else appVersions[1]

        // check first version matches
        assertEquals(packageVersion1.toVersion(repoId, packageId, version1), appVersion.version)
        val manifest = packageVersion1.manifest
        assertEquals(manifest.usesPermission.toSet(), appVersion.usesPermission?.toSet())
        assertEquals(manifest.usesPermissionSdk23.toSet(), appVersion.usesPermissionSdk23?.toSet())
        assertEquals(manifest.features.toSet(), appVersion.features?.toSet())

        // check second version matches
        assertEquals(packageVersion2.toVersion(repoId, packageId, version2), appVersion2.version)
        val manifest2 = packageVersion2.manifest
        assertEquals(manifest2.usesPermission.toSet(), appVersion2.usesPermission?.toSet())
        assertEquals(manifest2.usesPermissionSdk23.toSet(),
            appVersion2.usesPermissionSdk23?.toSet())
        assertEquals(manifest2.features.toSet(), appVersion2.features?.toSet())

        // delete app and check that all associated data also gets deleted
        appDao.deleteAppMetadata(repoId, packageId)
        assertEquals(0, versionDao.getAppVersions(repoId, packageId).size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageId).size)
    }

}
