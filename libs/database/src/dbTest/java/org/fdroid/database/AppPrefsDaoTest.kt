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
        val repoId3 = repoDao.insertOrReplace(getRandomRepo())
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
        val repoId3 = repoDao.insertOrReplace(getRandomRepo())
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

}
