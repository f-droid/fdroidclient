package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.TestUtils.assertRepoEquals
import org.fdroid.database.TestUtils.getOrFail
import org.fdroid.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.TestUtils.orNull
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
internal class RepositoryDaoTest : DbTest() {

    @Test
    fun testInsertInitialRepository() {
        val repo = InitialRepository(
            name = getRandomString(),
            address = getRandomString(),
            mirrors = listOf(getRandomString(), getRandomString(), getRandomString()),
            description = getRandomString(),
            certificate = "abcdef", // not random, because format gets checked
            version = Random.nextLong(),
            enabled = Random.nextBoolean(),
        )
        val repoId = repoDao.insert(repo)

        val actualRepo = repoDao.getRepository(repoId) ?: fail()
        assertEquals(repo.name, actualRepo.getName(locales))
        assertEquals(repo.address, actualRepo.address)
        assertEquals(repo.description, actualRepo.getDescription(locales))
        assertEquals(repo.certificate, actualRepo.certificate)
        assertEquals(repo.version, actualRepo.version)
        assertEquals(repo.enabled, actualRepo.enabled)
        assertEquals(Int.MAX_VALUE - 2, actualRepo.weight) // ignoring provided weight
        assertEquals(-1, actualRepo.timestamp)
        assertEquals(3, actualRepo.mirrors.size)
        assertEquals(emptyList(), actualRepo.userMirrors)
        assertEquals(emptyList(), actualRepo.disabledMirrors)
        assertEquals(repo.mirrors.toSet(), actualRepo.mirrors.map { it.url }.toSet())
        assertEquals(emptyList(), actualRepo.antiFeatures)
        assertEquals(emptyList(), actualRepo.categories)
        assertEquals(emptyList(), actualRepo.releaseChannels)
        assertNull(actualRepo.formatVersion)
        assertNull(actualRepo.repository.icon)
        assertNull(actualRepo.lastUpdated)
        assertNull(actualRepo.webBaseUrl)
    }

    @Test
    fun testInsertEmptyRepo() {
        // insert empty repo
        val address = getRandomString()
        val username = getRandomString().orNull()
        val password = getRandomString().orNull()
        val repoId = repoDao.insertEmptyRepo(address, username, password)

        // check that repo got inserted as expected
        val actualRepo = repoDao.getRepository(repoId) ?: fail()
        assertEquals(address, actualRepo.address)
        assertEquals(username, actualRepo.username)
        assertEquals(password, actualRepo.password)
        assertEquals(-1, actualRepo.timestamp)
        assertEquals(listOf(org.fdroid.download.Mirror(address)), actualRepo.getMirrors())
        assertEquals(emptyList(), actualRepo.antiFeatures)
        assertEquals(emptyList(), actualRepo.categories)
        assertEquals(emptyList(), actualRepo.releaseChannels)
        assertNull(actualRepo.formatVersion)
        assertNull(actualRepo.repository.icon)
        assertNull(actualRepo.lastUpdated)
        assertNull(actualRepo.webBaseUrl)
    }

    @Test
    fun insertAndDeleteTwoRepos() {
        // insert first repo
        val repo1 = getRandomRepo()
        val repoId1 = repoDao.insertOrReplace(repo1)

        // check that first repo got added and retrieved as expected
        repoDao.getRepositories().let { repos ->
            assertEquals(1, repos.size)
            assertRepoEquals(repo1, repos[0])
        }
        val repositoryPreferences1 = repoDao.getRepositoryPreferences(repoId1)
        assertEquals(repoId1, repositoryPreferences1?.repoId)

        // insert second repo
        val repo2 = getRandomRepo()
        val repoId2 = repoDao.insertOrReplace(repo2)

        // check that both repos got added and retrieved as expected
        listOf(
            repoDao.getRepositories().sortedBy { it.repoId },
            repoDao.getLiveRepositories().getOrFail().sortedBy { it.repoId },
        ).forEach { repos ->
            assertEquals(2, repos.size)
            assertRepoEquals(repo1, repos[0])
            assertRepoEquals(repo2, repos[1])
        }
        val repositoryPreferences2 = repoDao.getRepositoryPreferences(repoId2)
        assertEquals(repoId2, repositoryPreferences2?.repoId)
        // second repo has one weight point more than first repo
        assertEquals(repositoryPreferences1?.weight?.minus(2), repositoryPreferences2?.weight)

        // remove first repo and check that the database only returns one
        repoDao.deleteRepository(repoId1)
        listOf(
            repoDao.getRepositories(),
            repoDao.getLiveRepositories().getOrFail(),
        ).forEach { repos ->
            assertEquals(1, repos.size)
            assertRepoEquals(repo2, repos[0])
        }
        assertNull(repoDao.getRepositoryPreferences(repoId1))

        // remove second repo and check that all associated data got removed as well
        repoDao.deleteRepository(repoId2)
        assertEquals(0, repoDao.getRepositories().size)
        assertEquals(0, repoDao.countMirrors())
        assertEquals(0, repoDao.countAntiFeatures())
        assertEquals(0, repoDao.countCategories())
        assertEquals(0, repoDao.countReleaseChannels())
        assertNull(repoDao.getRepositoryPreferences(repoId2))
    }

    @Test
    fun insertTwoReposAndClearAll() {
        val repo1 = getRandomRepo()
        val repo2 = getRandomRepo()
        repoDao.insertOrReplace(repo1)
        repoDao.insertOrReplace(repo2)
        assertEquals(2, repoDao.getRepositories().size)
        assertEquals(2, repoDao.getLiveRepositories().getOrFail().size)

        repoDao.clearAll()
        assertEquals(0, repoDao.getRepositories().size)
        assertEquals(0, repoDao.getLiveRepositories().getOrFail().size)
    }

    @Test
    fun testGetRepositoryByCert() {
        val cert = getRandomString()
        // insert repo and (required) preferences
        val repo1 = getRandomRepo().toCoreRepository(version = 42L, certificate = cert)
        val repoId = repoDao.insertOrReplace(repo1)
        val repositoryPreferences = RepositoryPreferences(repoId, 3)
        repoDao.insert(repositoryPreferences)

        // repo is returned when querying for right cert
        assertEquals(repo1.copy(repoId = repoId), repoDao.getRepository(cert)?.repository)
        // nothing is returned when querying for non-existent cert
        assertNull(repoDao.getRepository("foo bar"))
    }

    @Test
    fun testSetRepositoryEnabled() {
        // repo is enabled by default
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        assertTrue(repoDao.getRepository(repoId)?.enabled ?: fail())

        // disabled repo is disabled
        repoDao.setRepositoryEnabled(repoId, false)
        assertFalse(repoDao.getRepository(repoId)?.enabled ?: fail())

        // enabling again works
        repoDao.setRepositoryEnabled(repoId, true)
        assertTrue(repoDao.getRepository(repoId)?.enabled ?: fail())
    }

    @Test
    fun testUpdateUserMirrors() {
        // repo is enabled by default
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        assertEquals(emptyList(), repoDao.getRepository(repoId)?.userMirrors)

        // add user mirrors
        val userMirrors = listOf(getRandomString(), getRandomString(), getRandomString())
        repoDao.updateUserMirrors(repoId, userMirrors)
        val repo = repoDao.getRepository(repoId) ?: fail()
        assertEquals(userMirrors, repo.userMirrors)

        // user mirrors are part of all mirrors
        val userDownloadMirrors = userMirrors.map { org.fdroid.download.Mirror(it) }
        assertTrue(repo.getMirrors().containsAll(userDownloadMirrors))

        // remove user mirrors
        repoDao.updateUserMirrors(repoId, emptyList())
        assertEquals(emptyList(), repoDao.getRepository(repoId)?.userMirrors)
    }

    @Test
    fun testUpdateUsernameAndPassword() {
        // repo has no username or password initially
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        repoDao.getRepository(repoId)?.let { repo ->
            assertEquals(null, repo.username)
            assertEquals(null, repo.password)
        } ?: fail()

        // add user name and password
        val username = getRandomString().orNull()
        val password = getRandomString().orNull()
        repoDao.updateUsernameAndPassword(repoId, username, password)
        repoDao.getRepository(repoId)?.let { repo ->
            assertEquals(username, repo.username)
            assertEquals(password, repo.password)
        } ?: fail()
    }

    @Test
    fun testUpdateDisabledMirrors() {
        // repo has no username or password initially
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        repoDao.getRepository(repoId)?.let { repo ->
            assertEquals(null, repo.username)
            assertEquals(null, repo.password)
        } ?: fail()

        // add user name and password
        val username = getRandomString().orNull()
        val password = getRandomString().orNull()
        repoDao.updateUsernameAndPassword(repoId, username, password)
        repoDao.getRepository(repoId)?.let { repo ->
            assertEquals(username, repo.username)
            assertEquals(password, repo.password)
        } ?: fail()
    }

    @Test
    fun clearingRepoRemovesAllAssociatedData() {
        // insert one repo with one app with one version
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        val repositoryPreferences = repoDao.getRepositoryPreferences(repoId)
        val packageName = getRandomString()
        val versionId = getRandomString()
        appDao.insert(repoId, packageName, getRandomMetadataV2())
        val packageVersion = getRandomPackageVersionV2()
        versionDao.insert(repoId, packageName, versionId, packageVersion, Random.nextBoolean())

        // data is there as expected
        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(1, appDao.getAppMetadata().size)
        assertEquals(1, versionDao.getAppVersions(repoId, packageName).getOrFail().size)
        assertTrue(versionDao.getVersionedStrings(repoId, packageName).isNotEmpty())

        // clearing the repo removes apps and versions
        repoDao.clear(repoId)
        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(0, appDao.countApps())
        assertEquals(0, appDao.countLocalizedFiles())
        assertEquals(0, appDao.countLocalizedFileLists())
        assertEquals(0, versionDao.getAppVersions(repoId, packageName).getOrFail().size)
        assertEquals(0, versionDao.getVersionedStrings(repoId, packageName).size)
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

    @Test
    fun testGetMinRepositoryWeight() {
        assertEquals(Int.MAX_VALUE, repoDao.getMinRepositoryWeight())

        repoDao.insertOrReplace(getRandomRepo())
        assertEquals(Int.MAX_VALUE - 2, repoDao.getMinRepositoryWeight())

        repoDao.insertOrReplace(getRandomRepo())
        assertEquals(Int.MAX_VALUE - 4, repoDao.getMinRepositoryWeight())
    }

    @Test
    fun testReorderRepositories() {
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val repoId3 = repoDao.insertOrReplace(getRandomRepo())
        val repoId4 = repoDao.insertOrReplace(getRandomRepo())
        val repoId5 = repoDao.insertOrReplace(getRandomRepo())

        // repos are listed in the order they entered the DB [1, 2, 3, 4, 5]
        assertEquals(
            listOf(repoId1, repoId2, repoId3, repoId4, repoId5),
            repoDao.getRepositories().map { it.repoId },
        )

        // 2 gets moved to 5 [1, 3, 4, 5, 2]
        repoDao.reorderRepositories(
            repoToReorder = repoDao.getRepository(repoId2) ?: fail(),
            repoTarget = repoDao.getRepository(repoId5) ?: fail(),
        )
        assertEquals(
            listOf(repoId1, repoId3, repoId4, repoId5, repoId2),
            repoDao.getRepositories().map { it.repoId },
        )

        // 5 gets moved to 1 [5, 1, 3, 4, 2]
        repoDao.reorderRepositories(
            repoToReorder = repoDao.getRepository(repoId5) ?: fail(),
            repoTarget = repoDao.getRepository(repoId1) ?: fail(),
        )
        assertEquals(
            listOf(repoId5, repoId1, repoId3, repoId4, repoId2),
            repoDao.getRepositories().map { it.repoId },
        )

        // 3 gets moved to 5 [3, 5, 1, 4, 2]
        repoDao.reorderRepositories(
            repoToReorder = repoDao.getRepository(repoId3) ?: fail(),
            repoTarget = repoDao.getRepository(repoId5) ?: fail(),
        )
        assertEquals(
            listOf(repoId3, repoId5, repoId1, repoId4, repoId2),
            repoDao.getRepositories().map { it.repoId },
        )

        // 3 gets moved to itself, list shouldn't change [3, 5, 1, 4, 2]
        repoDao.reorderRepositories(
            repoToReorder = repoDao.getRepository(repoId3) ?: fail(),
            repoTarget = repoDao.getRepository(repoId3) ?: fail(),
        )
        assertEquals(
            listOf(repoId3, repoId5, repoId1, repoId4, repoId2),
            repoDao.getRepositories().map { it.repoId },
        )

        // we'll add an archive repo for repo1 to the list [3, 5, (1, 1a), 4, 2]
        repoDao.updateRepository(repoId1, "1234abcd")
        val repo1 = repoDao.getRepository(repoId1) ?: fail()
        val repo1a = InitialRepository(
            name = getRandomString(),
            address = "https://example.org/archive",
            description = getRandomString(),
            certificate = repo1.certificate ?: fail(),
            version = 42L,
            enabled = false,
        )
        val repoId1a = repoDao.insert(repo1a)
        repoDao.setWeight(repoId1a, repo1.weight - 1)

        // now we move repo 1 to position of repo 2 [3, 5, 4, 2, (1, 1a)]
        repoDao.reorderRepositories(
            repoToReorder = repoDao.getRepository(repoId1) ?: fail(),
            repoTarget = repoDao.getRepository(repoId2) ?: fail(),
        )
        assertEquals(
            listOf(repoId3, repoId5, repoId4, repoId2, repoId1, repoId1a),
            repoDao.getRepositories().map { it.repoId },
        )

        // now move repo 1 and its archive to top position [(1, 1a), 3, 5, 4, 2]
        repoDao.reorderRepositories(
            repoToReorder = repoDao.getRepository(repoId1) ?: fail(),
            repoTarget = repoDao.getRepository(repoId3) ?: fail(),
        )
        assertEquals(
            listOf(repoId1, repoId1a, repoId3, repoId5, repoId4, repoId2),
            repoDao.getRepositories().map { it.repoId },
        )

        // archive repos can't be reordered directly
        assertFailsWith<IllegalArgumentException> {
            repoDao.reorderRepositories(
                repoToReorder = repoDao.getRepository(repoId1a) ?: fail(),
                repoTarget = repoDao.getRepository(repoId3) ?: fail(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            repoDao.reorderRepositories(
                repoToReorder = repoDao.getRepository(repoId3) ?: fail(),
                repoTarget = repoDao.getRepository(repoId1a) ?: fail(),
            )
        }
    }
}
