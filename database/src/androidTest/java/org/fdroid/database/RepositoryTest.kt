package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.TestUtils.getRandomRepo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

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
        repoDao.removeRepository(repos[0].repository)
        assertEquals(1, repoDao.getRepositories().size)

        // remove second repo as well and check that all associated data got removed as well
        repoDao.removeRepository(repos[1].repository)
        assertEquals(0, repoDao.getRepositories().size)
        assertEquals(0, repoDao.getMirrors().size)
        assertEquals(0, repoDao.getAntiFeatures().size)
        assertEquals(0, repoDao.getCategories().size)
        assertEquals(0, repoDao.getReleaseChannels().size)
    }

}
