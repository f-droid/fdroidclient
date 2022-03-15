package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.test.TestAppUtils.assertScreenshotsEqual
import org.fdroid.database.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.database.test.TestRepoUtils.getRandomRepo
import org.fdroid.database.test.TestUtils.getRandomString
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AppTest : DbTest() {

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

}
