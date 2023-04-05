package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.test.TestRepoUtils
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(AndroidJUnit4::class)
internal class FDroidDatabaseTest : AppTest() {

    @Test
    fun testClearAllAppData() {
        // insert three apps in two repos
        val repoId2 = repoDao.insertOrReplace(TestRepoUtils.getRandomRepo())
        val repoId1 = repoDao.insertOrReplace(TestRepoUtils.getRandomRepo())
        appDao.insert(repoId1, packageName1, app1, locales)
        appDao.insert(repoId2, packageName2, app2, locales)
        appDao.insert(repoId2, packageName3, app3, locales)

        // assert that both repos and all three apps made it into the DB
        assertEquals(2, repoDao.getRepositories().size)
        assertEquals(3, appDao.countApps())

        // assert that repo timestamps are recent, not reset
        repoDao.getRepositories().forEach { repo ->
            assertNotEquals(-1, repo.timestamp)
        }

        // clear all app data
        db.clearAllAppData()

        // assert that both repos survived, but all apps are gone
        assertEquals(2, repoDao.getRepositories().size)
        assertEquals(0, appDao.countApps())

        // assert that repo timestamps got reset
        repoDao.getRepositories().forEach { repo ->
            assertEquals(-1, repo.timestamp)
        }
    }

}
