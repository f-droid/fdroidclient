package org.fdroid.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.test.TestAppUtils.assertScreenshotsEqual
import org.fdroid.database.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.database.test.TestRepoUtils.getRandomFileV2
import org.fdroid.database.test.TestRepoUtils.getRandomRepo
import org.fdroid.database.test.TestUtils.getOrAwaitValue
import org.fdroid.database.test.TestUtils.getRandomString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class AppTest : DbTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val packageId = getRandomString()

    @Test
    fun insertGetDeleteSingleApp() {
        val repoId = repoDao.insert(getRandomRepo())
        val metadataV2 = getRandomMetadataV2()
        appDao.insert(repoId, packageId, metadataV2)

        val app = appDao.getApp(repoId, packageId) ?: fail()
        val metadata = metadataV2.toAppMetadata(repoId, packageId)
        assertEquals(metadata.author, app.metadata.author)
        assertEquals(metadata.donation, app.metadata.donation)
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
    fun testAppOverViewItem() {
        val repoId = repoDao.insert(getRandomRepo())
        val packageId1 = getRandomString()
        val packageId2 = getRandomString()
        val packageId3 = getRandomString()
        val name1 = mapOf("en-US" to "1")
        val name2 = mapOf("en-US" to "2")
        val name3 = mapOf("en-US" to "3")
        val icons1 = mapOf("foo" to getRandomFileV2(), "bar" to getRandomFileV2())
        val icons2 = mapOf("23" to getRandomFileV2(), "42" to getRandomFileV2())
        val app1 = getRandomMetadataV2().copy(name = name1, icon = icons1)
        val app2 = getRandomMetadataV2().copy(name = name2, icon = icons2)
        val app3 = getRandomMetadataV2().copy(name = name3, icon = null)
        appDao.insert(repoId, packageId1, app1)
        appDao.insert(repoId, packageId2, app2)

        // icons of both apps are returned correctly
        val apps = appDao.getAppOverviewItems().getOrAwaitValue() ?: fail()
        assertEquals(2, apps.size)
        assertEquals(icons1,
            apps.find { it.packageId == packageId1 }?.localizedIcon?.toLocalizedFileV2())
        assertEquals(icons2,
            apps.find { it.packageId == packageId2 }?.localizedIcon?.toLocalizedFileV2())

        // app without icon is returned as well
        appDao.insert(repoId, packageId3, app3)
        val apps3 = appDao.getAppOverviewItems().getOrAwaitValue() ?: fail()
        assertEquals(3, apps3.size)
        assertEquals(icons1,
            apps3.find { it.packageId == packageId1 }?.localizedIcon?.toLocalizedFileV2())
        assertEquals(icons2,
            apps3.find { it.packageId == packageId2 }?.localizedIcon?.toLocalizedFileV2())
        assertEquals(emptyList(), apps3.find { it.packageId == packageId3 }!!.localizedIcon)

        // app4 is the same as app1
        val repoId2 = repoDao.insert(getRandomRepo())
        val app4 = getRandomMetadataV2().copy(name = name2, icon = icons2)
        appDao.insert(repoId2, packageId1, app4)

        val apps4 = appDao.getAppOverviewItems().getOrAwaitValue() ?: fail()
        assertEquals(4, apps4.size)
    }

    @Test
    fun testAppByRepoWeight() {
        val repoId1 = repoDao.insert(getRandomRepo())
        val repoId2 = repoDao.insert(getRandomRepo())
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
