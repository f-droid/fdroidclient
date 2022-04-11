package org.fdroid.database

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.SerializationException
import org.apache.commons.io.input.CountingInputStream
import org.fdroid.index.v2.IndexStreamProcessor
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class IndexV2InsertTest : DbTest() {

    @Test
    fun testStreamIndexV2IntoDb() {
        val c = getApplicationContext<Context>()
        val fileSize = c.resources.assets.openFd("index-v2.json").use { it.length }
        val inputStream = CountingInputStream(c.resources.assets.open("index-v2.json"))
        var currentByteCount: Long = 0
        val indexProcessor = IndexStreamProcessor(DbStreamReceiver(db) { true }, null) {
            val bytesRead = inputStream.byteCount
            val bytesSinceLastCall = bytesRead - currentByteCount
            if (bytesSinceLastCall > 0) {
                val percent = ((bytesRead.toDouble() / fileSize) * 100).roundToInt()
                Log.e("IndexV2InsertTest",
                    "Stream bytes read: $bytesRead ($percent%) +$bytesSinceLastCall")
            }
            // the stream gets read in big chunks, but ensure they are not too big, e.g. entire file
            assertTrue(bytesSinceLastCall < 400_000, "$bytesSinceLastCall")
            currentByteCount = bytesRead
            bytesRead
        }

        db.runInTransaction {
            val repoId = db.getRepositoryDao().insertEmptyRepo("https://f-droid.org/repo")
            inputStream.use { indexStream ->
                indexProcessor.process(repoId, 42, indexStream)
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
    }

    @Test
    fun testExceptionWhileStreamingDoesNotSaveIntoDb() {
        val c = getApplicationContext<Context>()
        val cIn = CountingInputStream(c.resources.assets.open("index-v2.json"))
        val indexProcessor = IndexStreamProcessor(DbStreamReceiver(db) { true }, null) {
            if (cIn.byteCount > 824096) throw SerializationException()
            cIn.byteCount
        }

        assertFailsWith<SerializationException> {
            db.runInTransaction {
                cIn.use { indexStream ->
                    indexProcessor.process(1, 42, indexStream)
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
