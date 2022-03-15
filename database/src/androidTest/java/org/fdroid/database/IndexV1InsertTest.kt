package org.fdroid.database

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.SerializationException
import org.apache.commons.io.input.CountingInputStream
import org.fdroid.index.v2.IndexStreamProcessor
import org.fdroid.index.IndexV1StreamProcessor
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class IndexV1InsertTest : DbTest() {

    @Test
    fun testStreamIndexV1IntoDb() {
        val c = getApplicationContext<Context>()
        val fileSize = c.resources.assets.openFd("index-v1.json").use { it.length }
        val inputStream = CountingInputStream(c.resources.assets.open("index-v1.json"))
        var currentByteCount: Long = 0
        val indexProcessor = IndexV1StreamProcessor(DbV1StreamReceiver(db)) {
            val bytesRead = inputStream.byteCount
            val bytesSinceLastCall = bytesRead - currentByteCount
            if (bytesSinceLastCall > 0) {
                val percent = ((bytesRead.toDouble() / fileSize) * 100).roundToInt()
                Log.e("IndexV1InsertTest",
                    "Stream bytes read: $bytesRead ($percent%) +$bytesSinceLastCall")
            }
            // the stream gets read in big chunks, but ensure they are not too big, e.g. entire file
            assertTrue(bytesSinceLastCall < 600_000, "$bytesSinceLastCall")
            currentByteCount = bytesRead
            bytesRead
        }

        db.runInTransaction {
            inputStream.use { indexStream ->
                indexProcessor.process(1, indexStream)
            }
        }
        assertTrue(repoDao.getRepositories().size == 1)
        assertTrue(appDao.countApps() > 0)
        assertTrue(appDao.countLocalizedFiles() > 0)
        assertTrue(appDao.countLocalizedFileLists() > 0)
        assertTrue(versionDao.countAppVersions() > 0)
        assertTrue(versionDao.countVersionedStrings() > 0)

        println("Apps: " + appDao.countApps())
        println("LocalizedFiles: " + appDao.countLocalizedFiles())
        println("LocalizedFileLists: " + appDao.countLocalizedFileLists())
        println("Versions: " + versionDao.countAppVersions())
        println("Perms/Features: " + versionDao.countVersionedStrings())

        insertV2ForComparison(2)

        val repo1 = repoDao.getRepository(1)
        val repo2 = repoDao.getRepository(2)
        assertEquals(repo1.repository, repo2.repository.copy(repoId = 1))
        assertEquals(repo1.mirrors, repo2.mirrors.map { it.copy(repoId = 1) })
        assertEquals(repo1.antiFeatures, repo2.antiFeatures)
        assertEquals(repo1.categories, repo2.categories)
        assertEquals(repo1.releaseChannels, repo2.releaseChannels)

        val appMetadata = appDao.getAppMetadata()
        val appMetadata1 = appMetadata.count { it.repoId == 1L }
        val appMetadata2 = appMetadata.count { it.repoId == 2L }
        assertEquals(appMetadata1, appMetadata2)

        val localizedFiles = appDao.getLocalizedFiles()
        val localizedFiles1 = localizedFiles.count { it.repoId == 1L }
        val localizedFiles2 = localizedFiles.count { it.repoId == 2L }
        assertEquals(localizedFiles1, localizedFiles2)

        val localizedFileLists = appDao.getLocalizedFileLists()
        val localizedFileLists1 = localizedFileLists.count { it.repoId == 1L }
        val localizedFileLists2 = localizedFileLists.count { it.repoId == 2L }
        assertEquals(localizedFileLists1, localizedFileLists2)

        appMetadata.filter { it.repoId ==2L }.forEach { m ->
            val metadata1 = appDao.getAppMetadata(1, m.packageId)
            val metadata2 = appDao.getAppMetadata(2, m.packageId)
            assertEquals(metadata1, metadata2.copy(repoId = 1))

            val lFiles1 = appDao.getLocalizedFiles(1, m.packageId).toSet()
            val lFiles2 = appDao.getLocalizedFiles(2, m.packageId)
            assertEquals(lFiles1, lFiles2.map { it.copy(repoId = 1) }.toSet())

            val lFileLists1 = appDao.getLocalizedFileLists(1, m.packageId).toSet()
            val lFileLists2 = appDao.getLocalizedFileLists(2, m.packageId)
            assertEquals(lFileLists1, lFileLists2.map { it.copy(repoId = 1) }.toSet())

            val version1 = versionDao.getVersions(1, m.packageId).toSet()
            val version2 = versionDao.getVersions(2, m.packageId)
            assertEquals(version1, version2.map { it.copy(repoId = 1) }.toSet())

            val vStrings1 = versionDao.getVersionedStrings(1, m.packageId).toSet()
            val vStrings2 = versionDao.getVersionedStrings(2, m.packageId)
            assertEquals(vStrings1, vStrings2.map { it.copy(repoId = 1) }.toSet())
        }
    }

    @Suppress("SameParameterValue")
    private fun insertV2ForComparison(repoId: Long) {
        val c = getApplicationContext<Context>()
        val inputStream = CountingInputStream(c.resources.assets.open("index-v2.json"))
        val indexProcessor = IndexStreamProcessor(DbStreamReceiver(db))
        db.runInTransaction {
            inputStream.use { indexStream ->
                indexProcessor.process(repoId, indexStream)
            }
        }
    }

    @Test
    fun testExceptionWhileStreamingDoesNotSaveIntoDb() {
        val c = getApplicationContext<Context>()
        val cIn = CountingInputStream(c.resources.assets.open("index-v1.json"))
        val indexProcessor = IndexStreamProcessor(DbStreamReceiver(db)) {
            if (cIn.byteCount > 824096) throw SerializationException()
            cIn.byteCount
        }

        assertFailsWith<SerializationException> {
            db.runInTransaction {
                cIn.use { indexStream ->
                    indexProcessor.process(1, indexStream)
                }
            }
        }
        assertTrue(repoDao.getRepositories().isEmpty())
        assertTrue(appDao.countApps() == 0)
        assertTrue(appDao.countLocalizedFiles() == 0)
        assertTrue(appDao.countLocalizedFileLists() == 0)
        assertTrue(versionDao.countAppVersions() == 0)
        assertTrue(versionDao.countVersionedStrings() == 0)
    }

}
