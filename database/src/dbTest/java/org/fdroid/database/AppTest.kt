package org.fdroid.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.TestUtils.getOrAwaitValue
import org.fdroid.test.TestAppUtils.assertScreenshotsEqual
import org.fdroid.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.test.TestRepoUtils.getRandomFileV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.TestVersionUtils.getRandomPackageVersionV2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
internal class AppTest : DbTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val packageId = getRandomString()
    private val packageId1 = getRandomString()
    private val packageId2 = getRandomString()
    private val packageId3 = getRandomString()
    private val name1 = mapOf("en-US" to "1")
    private val name2 = mapOf("en-US" to "2")
    private val name3 = mapOf("en-US" to "3")
    private val icons1 = mapOf("foo" to getRandomFileV2(), "bar" to getRandomFileV2())
    private val icons2 = mapOf("23" to getRandomFileV2(), "42" to getRandomFileV2())
    private val app1 = getRandomMetadataV2().copy(
        name = name1,
        icon = icons1,
        summary = null,
        lastUpdated = 10,
        categories = listOf("A", "B")
    )
    private val app2 = getRandomMetadataV2().copy(
        name = name2,
        icon = icons2,
        summary = name2,
        lastUpdated = 20,
        categories = listOf("A")
    )
    private val app3 = getRandomMetadataV2().copy(
        name = name3,
        icon = null,
        summary = name3,
        lastUpdated = 30,
        categories = listOf("A", "B")
    )

    @Test
    fun insertGetDeleteSingleApp() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        val metadataV2 = getRandomMetadataV2()
        appDao.insert(repoId, packageId, metadataV2)

        val app = appDao.getApp(repoId, packageId) ?: fail()
        val metadata = metadataV2.toAppMetadata(repoId, packageId)
        assertEquals(metadata, app.metadata)
        assertEquals(metadataV2.icon, app.icon)
        assertEquals(metadataV2.featureGraphic, app.featureGraphic)
        assertEquals(metadataV2.promoGraphic, app.promoGraphic)
        assertEquals(metadataV2.tvBanner, app.tvBanner)
        assertScreenshotsEqual(metadataV2.screenshots, app.screenshots)

        assertEquals(metadata, appDao.getApp(packageId).getOrAwaitValue()?.metadata)

        appDao.deleteAppMetadata(repoId, packageId)
        assertEquals(0, appDao.getAppMetadata().size)
        assertEquals(0, appDao.getLocalizedFiles().size)
        assertEquals(0, appDao.getLocalizedFileLists().size)
    }

    @Test
    fun testAppOverViewItemSortOrder() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageId1, app1, locales)
        appDao.insert(repoId, packageId2, app2, locales)
        versionDao.insert(repoId, packageId1, "1", getRandomPackageVersionV2(), true)
        versionDao.insert(repoId, packageId2, "2", getRandomPackageVersionV2(), true)

        // icons of both apps are returned correctly
        val apps = appDao.getAppOverviewItems().getOrAwaitValue() ?: fail()
        assertEquals(2, apps.size)
        // app 2 is first, because has icon and summary
        assertEquals(packageId2, apps[0].packageId)
        assertEquals(icons2, apps[0].localizedIcon?.toLocalizedFileV2())
        // app 1 is next, because has icon
        assertEquals(packageId1, apps[1].packageId)
        assertEquals(icons1, apps[1].localizedIcon?.toLocalizedFileV2())

        // app without icon is returned last
        appDao.insert(repoId, packageId3, app3)
        versionDao.insert(repoId, packageId3, "3", getRandomPackageVersionV2(), true)
        val apps3 = appDao.getAppOverviewItems().getOrAwaitValue() ?: fail()
        assertEquals(3, apps3.size)
        assertEquals(packageId2, apps3[0].packageId)
        assertEquals(packageId1, apps3[1].packageId)
        assertEquals(packageId3, apps3[2].packageId)
        assertEquals(emptyList(), apps3[2].localizedIcon)

        // app1b is the same as app1 (but in another repo) and thus will not be shown again
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val app1b = app1.copy(name = name2, icon = icons2, summary = name2)
        appDao.insert(repoId2, packageId1, app1b)
        // note that we don't insert a version here
        val apps4 = appDao.getAppOverviewItems().getOrAwaitValue() ?: fail()
        assertEquals(3, apps4.size)

        // app3b is the same as app3, but has an icon, so is not last anymore
        val app3b = app3.copy(icon = icons2)
        appDao.insert(repoId2, packageId3, app3b)
        // note that we don't insert a version here
        val apps5 = appDao.getAppOverviewItems().getOrAwaitValue() ?: fail()
        assertEquals(3, apps5.size)
        assertEquals(packageId3, apps5[0].packageId)
        assertEquals(emptyList(), apps5[0].antiFeatureNames)
        assertEquals(packageId2, apps5[1].packageId)
        assertEquals(packageId1, apps5[2].packageId)
    }

    @Test
    fun testAppOverViewItemSortOrderWithCategories() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageId1, app1, locales)
        appDao.insert(repoId, packageId2, app2, locales)
        versionDao.insert(repoId, packageId1, "1", getRandomPackageVersionV2(), true)
        versionDao.insert(repoId, packageId2, "2", getRandomPackageVersionV2(), true)

        // icons of both apps are returned correctly
        val apps = appDao.getAppOverviewItems("A").getOrAwaitValue() ?: fail()
        assertEquals(2, apps.size)
        // app 2 is first, because has icon and summary
        assertEquals(packageId2, apps[0].packageId)
        assertEquals(icons2, apps[0].localizedIcon?.toLocalizedFileV2())
        // app 1 is next, because has icon
        assertEquals(packageId1, apps[1].packageId)
        assertEquals(icons1, apps[1].localizedIcon?.toLocalizedFileV2())

        // only one app is returned for category B
        assertEquals(1, appDao.getAppOverviewItems("B").getOrAwaitValue()?.size ?: fail())

        // app without icon is returned last
        appDao.insert(repoId, packageId3, app3)
        versionDao.insert(repoId, packageId3, "3", getRandomPackageVersionV2(), true)
        val apps3 = appDao.getAppOverviewItems("A").getOrAwaitValue() ?: fail()
        assertEquals(3, apps3.size)
        assertEquals(packageId2, apps3[0].packageId)
        assertEquals(packageId1, apps3[1].packageId)
        assertEquals(packageId3, apps3[2].packageId)
        assertEquals(emptyList(), apps3[2].localizedIcon)

        // app1b is the same as app1 (but in another repo) and thus will not be shown again
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val app1b = app1.copy(name = name2, icon = icons2, summary = name2)
        appDao.insert(repoId2, packageId1, app1b)
        // note that we don't insert a version here
        val apps4 = appDao.getAppOverviewItems("A").getOrAwaitValue() ?: fail()
        assertEquals(3, apps4.size)

        // app3b is the same as app3, but has an icon, so is not last anymore
        val app3b = app3.copy(icon = icons2)
        appDao.insert(repoId2, packageId3, app3b)
        // note that we don't insert a version here
        val apps5 = appDao.getAppOverviewItems("A").getOrAwaitValue() ?: fail()
        assertEquals(3, apps5.size)
        assertEquals(packageId3, apps5[0].packageId)
        assertEquals(emptyList(), apps5[0].antiFeatureNames)
        assertEquals(packageId2, apps5[1].packageId)
        assertEquals(packageId1, apps5[2].packageId)

        // only two apps are returned for category B
        assertEquals(2, appDao.getAppOverviewItems("B").getOrAwaitValue()?.size)
    }

    @Test
    fun testAppOverViewItemOnlyFromEnabledRepos() {
        val repoId = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId, packageId1, app1, locales)
        appDao.insert(repoId, packageId2, app2, locales)
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        appDao.insert(repoId2, packageId3, app3, locales)

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
    fun testAppByRepoWeight() {
        val repoId1 = repoDao.insertOrReplace(getRandomRepo())
        val repoId2 = repoDao.insertOrReplace(getRandomRepo())
        val metadata1 = getRandomMetadataV2()
        val metadata2 = metadata1.copy(lastUpdated = metadata1.lastUpdated + 1)

        // app is only in one repo, so returns it's repoId
        appDao.insert(repoId1, packageId, metadata1)
        assertEquals(repoId1, appDao.getRepoIdForPackage(packageId).getOrAwaitValue())

        // ensure second repo has a higher weight
        val repoPrefs1 = repoDao.getRepositoryPreferences(repoId1) ?: fail()
        val repoPrefs2 = repoDao.getRepositoryPreferences(repoId2) ?: fail()
        assertTrue(repoPrefs1.weight < repoPrefs2.weight)

        // app is now in repo with higher weight, so it's repoId gets returned
        appDao.insert(repoId2, packageId, metadata2)
        assertEquals(repoId2, appDao.getRepoIdForPackage(packageId).getOrAwaitValue())
        assertEquals(appDao.getApp(repoId2, packageId)?.metadata,
            appDao.getApp(packageId).getOrAwaitValue()?.metadata)
        assertScreenshotsEqual(appDao.getApp(repoId2, packageId)?.screenshots,
            appDao.getApp(packageId).getOrAwaitValue()?.screenshots)
        assertEquals(appDao.getApp(repoId2, packageId)?.icon,
            appDao.getApp(packageId).getOrAwaitValue()?.icon)
        assertEquals(appDao.getApp(repoId2, packageId)?.featureGraphic,
            appDao.getApp(packageId).getOrAwaitValue()?.featureGraphic)
        assertNotEquals(appDao.getApp(repoId1, packageId),
            appDao.getApp(packageId).getOrAwaitValue())
    }

}
