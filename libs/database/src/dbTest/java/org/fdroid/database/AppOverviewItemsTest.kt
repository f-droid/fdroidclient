package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.TestUtils.getOrAwaitValue
import org.fdroid.database.TestUtils.getOrFail
import org.fdroid.index.v2.MetadataV2
import org.fdroid.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class AppOverviewItemsTest : AppTest() {

    @Test
    fun testAntiFeatures() = runBlocking {
        // insert one app with without version
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1, locales)

        // without version, anti-features are empty
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertNull(apps[0].antiFeatures)
        }

        // with one version, the app has those anti-features
        val version = getRandomPackageVersionV2(versionCode = 42)
        versionDao.insert(repoId, packageName, "1", version, true)
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(version.antiFeatures, apps[0].antiFeatures)
        }

        // with two versions, the app has the anti-features of the highest version
        val version2 = getRandomPackageVersionV2(versionCode = 23)
        versionDao.insert(repoId, packageName, "2", version2, true)
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(version.antiFeatures, apps[0].antiFeatures)
        }

        // with three versions, the app has the anti-features of the highest version
        val version3 = getRandomPackageVersionV2(versionCode = 1337)
        versionDao.insert(repoId, packageName, "3", version3, true)
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(version3.antiFeatures, apps[0].antiFeatures)
        }
    }

    @Test
    fun testIcons() = runBlocking {
        // insert one app
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1, locales)

        // icon is returned correctly
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1.icon.getBestLocale(locales), apps[0].getIcon(locales))
        }

        // insert same app into another repo
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName, app2, locales)

        // app is still returned as before
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1.icon.getBestLocale(locales), apps[0].getIcon(locales))
        }

        // after preferring second repo, icon is returned from app in second repo
        appPrefsDao.update(AppPrefs(packageName, preferredRepoId = repoId2))
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app2.icon.getBestLocale(locales), apps[0].getIcon(locales))
        }
    }

    @Test
    fun testLimit() {
        // insert three apps
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName1, app1, locales)
        appDao.insert(repoId, packageName2, app2, locales)
        appDao.insert(repoId, packageName3, app3, locales)

        // limit is respected
        for (i in 0..3) assertEquals(i, appDao.getAppOverviewItems(i).getOrFail().size)
        assertEquals(3, appDao.getAppOverviewItems(42).getOrFail().size)
    }

    @Test
    fun testIncompatibleFlag() = runBlocking {
        // insert two apps
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName1, app1, locales)
        appDao.insert(repoId, packageName2, app2, locales)

        // both apps are not compatible
        getItems().forEach { apps ->
            assertEquals(2, apps.size)
            apps.forEach {
                assertFalse(it.isCompatible)
            }
        }
        // both apps, in the same category, are not compatible
        appDao.getAppOverviewItems("A").getOrFail().also {
            assertEquals(2, it.size)
        }.forEach {
            assertFalse(it.isCompatible)
        }
        assertFalse(appDao.getAppOverviewItem(repoId, packageName1)!!.isCompatible)
        assertFalse(appDao.getAppOverviewItem(repoId, packageName2)!!.isCompatible)

        // each app gets a version
        versionDao.insert(repoId, packageName1, "1", getRandomPackageVersionV2(), true)
        versionDao.insert(repoId, packageName2, "1", getRandomPackageVersionV2(), false)

        // updating compatibility for apps
        appDao.updateCompatibility(repoId)

        // now only one is not compatible
        getItems().forEach { apps ->
            assertEquals(2, apps.size)
            assertFalse(apps[0].isCompatible)
            assertTrue(apps[1].isCompatible)
        }
        appDao.getAppOverviewItems("A").getOrFail().also {
            assertEquals(2, it.size)
            assertFalse(it[0].isCompatible)
            assertTrue(it[1].isCompatible)
        }
        assertTrue(appDao.getAppOverviewItem(repoId, packageName1)!!.isCompatible)
        assertFalse(appDao.getAppOverviewItem(repoId, packageName2)!!.isCompatible)
    }

    @Test
    fun testGetByRepoWeight() = runBlocking {
        // insert one app with one version
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1, locales)
        versionDao.insert(repoId, packageName, "1", getRandomPackageVersionV2(2), true)

        // app is returned correctly
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1, apps[0])
        }

        // add another app without version
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName, app2, locales)

        // app is still returned as before, new repo doesn't override old one
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1, apps[0])
        }

        // now second app from second repo is returned after preferring it explicitly
        appPrefsDao.update(AppPrefs(packageName, preferredRepoId = repoId2))
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app2, apps[0])
        }
    }

    @Test
    fun testGetByRepoPref() = runBlocking {
        // insert same app into three repos (repoId1 has highest weight)
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        val repoId3 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageName, app1, locales)
        appDao.insert(repoId2, packageName, app2, locales)
        appDao.insert(repoId3, packageName, app3, locales)

        // app is returned correctly from repo1
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1, apps[0])
        }
        appDao.getAppOverviewItems("A").getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1, apps[0])
        }

        // prefer repo3 for this app
        appPrefsDao.update(AppPrefs(packageName, preferredRepoId = repoId3))
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app3, apps[0])
        }
        appDao.getAppOverviewItems("B").getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(app3, apps[0])
        }

        // prefer repo2 for this app
        appPrefsDao.update(AppPrefs(packageName, preferredRepoId = repoId2))
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app2, apps[0])
        }
        appDao.getAppOverviewItems("A").getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(app2, apps[0])
        }
        appDao.getAppOverviewItems("B").getOrFail().let { apps ->
            assertEquals(0, apps.size) // app2 is not in category B
        }

        // prefer repo1 for this app
        appPrefsDao.update(AppPrefs(packageName, preferredRepoId = repoId1))
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1, apps[0])
        }
        appDao.getAppOverviewItems("A").getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1, apps[0])
        }
    }

    @Test
    fun testSortOrder() {
        // insert two apps with one version each
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName1, app1, locales)
        appDao.insert(repoId, packageName2, app2, locales)
        versionDao.insert(repoId, packageName1, "1", getRandomPackageVersionV2(), true)
        versionDao.insert(repoId, packageName2, "2", getRandomPackageVersionV2(), true)

        // icons of both apps are returned correctly
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(2, apps.size)
            // app 2 is first, because has icon and summary
            assertEquals(packageName2, apps[0].packageName)
            assertEquals(icons2, apps[0].localizedIcon?.toLocalizedFileV2())
            // app 1 is next, because has icon
            assertEquals(packageName1, apps[1].packageName)
            assertEquals(icons1, apps[1].localizedIcon?.toLocalizedFileV2())
        }

        // app without icon is returned last
        appDao.insert(repoId, packageName3, app3)
        versionDao.insert(repoId, packageName3, "3", getRandomPackageVersionV2(), true)
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(3, apps.size)
            assertEquals(packageName2, apps[0].packageName)
            assertEquals(packageName1, apps[1].packageName)
            assertEquals(packageName3, apps[2].packageName)
            assertEquals(emptyList(), apps[2].localizedIcon)
        }

        // app1b is the same as app1 (but in another repo) and thus will not be shown again
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val app1b = app1.copy(name = name2, icon = icons2, summary = name2)
        appDao.insert(repoId2, packageName1, app1b)
        // note that we don't insert a version here
        assertEquals(3, appDao.getAppOverviewItems().getOrFail().size)

        // app3b is the same as app3, but has an icon, so is not last anymore
        // after we prefer that repo for this app
        val app3b = app3.copy(icon = icons2)
        appDao.insert(repoId2, packageName3, app3b)
        appPrefsDao.update(AppPrefs(packageName3, preferredRepoId = repoId2))
        // note that we don't insert a version here
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(3, apps.size)
            assertEquals(packageName3, apps[0].packageName)
            assertEquals(emptyList(), apps[0].antiFeatureKeys)
            assertEquals(packageName2, apps[1].packageName)
            assertEquals(packageName1, apps[2].packageName)
        }
    }

    @Test
    fun testSortOrderWithCategories() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName1, app1, locales)
        appDao.insert(repoId, packageName2, app2, locales)
        versionDao.insert(repoId, packageName1, "1", getRandomPackageVersionV2(), true)
        versionDao.insert(repoId, packageName2, "2", getRandomPackageVersionV2(), true)

        // icons of both apps are returned correctly
        appDao.getAppOverviewItems("A").getOrFail().let { apps ->
            assertEquals(2, apps.size)
            // app 2 is first, because has icon and summary
            assertEquals(packageName2, apps[0].packageName)
            assertEquals(icons2, apps[0].localizedIcon?.toLocalizedFileV2())
            // app 1 is next, because has icon
            assertEquals(packageName1, apps[1].packageName)
            assertEquals(icons1, apps[1].localizedIcon?.toLocalizedFileV2())
        }

        // only one app is returned for category B
        assertEquals(1, appDao.getAppOverviewItems("B").getOrFail().size)

        // app without icon is returned last
        appDao.insert(repoId, packageName3, app3)
        versionDao.insert(repoId, packageName3, "3", getRandomPackageVersionV2(), true)
        appDao.getAppOverviewItems("A").getOrFail().let { apps ->
            assertEquals(3, apps.size)
            assertEquals(packageName2, apps[0].packageName)
            assertEquals(packageName1, apps[1].packageName)
            assertEquals(packageName3, apps[2].packageName)
            assertEquals(emptyList(), apps[2].localizedIcon)
        }

        // app1b is the same as app1 (but in another repo) and thus will not be shown again
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val app1b = app1.copy(name = name2, icon = icons2, summary = name2)
        appDao.insert(repoId2, packageName1, app1b)
        // note that we don't insert a version here
        assertEquals(3, appDao.getAppOverviewItems("A").getOrFail().size)

        // app3b is the same as app3, but has an icon and is preferred, so is not last anymore
        val app3b = app3.copy(icon = icons2)
        appDao.insert(repoId2, packageName3, app3b)
        appPrefsDao.update(AppPrefs(packageName3, preferredRepoId = repoId2))
        // note that we don't insert a version here
        appDao.getAppOverviewItems("A").getOrFail().let { apps ->
            assertEquals(3, apps.size)
            assertEquals(packageName3, apps[0].packageName)
            assertEquals(emptyList(), apps[0].antiFeatureKeys)
            assertEquals(packageName2, apps[1].packageName)
            assertEquals(packageName1, apps[2].packageName)
        }

        // only two apps are returned for category B
        assertEquals(2, appDao.getAppOverviewItems("B").getOrFail().size)
    }

    @Test
    fun testOnlyFromEnabledRepos() = runBlocking {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName1, app1, locales)
        appDao.insert(repoId, packageName2, app2, locales)
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName3, app3, locales)

        // 3 apps from 2 repos
        getItems().forEach { apps ->
            assertEquals(3, apps.size)
        }
        assertEquals(3, appDao.getAppOverviewItems("A").getOrAwaitValue()?.size)

        // only 1 app after disabling first repo
        repoDao.setRepositoryEnabled(repoId, false)
        getItems().forEach { apps ->
            assertEquals(1, apps.size)
        }
        assertEquals(1, appDao.getAppOverviewItems("A").getOrAwaitValue()?.size)
        assertEquals(1, appDao.getAppOverviewItems("B").getOrAwaitValue()?.size)

        // no more apps after disabling all repos
        repoDao.setRepositoryEnabled(repoId2, false)
        getItems().forEach { apps ->
            assertEquals(0, apps.size)
        }
        assertEquals(0, appDao.getAppOverviewItems("A").getOrAwaitValue()?.size)
        assertEquals(0, appDao.getAppOverviewItems("B").getOrAwaitValue()?.size)
    }

    @Test
    fun testGetAppOverviewItem() {
        // insert three apps into two repos
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName1, app1, locales)
        appDao.insert(repoId, packageName2, app2, locales)
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName3, app3, locales)

        // each app gets returned properly
        assertEquals(app1, appDao.getAppOverviewItem(repoId, packageName1))
        assertEquals(app2, appDao.getAppOverviewItem(repoId, packageName2))
        assertEquals(app3, appDao.getAppOverviewItem(repoId2, packageName3))

        // apps don't get returned from wrong repos
        assertNull(appDao.getAppOverviewItem(repoId2, packageName1))
        assertNull(appDao.getAppOverviewItem(repoId2, packageName2))
        assertNull(appDao.getAppOverviewItem(repoId, packageName3))
    }

    @Test
    fun testGetAppOverviewItemWithIcons() {
        // insert one app (with overlapping icons) into two repos
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId1, packageName, app1, locales)
        appDao.insert(repoId2, packageName, app2, locales)

        // each app gets returned properly
        assertEquals(app1, appDao.getAppOverviewItem(repoId1, packageName))
        assertEquals(app2, appDao.getAppOverviewItem(repoId2, packageName))

        // disable second repo
        repoDao.setRepositoryEnabled(repoId2, false)

        // each app still gets returned properly
        assertEquals(app1, appDao.getAppOverviewItem(repoId1, packageName))
        assertEquals(app2, appDao.getAppOverviewItem(repoId2, packageName))
    }

    @Test
    fun testByAuthor() = runBlocking {
        val author = getRandomString()
        val packageName = getRandomString()
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, getRandomMetadataV2(author), locales)

        // author has only one app
        assertFalse(appDao.hasAuthorMoreThanOneApp(author).getOrFail())
        assertEquals(0, appDao.getAppsByAuthor("foo bar").size)
        appDao.getAppsByAuthor(author).let { apps ->
            assertEquals(1, apps.size)
            assertEquals(packageName, apps[0].packageName)
        }

        // now add 49 more apps
        (1 until 50).forEach { _ ->
            appDao.insert(repoId, getRandomString(), getRandomMetadataV2(author), locales)
        }
        assertTrue(appDao.hasAuthorMoreThanOneApp(author).getOrFail())
        assertEquals(50, appDao.getAppsByAuthor(author).size)
    }

    @Test
    fun testByCategory() = runBlocking {
        // insert three apps
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName1, app1, locales)
        appDao.insert(repoId, packageName2, app2, locales)
        appDao.insert(repoId, packageName3, app3, locales)

        // only two apps are in category B
        appDao.getAppsByCategory("B").let { apps ->
            assertEquals(2, apps.size)
            assertNotEquals(packageName2, apps[0].packageName)
            assertNotEquals(packageName2, apps[1].packageName)
        }

        // no app is in category C
        assertEquals(0, appDao.getAppsByCategory("C").size)

        // we'll add app1 as a variant of app2, but its repo has lower weight, so no effect
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName2, app1, locales)
        appDao.getAppsByCategory("B").let { apps ->
            assertEquals(2, apps.size)
            assertNotEquals(packageName2, apps[0].packageName)
            assertNotEquals(packageName2, apps[1].packageName)
        }
    }

    private suspend fun getItems(): List<List<AppOverviewItem>> {
        return listOf(
            appDao.getAppOverviewItems().getOrFail(),
            // manually sort the second list, so both results are comparable
            appDao.getAllApps().sortedByDescending { it.lastUpdated },
        )
    }

    private fun assertEquals(expected: MetadataV2, actual: AppOverviewItem?) {
        assertNotNull(actual)
        assertEquals(expected.added, actual.added)
        assertEquals(expected.lastUpdated, actual.lastUpdated)
        assertEquals(expected.name.getBestLocale(locales), actual.name)
        assertEquals(expected.summary.getBestLocale(locales), actual.summary)
        assertEquals(expected.summary.getBestLocale(locales), actual.summary)
        assertEquals(expected.icon.getBestLocale(locales), actual.getIcon(locales))
    }

}
