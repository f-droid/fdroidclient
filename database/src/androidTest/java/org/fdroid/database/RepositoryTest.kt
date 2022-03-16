package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.database.test.TestRepoUtils.assertRepoEquals
import org.fdroid.database.test.TestRepoUtils.getRandomRepo
import org.fdroid.database.test.TestUtils.getRandomString
import org.fdroid.database.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RepositoryTest : DbTest() {

    @Test
    fun insertAndDeleteTwoRepos() {
        // insert first repo
        val repo1 = getRandomRepo()
        repoDao.insert(repo1)

        // check that first repo got added and retrieved as expected
        var repos = repoDao.getRepositories()
        assertEquals(1, repos.size)
        assertRepoEquals(repo1, repos[0])

        // insert second repo
        val repo2 = getRandomRepo()
        repoDao.insert(repo2)

        // check that both repos got added and retrieved as expected
        repos = repoDao.getRepositories().sortedBy { it.repository.repoId }
        assertEquals(2, repos.size)
        assertRepoEquals(repo1, repos[0])
        assertRepoEquals(repo2, repos[1])

        // remove first repo and check that the database only returns one
        repoDao.deleteRepository(repos[0].repository)
        assertEquals(1, repoDao.getRepositories().size)

        // remove second repo as well and check that all associated data got removed as well
        repoDao.deleteRepository(repos[1].repository)
        assertEquals(0, repoDao.getRepositories().size)
        assertEquals(0, repoDao.getMirrors().size)
        assertEquals(0, repoDao.getAntiFeatures().size)
        assertEquals(0, repoDao.getCategories().size)
        assertEquals(0, repoDao.getReleaseChannels().size)
    }

    @Test
    fun replacingRepoRemovesAllAssociatedData() {
        val repoId = repoDao.insert(getRandomRepo())
        val packageId = getRandomString()
        val versionId = getRandomString()
        appDao.insert(repoId, packageId, getRandomMetadataV2())
        val packageVersion = getRandomPackageVersionV2()
        versionDao.insert(repoId, packageId, versionId, packageVersion)

        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(1, appDao.getAppMetadata().size)
        assertEquals(1, versionDao.getAppVersions(repoId, packageId).size)
        assertTrue(versionDao.getVersionedStrings(repoId, packageId).isNotEmpty())

        repoDao.replace(repoId, getRandomRepo())
        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(0, appDao.getAppMetadata().size)
        assertEquals(0, appDao.getLocalizedFiles().size)
        assertEquals(0, appDao.getLocalizedFileLists().size)
        assertEquals(0, versionDao.getAppVersions(repoId, packageId).size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageId).size)
    }
}
