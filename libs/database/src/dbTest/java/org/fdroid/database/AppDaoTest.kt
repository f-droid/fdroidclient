package org.fdroid.database

import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.TestUtils.getOrFail
import org.fdroid.database.TestUtils.toMetadataV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestUtils.sort
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
internal class AppDaoTest : AppTest() {

    @Test
    fun insertGetDeleteSingleApp() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1)

        assertEquals(app1, appDao.getApp(repoId, packageName)?.toMetadataV2()?.sort())
        assertEquals(app1, appDao.getApp(packageName).getOrFail()?.toMetadataV2()?.sort())

        appDao.deleteAppMetadata(repoId, packageName)
        assertEquals(0, appDao.countApps())
        assertEquals(0, appDao.countLocalizedFiles())
        assertEquals(0, appDao.countLocalizedFileLists())
    }

    @Test
    fun testGetSameAppFromTwoRepos() {
        // insert same app into three repos (repoId1 has highest weight)
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val repoId3 = repoDao.insertOrReplace(getRandomRepo())
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageName, app1, locales)
        appDao.insert(repoId2, packageName, app2, locales)
        appDao.insert(repoId3, packageName, app3, locales)

        // ensure expected repo weights
        val repoPrefs1 = repoDao.getRepositoryPreferences(repoId1) ?: fail()
        val repoPrefs2 = repoDao.getRepositoryPreferences(repoId2) ?: fail()
        val repoPrefs3 = repoDao.getRepositoryPreferences(repoId3) ?: fail()
        assertTrue(repoPrefs2.weight < repoPrefs3.weight)
        assertTrue(repoPrefs3.weight < repoPrefs1.weight)

        // each app gets returned as stored from each repo
        assertEquals(app1, appDao.getApp(repoId1, packageName)?.toMetadataV2()?.sort())
        assertEquals(app2, appDao.getApp(repoId2, packageName)?.toMetadataV2()?.sort())
        assertEquals(app3, appDao.getApp(repoId3, packageName)?.toMetadataV2()?.sort())

        // if repo is not given, app from repo with highest weight is returned
        assertEquals(app1, appDao.getApp(packageName).getOrFail()?.toMetadataV2()?.sort())

        // clear all apps
        appDao.clearAll()
        assertEquals(0, appDao.countApps())
        assertEquals(0, appDao.countLocalizedFiles())
        assertEquals(0, appDao.countLocalizedFileLists())
    }

    @Test
    fun testGetSameAppFromTwoReposOneDisabled() {
        // insert same app into two repos (repoId2 has highest weight)
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageName, app1, locales)
        appDao.insert(repoId2, packageName, app2, locales)

        // app from repo with highest weight gets returned
        assertEquals(app2, appDao.getApp(packageName).getOrFail()?.toMetadataV2()?.sort())

        // repo with highest weight gets disabled
        repoDao.setRepositoryEnabled(repoId2, false)

        // now app from repo with lower weight is returned
        assertEquals(app1, appDao.getApp(packageName).getOrFail()?.toMetadataV2()?.sort())
    }

    @Test
    fun testUpdateCompatibility() {
        // insert two apps with one version each
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1, locales)

        // without versions, app isn't compatible
        assertEquals(false, appDao.getApp(repoId, packageName)?.metadata?.isCompatible)
        appDao.updateCompatibility(repoId)
        assertEquals(false, appDao.getApp(repoId, packageName)?.metadata?.isCompatible)

        // still incompatible with incompatible version
        versionDao.insert(repoId, packageName, "1", getRandomPackageVersionV2(), false)
        appDao.updateCompatibility(repoId)
        assertEquals(false, appDao.getApp(repoId, packageName)?.metadata?.isCompatible)

        // only with at least one compatible version, the app becomes compatible
        versionDao.insert(repoId, packageName, "2", getRandomPackageVersionV2(), true)
        appDao.updateCompatibility(repoId)
        assertEquals(true, appDao.getApp(repoId, packageName)?.metadata?.isCompatible)
    }

    @Test
    fun testAfterLocalesChanged() {
        // insert app with German and French locales
        val localesBefore = LocaleListCompat.forLanguageTags("de-DE")
        val app = app1.copy(
            name = mapOf("de-DE" to "de-DE", "fr-FR" to "fr-FR"),
            summary = mapOf("de-DE" to "de-DE", "fr-FR" to "fr-FR"),
        )
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app, localesBefore)

        // device is set to German, so name and summary come out German
        val appBefore = appDao.getApp(repoId, packageName)
        assertEquals("de-DE", appBefore?.name)
        assertEquals("de-DE", appBefore?.summary)

        // device gets switched to French
        val localesAfter = LocaleListCompat.forLanguageTags("fr-FR")
        db.afterLocalesChanged(localesAfter)

        // device is set to French now, so name and summary come out French
        val appAfter = appDao.getApp(repoId, packageName)
        assertEquals("fr-FR", appAfter?.name)
        assertEquals("fr-FR", appAfter?.summary)
    }

    @Test
    fun testGetNumberOfAppsInCategory() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())

        // app1 is in A and B
        appDao.insert(repoId, packageName1, app1, locales)
        assertEquals(1, appDao.getNumberOfAppsInCategory("A"))
        assertEquals(1, appDao.getNumberOfAppsInCategory("B"))
        assertEquals(0, appDao.getNumberOfAppsInCategory("C"))

        // app2 is in A
        appDao.insert(repoId, packageName2, app2, locales)
        assertEquals(2, appDao.getNumberOfAppsInCategory("A"))
        assertEquals(1, appDao.getNumberOfAppsInCategory("B"))
        assertEquals(0, appDao.getNumberOfAppsInCategory("C"))

        // app3 is in A and B
        appDao.insert(repoId, packageName3, app3, locales)
        assertEquals(3, appDao.getNumberOfAppsInCategory("A"))
        assertEquals(2, appDao.getNumberOfAppsInCategory("B"))
        assertEquals(0, appDao.getNumberOfAppsInCategory("C"))
    }

    @Test
    fun testGetNumberOfAppsInRepository() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        assertEquals(0, appDao.getNumberOfAppsInRepository(repoId))

        appDao.insert(repoId, packageName1, app1, locales)
        assertEquals(1, appDao.getNumberOfAppsInRepository(repoId))

        appDao.insert(repoId, packageName2, app2, locales)
        assertEquals(2, appDao.getNumberOfAppsInRepository(repoId))

        appDao.insert(repoId, packageName3, app3, locales)
        assertEquals(3, appDao.getNumberOfAppsInRepository(repoId))
    }

}
