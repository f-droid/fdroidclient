package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.SerializationException
import org.apache.commons.io.input.CountingInputStream
import org.fdroid.CompatibilityChecker
import org.fdroid.index.v2.IndexV2FullStreamProcessor
import org.fdroid.test.TestDataEmptyV2
import org.fdroid.test.TestDataMaxV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class IndexV2InsertTest : DbTest() {

    @Test
    fun testStreamEmptyIntoDb() {
        val repoId = streamIndexV2IntoDb("index-empty-v2.json")
        assertEquals(1, repoDao.getRepositories().size)
        assertDbEquals(repoId, TestDataEmptyV2.index)
    }

    @Test
    fun testStreamMinIntoDb() {
        val repoId = streamIndexV2IntoDb("index-min-v2.json")
        assertEquals(1, repoDao.getRepositories().size)
        assertDbEquals(repoId, TestDataMinV2.index)
    }

    @Test
    fun testStreamMinReorderedIntoDb() {
        val repoId = streamIndexV2IntoDb("index-min-reordered-v2.json")
        assertEquals(1, repoDao.getRepositories().size)
        assertDbEquals(repoId, TestDataMinV2.index)
    }

    @Test
    fun testStreamMidIntoDb() {
        val repoId = streamIndexV2IntoDb("index-mid-v2.json")
        assertEquals(1, repoDao.getRepositories().size)
        assertDbEquals(repoId, TestDataMidV2.index)
    }

    @Test
    fun testStreamMaxIntoDb() {
        val repoId = streamIndexV2IntoDb("index-max-v2.json")
        assertEquals(1, repoDao.getRepositories().size)
        assertDbEquals(repoId, TestDataMaxV2.index)
    }

    @Test
    fun testExceptionWhileStreamingDoesNotSaveIntoDb() {
        val cIn = CountingInputStream(assets.open("index-max-v2.json"))
        val compatibilityChecker = CompatibilityChecker {
            if (cIn.byteCount > 0) throw SerializationException()
            true
        }
        assertFailsWith<SerializationException> {
            db.runInTransaction {
                val repoId = db.getRepositoryDao().insertEmptyRepo("http://example.org")
                val streamReceiver = DbV2StreamReceiver(db, repoId, compatibilityChecker)
                val indexProcessor = IndexV2FullStreamProcessor(streamReceiver, "")
                cIn.use { indexStream ->
                    indexProcessor.process(42, indexStream) {}
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
