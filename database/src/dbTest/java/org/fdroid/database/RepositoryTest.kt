package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.TestUtils.assertRepoEquals
import org.fdroid.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class RepositoryTest : DbTest() {

    @Test
    fun insertAndDeleteTwoRepos() {
        // insert first repo
        val repo1 = getRandomRepo()
        val repoId1 = repoDao.insertOrReplace(repo1)

        // check that first repo got added and retrieved as expected
        var repos = repoDao.getRepositories()
        assertEquals(1, repos.size)
        assertRepoEquals(repo1, repos[0])
        val repositoryPreferences1 = repoDao.getRepositoryPreferences(repoId1)
        assertEquals(repoId1, repositoryPreferences1?.repoId)

        // insert second repo
        val repo2 = getRandomRepo()
        val repoId2 = repoDao.insertOrReplace(repo2)

        // check that both repos got added and retrieved as expected
        repos = repoDao.getRepositories().sortedBy { it.repoId }
        assertEquals(2, repos.size)
        assertRepoEquals(repo1, repos[0])
        assertRepoEquals(repo2, repos[1])
        val repositoryPreferences2 = repoDao.getRepositoryPreferences(repoId2)
        assertEquals(repoId2, repositoryPreferences2?.repoId)
        assertEquals(repositoryPreferences1?.weight?.plus(1), repositoryPreferences2?.weight)

        // remove first repo and check that the database only returns one
        repoDao.deleteRepository(repos[0].repository.repoId)
        assertEquals(1, repoDao.getRepositories().size)

        // remove second repo as well and check that all associated data got removed as well
        repoDao.deleteRepository(repos[1].repository.repoId)
        assertEquals(0, repoDao.getRepositories().size)
        assertEquals(0, repoDao.getMirrors().size)
        assertEquals(0, repoDao.getAntiFeatures().size)
        assertEquals(0, repoDao.getCategories().size)
        assertEquals(0, repoDao.getReleaseChannels().size)
        assertNull(repoDao.getRepositoryPreferences(repoId1))
        assertNull(repoDao.getRepositoryPreferences(repoId2))
    }

    @Test
    fun insertTwoReposAndClearAll() {
        val repo1 = getRandomRepo()
        val repo2 = getRandomRepo()
        repoDao.insertOrReplace(repo1)
        repoDao.insertOrReplace(repo2)
        assertEquals(2, repoDao.getRepositories().size)

        repoDao.clearAll()
        assertEquals(0, repoDao.getRepositories().size)
    }

    @Test
    fun clearingRepoRemovesAllAssociatedData() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        val repositoryPreferences = repoDao.getRepositoryPreferences(repoId)
        val packageId = getRandomString()
        val versionId = getRandomString()
        appDao.insert(repoId, packageId, getRandomMetadataV2())
        val packageVersion = getRandomPackageVersionV2()
        versionDao.insert(repoId, packageId, versionId, packageVersion, Random.nextBoolean())

        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(1, appDao.getAppMetadata().size)
        assertEquals(1, versionDao.getAppVersions(repoId, packageId).size)
        assertTrue(versionDao.getVersionedStrings(repoId, packageId).isNotEmpty())

        repoDao.clear(repoId)
        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(0, appDao.getAppMetadata().size)
        assertEquals(0, appDao.getLocalizedFiles().size)
        assertEquals(0, appDao.getLocalizedFileLists().size)
        assertEquals(0, versionDao.getAppVersions(repoId, packageId).size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageId).size)
        // preferences are not touched by clearing
        assertEquals(repositoryPreferences, repoDao.getRepositoryPreferences(repoId))
    }

    @Test
    fun certGetsUpdated() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(null, repoDao.getRepositories()[0].certificate)

        val cert = getRandomString()
        repoDao.updateRepository(repoId, cert)

        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(cert, repoDao.getRepositories()[0].certificate)
    }
}
