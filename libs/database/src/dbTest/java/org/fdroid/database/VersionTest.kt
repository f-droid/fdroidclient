package org.fdroid.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.TestUtils.getOrFail
import org.fdroid.index.v2.PackageVersionV2
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

    private val packageName = getRandomString()
    private val packageVersion1 = getRandomPackageVersionV2()
    private val packageVersion2 = getRandomPackageVersionV2()
    private val packageVersion3 = getRandomPackageVersionV2()
    private val versionId1 = packageVersion1.file.sha256
    private val versionId2 = packageVersion2.file.sha256
    private val versionId3 = packageVersion3.file.sha256
    private val isCompatible1 = Random.nextBoolean()
    private val isCompatible2 = Random.nextBoolean()
    private val packageVersions = mapOf(
        versionId1 to packageVersion1,
        versionId2 to packageVersion2,
    )

    private fun getVersion1(repoId: Long) =
        packageVersion1.toVersion(repoId, packageName, versionId1, isCompatible1)

    private fun getVersion2(repoId: Long) =
        packageVersion2.toVersion(repoId, packageName, versionId2, isCompatible2)

    private val compatChecker: (PackageVersionV2) -> Boolean = {
        when (it.file.sha256) {
            versionId1 -> isCompatible1
            versionId2 -> isCompatible2
            else -> fail()
        }
    }

    @Test
    fun insertGetDeleteSingleVersion() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, getRandomMetadataV2())
        versionDao.insert(repoId, packageName, versionId1, packageVersion1, isCompatible1)

        val appVersions = versionDao.getAppVersions(repoId, packageName)
        assertEquals(1, appVersions.size)
        val appVersion = appVersions[0]
        assertEquals(versionId1, appVersion.version.versionId)
        assertEquals(getVersion1(repoId), appVersion.version)
        val manifest = packageVersion1.manifest
        assertEquals(manifest.usesPermission.toSet(), appVersion.usesPermission.toSet())
        assertEquals(manifest.usesPermissionSdk23.toSet(), appVersion.usesPermissionSdk23.toSet())
        assertEquals(
            manifest.features.map { it.name }.toSet(),
            appVersion.version.manifest.features?.toSet()
        )

        val versionedStrings = versionDao.getVersionedStrings(repoId, packageName)
        val expectedSize = manifest.usesPermission.size + manifest.usesPermissionSdk23.size
        assertEquals(expectedSize, versionedStrings.size)

        versionDao.deleteAppVersion(repoId, packageName, versionId1)
        assertEquals(0, versionDao.getAppVersions(repoId, packageName).size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageName).size)
    }

    @Test
    fun insertGetDeleteTwoVersions() {
        // insert two versions along with required objects
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, getRandomMetadataV2())
        versionDao.insert(repoId, packageName, versionId1, packageVersion1, isCompatible1)
        versionDao.insert(repoId, packageName, versionId2, packageVersion2, isCompatible2)

        // get app versions from DB and assign them correctly
        val appVersions = versionDao.getAppVersions(packageName).getOrFail()
        assertEquals(2, appVersions.size)
        val appVersion = if (versionId1 == appVersions[0].version.versionId) {
            appVersions[0]
        } else appVersions[1]
        val appVersion2 = if (versionId2 == appVersions[0].version.versionId) {
            appVersions[0]
        } else appVersions[1]

        // check first version matches
        assertEquals(getVersion1(repoId), appVersion.version)
        val manifest = packageVersion1.manifest
        assertEquals(manifest.usesPermission.toSet(), appVersion.usesPermission.toSet())
        assertEquals(manifest.usesPermissionSdk23.toSet(), appVersion.usesPermissionSdk23.toSet())
        assertEquals(
            manifest.features.map { it.name }.toSet(),
            appVersion.version.manifest.features?.toSet()
        )

        // check second version matches
        assertEquals(getVersion2(repoId), appVersion2.version)
        val manifest2 = packageVersion2.manifest
        assertEquals(manifest2.usesPermission.toSet(), appVersion2.usesPermission.toSet())
        assertEquals(manifest2.usesPermissionSdk23.toSet(),
            appVersion2.usesPermissionSdk23.toSet())
        assertEquals(
            manifest.features.map { it.name }.toSet(),
            appVersion.version.manifest.features?.toSet()
        )

        // delete app and check that all associated data also gets deleted
        appDao.deleteAppMetadata(repoId, packageName)
        assertEquals(0, versionDao.getAppVersions(repoId, packageName).size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageName).size)
    }

    @Test
    fun versionsOnlyFromEnabledRepo() {
        // insert two versions into the same repo
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, getRandomMetadataV2())
        versionDao.insert(repoId, packageName, packageVersions, compatChecker)
        assertEquals(2, versionDao.getAppVersions(packageName).getOrFail().size)
        assertEquals(2, versionDao.getVersions(listOf(packageName)).size)

        // add another version into another repo
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName, getRandomMetadataV2())
        versionDao.insert(repoId2, packageName, versionId3, packageVersion3, true)
        assertEquals(3, versionDao.getAppVersions(packageName).getOrFail().size)
        assertEquals(3, versionDao.getVersions(listOf(packageName)).size)

        // disable second repo
        repoDao.setRepositoryEnabled(repoId2, false)

        // now only two versions get returned
        assertEquals(2, versionDao.getAppVersions(packageName).getOrFail().size)
        assertEquals(2, versionDao.getVersions(listOf(packageName)).size)
    }

    @Test
    fun versionsSortedByVersionCode() {
        // insert three versions into the same repo
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, getRandomMetadataV2())
        versionDao.insert(repoId, packageName, packageVersions, compatChecker)
        versionDao.insert(repoId, packageName, versionId3, packageVersion3, true)
        val versions1 = versionDao.getAppVersions(packageName).getOrFail()
        val versions2 = versionDao.getVersions(listOf(packageName))
        assertEquals(3, versions1.size)
        assertEquals(3, versions2.size)

        // check that they are sorted as expected
        listOf(
            packageVersion1.manifest.versionCode,
            packageVersion2.manifest.versionCode,
            packageVersion3.manifest.versionCode,
        ).sortedDescending().forEachIndexed { i, versionCode ->
            assertEquals(versionCode, versions1[i].version.manifest.versionCode)
            assertEquals(versionCode, versions2[i].versionCode)
        }
    }

    @Test
    fun getVersionsRespectsAppPrefsIgnore() {
        // insert one version into the repo
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        val versionCode = Random.nextLong(1, Long.MAX_VALUE)
        val packageVersion = getRandomPackageVersionV2(versionCode)
        val versionId = packageVersion.file.sha256
        appDao.insert(repoId, packageName, getRandomMetadataV2())
        versionDao.insert(repoId, packageName, versionId, packageVersion, true)
        assertEquals(1, versionDao.getVersions(listOf(packageName)).size)

        // default app prefs don't change result
        var appPrefs = AppPrefs(packageName)
        appPrefsDao.update(appPrefs)
        assertEquals(1, versionDao.getVersions(listOf(packageName)).size)

        // ignore lower version code doesn't change result
        appPrefs = appPrefs.toggleIgnoreVersionCodeUpdate(versionCode - 1)
        appPrefsDao.update(appPrefs)
        assertEquals(1, versionDao.getVersions(listOf(packageName)).size)

        // ignoring exact version code does change result
        appPrefs = appPrefs.toggleIgnoreVersionCodeUpdate(versionCode)
        appPrefsDao.update(appPrefs)
        assertEquals(0, versionDao.getVersions(listOf(packageName)).size)

        // ignoring higher version code does change result
        appPrefs = appPrefs.toggleIgnoreVersionCodeUpdate(versionCode + 1)
        appPrefsDao.update(appPrefs)
        assertEquals(0, versionDao.getVersions(listOf(packageName)).size)

        // ignoring all updates does change result
        appPrefs = appPrefs.toggleIgnoreAllUpdates()
        appPrefsDao.update(appPrefs)
        assertEquals(0, versionDao.getVersions(listOf(packageName)).size)

        // not ignoring all updates brings back version
        appPrefs = appPrefs.toggleIgnoreAllUpdates()
        appPrefsDao.update(appPrefs)
        assertEquals(1, versionDao.getVersions(listOf(packageName)).size)

        // clear all apps and their versions
        appDao.clearAll()
        assertEquals(0, versionDao.countAppVersions())
        assertEquals(0, versionDao.countVersionedStrings())
    }

    @Test
    fun getVersionsConsidersOnlyGivenPackages() {
        // insert two versions
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, getRandomMetadataV2())
        versionDao.insert(repoId, packageName, packageVersions, compatChecker)
        assertEquals(2, versionDao.getVersions(listOf(packageName)).size)

        // insert versions for a different package
        val packageName2 = getRandomString()
        appDao.insert(repoId, packageName2, getRandomMetadataV2())
        versionDao.insert(repoId, packageName2, packageVersions, compatChecker)

        // still only returns above versions
        assertEquals(2, versionDao.getVersions(listOf(packageName)).size)

        // all versions are returned only if all packages are asked for
        assertEquals(4, versionDao.getVersions(listOf(packageName, packageName2)).size)
    }

    @Test
    fun getVersionsHandlesMaxVariableNumber() {
        // sqlite has a maximum number of 999 variables that can be used in a query
        val packagesOk = MutableList(998) { "" } + listOf(packageName)
        val packagesNotOk1 = MutableList(1000) { "" } + listOf(packageName)
        val packagesNotOk2 = MutableList(5000) { "" } + listOf(packageName)

        // insert two versions
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, getRandomMetadataV2())
        versionDao.insert(repoId, packageName, packageVersions, compatChecker)
        assertEquals(2, versionDao.getVersions(listOf(packageName)).size)

        // versions are returned as expected for all lists, no matter their size
        assertEquals(2, versionDao.getVersions(packagesOk).size)
        assertEquals(2, versionDao.getVersions(packagesNotOk1).size)
        assertEquals(2, versionDao.getVersions(packagesNotOk2).size)
    }

}
