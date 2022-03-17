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

        val app = appDao.getApp(repoId, packageId)
        val metadata = metadataV2.toAppMetadata(repoId, packageId)
        assertEquals(metadata.author, app.metadata.author)
        assertEquals(metadata.donation, app.metadata.donation)
        assertEquals(metadata, app.metadata)
        assertEquals(metadataV2.icon, app.icon)
        assertEquals(metadataV2.featureGraphic, app.featureGraphic)
        assertEquals(metadataV2.promoGraphic, app.promoGraphic)
        assertEquals(metadataV2.tvBanner, app.tvBanner)
        assertScreenshotsEqual(metadataV2.screenshots, app.screenshots)

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

}
