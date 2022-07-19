package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.SerializationException
import org.apache.commons.io.input.CountingInputStream
import org.fdroid.index.IndexConverter
import org.fdroid.index.v1.IndexV1StreamProcessor
import org.fdroid.index.v1.IndexV1StreamReceiver
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2
import org.fdroid.test.TestDataEmptyV1
import org.fdroid.test.TestDataMaxV1
import org.fdroid.test.TestDataMidV1
import org.fdroid.test.TestDataMinV1
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class IndexV1InsertTest : DbTest() {

    private val indexConverter = IndexConverter()

    @Test
    fun testStreamEmptyIntoDb() {
        val repoId = streamIndex("index-empty-v1.json")
        assertEquals(1, repoDao.getRepositories().size)
        val index = indexConverter.toIndexV2(TestDataEmptyV1.index)
        assertDbEquals(repoId, index)
    }

    @Test
    fun testStreamMinIntoDb() {
        val repoId = streamIndex("index-min-v1.json")
        assertTrue(repoDao.getRepositories().size == 1)
        val index = indexConverter.toIndexV2(TestDataMinV1.index)
        assertDbEquals(repoId, index)
    }

    @Test
    fun testStreamMidIntoDb() {
        val repoId = streamIndex("index-mid-v1.json")
        assertTrue(repoDao.getRepositories().size == 1)
        val index = indexConverter.toIndexV2(TestDataMidV1.index)
        assertDbEquals(repoId, index)
    }

    @Test
    fun testStreamMaxIntoDb() {
        val repoId = streamIndex("index-max-v1.json")
        assertTrue(repoDao.getRepositories().size == 1)
        val index = indexConverter.toIndexV2(TestDataMaxV1.index)
        assertDbEquals(repoId, index)
    }

    private fun streamIndex(path: String): Long {
        val repoId = db.getRepositoryDao().insertEmptyRepo("https://f-droid.org/repo")
        val streamReceiver = TestStreamReceiver(repoId)
        val indexProcessor = IndexV1StreamProcessor(streamReceiver, null, -1)
        db.runInTransaction {
            assets.open(path).use { indexStream ->
                indexProcessor.process(indexStream)
            }
        }
        return repoId
    }

    @Test
    fun testExceptionWhileStreamingDoesNotSaveIntoDb() {
        val cIn = CountingInputStream(assets.open("index-max-v1.json"))
        assertFailsWith<SerializationException> {
            db.runInTransaction {
                val repoId = db.getRepositoryDao().insertEmptyRepo("https://f-droid.org/repo")
                val streamReceiver = TestStreamReceiver(repoId) {
                    if (cIn.byteCount > 0) throw SerializationException()
                }
                val indexProcessor = IndexV1StreamProcessor(streamReceiver, null, -1)
                cIn.use { indexStream ->
                    indexProcessor.process(indexStream)
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

    @Suppress("DEPRECATION")
    inner class TestStreamReceiver(
        repoId: Long,
        private val callback: () -> Unit = {},
    ) : IndexV1StreamReceiver {
        private val streamReceiver = DbV1StreamReceiver(db, repoId) { true }
        override fun receive(repo: RepoV2, version: Long, certificate: String?) {
            streamReceiver.receive(repo, version, certificate)
            callback()
        }

        override fun receive(packageName: String, m: MetadataV2) {
            streamReceiver.receive(packageName, m)
            callback()
        }

        override fun receive(packageName: String, v: Map<String, PackageVersionV2>) {
            streamReceiver.receive(packageName, v)
            callback()
        }

        override fun updateRepo(
            antiFeatures: Map<String, AntiFeatureV2>,
            categories: Map<String, CategoryV2>,
            releaseChannels: Map<String, ReleaseChannelV2>,
        ) {
            streamReceiver.updateRepo(antiFeatures, categories, releaseChannels)
            callback()
        }

        override fun updateAppMetadata(packageName: String, preferredSigner: String?) {
            streamReceiver.updateAppMetadata(packageName, preferredSigner)
            callback()
        }

    }

}
