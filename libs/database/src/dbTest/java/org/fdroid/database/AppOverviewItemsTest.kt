package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.TestUtils.getOrAwaitValue
import org.fdroid.database.TestUtils.getOrFail
import org.fdroid.index.v2.MetadataV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
internal class AppOverviewItemsTest : AppTest() {

    @Test
    fun testAntiFeatures() {
        // insert one apps with without version
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1, locales)

        // without version, anti-features are empty
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertNull(apps[0].antiFeatures)
        }

        // with one version, the app has those anti-features
        val version = getRandomPackageVersionV2(versionCode = 42)
        versionDao.insert(repoId, packageName, "1", version, true)
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(version.antiFeatures, apps[0].antiFeatures)
        }

        // with two versions, the app has the anti-features of the highest version
        val version2 = getRandomPackageVersionV2(versionCode = 23)
        versionDao.insert(repoId, packageName, "2", version2, true)
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(version.antiFeatures, apps[0].antiFeatures)
        }

        // with three versions, the app has the anti-features of the highest version
        val version3 = getRandomPackageVersionV2(versionCode = 1337)
        versionDao.insert(repoId, packageName, "3", version3, true)
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(version3.antiFeatures, apps[0].antiFeatures)
        }
    }

    @Test
    fun testIcons() {
        // insert one app
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1, locales)

        // icon is returned correctly
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1.icon.getBestLocale(locales), apps[0].getIcon(locales))
        }

        // insert same app into another repo
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName, app2, locales)

        // now icon is returned from app in second repo
        appDao.getAppOverviewItems().getOrFail().let { apps ->
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
    fun testGetByRepoWeight() {
        // insert one app with one version
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName, app1, locales)
        versionDao.insert(repoId, packageName, "1", getRandomPackageVersionV2(2), true)

        // app is returned correctly
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(app1, apps[0])
        }

        // add another app without version
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName, app2, locales)

        // now second app from second repo is returned
        appDao.getAppOverviewItems().getOrFail().let { apps ->
            assertEquals(1, apps.size)
            assertEquals(app2, apps[0])
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
        val app3b = app3.copy(icon = icons2)
        appDao.insert(repoId2, packageName3, app3b)
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

        // app3b is the same as app3, but has an icon, so is not last anymore
        val app3b = app3.copy(icon = icons2)
        appDao.insert(repoId2, packageName3, app3b)
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
    fun testOnlyFromEnabledRepos() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageName1, app1, locales)
        appDao.insert(repoId, packageName2, app2, locales)
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageName3, app3, locales)

        // 3 apps from 2 repos
        assertEquals(3, appDao.getAppOverviewItems().getOrAwaitValue()?.size)
        assertEquals(3, appDao.getAppOverviewItems("A").getOrAwaitValue()?.size)

        // only 1 app after disabling first repo
        repoDao.setRepositoryEnabled(repoId, false)
        assertEquals(1, appDao.getAppOverviewItems().getOrAwaitValue()?.size)
        assertEquals(1, appDao.getAppOverviewItems("A").getOrAwaitValue()?.size)
        assertEquals(1, appDao.getAppOverviewItems("B").getOrAwaitValue()?.size)

        // no more apps after disabling all repos
        repoDao.setRepositoryEnabled(repoId2, false)
        assertEquals(0, appDao.getAppOverviewItems().getOrAwaitValue()?.size)
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
