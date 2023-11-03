package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.TestUtils.getOrFail
import org.fdroid.database.TestUtils.toMetadataV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestUtils.sort
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class AppPrefsDaoTest : AppTest() {

    @Test
    fun testDisablingPreferredRepo() {
        // insert same app into three repos (repoId3 has highest weight)
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageName, app1, locales)
        appDao.insert(repoId2, packageName, app2, locales)
        appDao.insert(repoId2, packageName, app3, locales)

        // app from preferred repo gets returned
        appPrefsDao.update(AppPrefs(packageName, preferredRepoId = repoId1))
        assertEquals(app1, appDao.getApp(packageName).getOrFail()?.toMetadataV2()?.sort())

        // preferred repo gets disabled
        repoDao.setRepositoryEnabled(repoId1, false)

        // now app from repo with highest weight is returned
        assertEquals(app3, appDao.getApp(packageName).getOrFail()?.toMetadataV2()?.sort())
    }

    @Test
    fun testRemovingPreferredRepo() {
        // insert same app into three repos (repoId3 has highest weight)
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageName, app1, locales)
        appDao.insert(repoId2, packageName, app2, locales)
        appDao.insert(repoId2, packageName, app3, locales)

        // app from preferred repo gets returned
        appPrefsDao.update(AppPrefs(packageName, preferredRepoId = repoId1))
        assertEquals(app1, appDao.getApp(packageName).getOrFail()?.toMetadataV2()?.sort())

        // preferred repo gets removed
        repoDao.deleteRepository(repoId1)

        // now app from repo with highest weight is returned
        assertEquals(app3, appDao.getApp(packageName).getOrFail()?.toMetadataV2()?.sort())
    }

    @Test
    fun testGetPreferredRepos() {
        // insert three apps, the third is in repo2 and repo3
        val repoId3 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageName1, app1, locales)
        appDao.insert(repoId2, packageName2, app2, locales)
        appDao.insert(repoId2, packageName3, app3, locales)
        appDao.insert(repoId3, packageName3, app3, locales)

        // app1 and app2 are only in one repo, so that one is preferred
        appPrefsDao.getPreferredRepos(listOf(packageName1, packageName2)).also { preferredRepos ->
            assertEquals(2, preferredRepos.size)
            assertEquals(repoId1, preferredRepos[packageName1])
            assertEquals(repoId2, preferredRepos[packageName2])
        }

        // preference only based on global repo priority/weight (3>2>1)
        appPrefsDao.getPreferredRepos(listOf(packageName3, packageName2)).also { preferredRepos ->
            assertEquals(2, preferredRepos.size)
            assertEquals(repoId2, preferredRepos[packageName2])
            assertEquals(repoId3, preferredRepos[packageName3])
        }

        // now app3 prefers repo2 explicitly
        appPrefsDao.update(AppPrefs(packageName3, preferredRepoId = repoId2))
        appPrefsDao.getPreferredRepos(listOf(packageName3)).also { preferredRepos ->
            assertEquals(1, preferredRepos.size)
            assertEquals(repoId2, preferredRepos[packageName3])
        }

        // app3 moves back to preferring repo3 and query for non-existent package name as well
        appPrefsDao.update(AppPrefs(packageName3, preferredRepoId = repoId3))
        appPrefsDao.getPreferredRepos(listOf(packageName, packageName3)).also { preferredRepos ->
            assertEquals(1, preferredRepos.size)
            assertEquals(repoId3, preferredRepos[packageName3])
        }
    }

    @Test
    fun getGetPreferredReposHandlesMaxVariableNumber() {
        // sqlite has a maximum number of 999 variables that can be used in a query
        val packagesOk = MutableList(998) { "" } + listOf(packageName)
        val packagesNotOk1 = MutableList(1000) { "" } + listOf(packageName)
        val packagesNotOk2 = MutableList(5000) { "" } + listOf(packageName)

        // insert same app in three repos
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1, locales)
        appDao.insert(repoId, packageName, app2, locales)
        appDao.insert(repoId, packageName, app3, locales)

        // preferred repos are returned as expected for all lists, no matter their size
        assertEquals(1, appPrefsDao.getPreferredRepos(packagesOk).size)
        assertEquals(1, appPrefsDao.getPreferredRepos(packagesNotOk1).size)
        assertEquals(1, appPrefsDao.getPreferredRepos(packagesNotOk2).size)
    }

}
