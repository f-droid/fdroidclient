package org.fdroid.index.v2

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.fdroid.CompatibilityChecker
import org.fdroid.database.DbTest
import org.fdroid.database.IndexFormatVersion.TWO
import org.fdroid.database.Repository
import org.fdroid.download.Downloader
import org.fdroid.download.DownloaderFactory
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.SigningException
import org.fdroid.index.TempFileProvider
import org.fdroid.test.TestDataEntryV2
import org.fdroid.test.TestDataMaxV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.fdroid.test.VerifierConstants.CERTIFICATE
import org.fdroid.test.VerifierConstants.FINGERPRINT
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
internal class IndexV2UpdaterTest : DbTest() {

    @get:Rule
    var tmpFolder: TemporaryFolder = TemporaryFolder()

    private val tempFileProvider: TempFileProvider = mockk()
    private val downloaderFactory: DownloaderFactory = mockk()
    private val downloader: Downloader = mockk()
    private val compatibilityChecker: CompatibilityChecker = CompatibilityChecker { true }
    private lateinit var indexUpdater: IndexV2Updater

    @Before
    override fun createDb() {
        super.createDb()
        indexUpdater = IndexV2Updater(db, tempFileProvider, downloaderFactory, compatibilityChecker)
    }

    @Test
    fun testFullIndexEmptyToMin() {
        val repoId = repoDao.insertEmptyRepo("http://example.org")
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-min/entry.jar",
            jsonPath = "resources/index-min-v2.json",
            entryFileV2 = TestDataEntryV2.emptyToMin.index
        )
        val result = indexUpdater.updateNewRepo(repo, FINGERPRINT).noError()
        assertEquals(IndexUpdateResult.Processed, result)
        assertDbEquals(repoId, TestDataMinV2.index)

        // check that certificate and format version got entered
        val updatedRepo = repoDao.getRepository(repoId) ?: fail()
        assertEquals(TWO, updatedRepo.formatVersion)
        assertEquals(CERTIFICATE, updatedRepo.certificate)
    }

    @Test
    fun testFullIndexEmptyToMid() {
        val repoId = repoDao.insertEmptyRepo("http://example.org")
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-mid/entry.jar",
            jsonPath = "resources/index-mid-v2.json",
            entryFileV2 = TestDataEntryV2.emptyToMid.index
        )
        val result = indexUpdater.updateNewRepo(repo, FINGERPRINT).noError()
        assertEquals(IndexUpdateResult.Processed, result)
        assertDbEquals(repoId, TestDataMidV2.index)
    }

    @Test
    fun testFullIndexEmptyToMax() {
        val repoId = repoDao.insertEmptyRepo("http://example.org")
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-max/entry.jar",
            jsonPath = "resources/index-max-v2.json",
            entryFileV2 = TestDataEntryV2.emptyToMax.index
        )
        val result = indexUpdater.updateNewRepo(repo, FINGERPRINT).noError()
        assertEquals(IndexUpdateResult.Processed, result)
        assertDbEquals(repoId, TestDataMaxV2.index)
    }

    @Test
    fun testDiffMinToMid() {
        val repoId = streamIndexV2IntoDb("resources/index-min-v2.json")
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-mid/entry.jar",
            jsonPath = "resources/diff-empty-mid/42.json",
            entryFileV2 = TestDataEntryV2.emptyToMid.diffs["42"] ?: fail()
        )
        val result = indexUpdater.update(repo).noError()
        assertEquals(IndexUpdateResult.Processed, result)
        assertDbEquals(repoId, TestDataMidV2.index)
    }

    @Test
    fun testDiffEmptyToMin() {
        val repoId = streamIndexV2IntoDb("resources/index-empty-v2.json")
        repoDao.updateRepository(repoId, CERTIFICATE)
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-min/entry.jar",
            jsonPath = "resources/diff-empty-min/23.json",
            entryFileV2 = TestDataEntryV2.emptyToMin.diffs["23"] ?: fail()
        )
        val result = indexUpdater.update(repo).noError()
        assertEquals(IndexUpdateResult.Processed, result)
        assertDbEquals(repoId, TestDataMinV2.index)
    }

    @Test
    fun testDiffMidToMax() {
        val repoId = streamIndexV2IntoDb("resources/index-mid-v2.json")
        repoDao.updateRepository(repoId, CERTIFICATE)
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-max/entry.jar",
            jsonPath = "resources/diff-empty-max/1337.json",
            entryFileV2 = TestDataEntryV2.emptyToMax.diffs["1337"] ?: fail()
        )
        val result = indexUpdater.update(repo).noError()
        assertEquals(IndexUpdateResult.Processed, result)
        assertDbEquals(repoId, TestDataMaxV2.index)
    }

    @Test
    fun testSameTimestampUnchanged() {
        val repoId = streamIndexV2IntoDb("resources/index-min-v2.json")
        repoDao.updateRepository(repoId, CERTIFICATE)
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-min/entry.jar",
            jsonPath = "resources/diff-empty-min/23.json",
            entryFileV2 = TestDataEntryV2.emptyToMin.diffs["23"] ?: fail()
        )
        val result = indexUpdater.update(repo).noError()
        assertEquals(IndexUpdateResult.Unchanged, result)
        assertDbEquals(repoId, TestDataMinV2.index)
    }

    @Test
    fun testHigherTimestampUnchanged() {
        val repoId = streamIndexV2IntoDb("resources/index-mid-v2.json")
        repoDao.updateRepository(repoId, CERTIFICATE)
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-min/entry.jar",
            jsonPath = "resources/diff-empty-min/23.json",
            entryFileV2 = TestDataEntryV2.emptyToMin.diffs["23"] ?: fail()
        )
        val result = indexUpdater.update(repo).noError()
        assertEquals(IndexUpdateResult.Unchanged, result)
        assertDbEquals(repoId, TestDataMidV2.index)
    }

    @Test
    fun testNoDiffFoundIndexFallback() {
        val repoId = streamIndexV2IntoDb("resources/index-empty-v2.json")
        repoDao.updateRepository(repoId, CERTIFICATE)
        // fake timestamp of internal repo, so we will fail to find a diff in entry.json
        val newRepo = repoDao.getRepository(repoId)?.repository?.copy(timestamp = 22) ?: fail()
        repoDao.updateRepository(newRepo)
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-min/entry.jar",
            jsonPath = "resources/index-min-v2.json",
            entryFileV2 = TestDataEntryV2.emptyToMin.index
        )
        val result = indexUpdater.update(repo).noError()
        assertEquals(IndexUpdateResult.Processed, result)
        assertDbEquals(repoId, TestDataMinV2.index)
    }

    @Test
    fun testWrongFingerprint() {
        val repoId = repoDao.insertEmptyRepo("http://example.org")
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-min/entry.jar",
            jsonPath = "resources/index-min-v2.json",
            entryFileV2 = TestDataEntryV2.emptyToMin.index
        )
        val result = indexUpdater.updateNewRepo(repo, "wrong fingerprint")
        assertTrue(result is IndexUpdateResult.Error)
        assertTrue(result.e is SigningException)
    }

    @Test
    fun testNormalUpdateOnRepoWithMissingFingerprint() {
        val repoId = repoDao.insertEmptyRepo("http://example.org")
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-min/entry.jar",
            jsonPath = "resources/index-min-v2.json",
            entryFileV2 = TestDataEntryV2.emptyToMin.index
        )
        val result = indexUpdater.update(repo)
        assertTrue(result is IndexUpdateResult.Error)
        assertTrue(result.e is IllegalArgumentException)
    }

    /**
     * Ensures that a v1 repo can't use a diff when upgrading to v1,
     * but must use a full index update.
     */
    @Test
    fun testV1ToV2ForcesFullUpdateEvenIfDiffExists() {
        val repoId = streamIndexV1IntoDb("resources/index-min-v1.json")
        val repo = prepareUpdate(
            repoId = repoId,
            entryPath = "resources/diff-empty-mid/entry.jar",
            jsonPath = "resources/index-mid-v2.json",
            entryFileV2 = TestDataEntryV2.emptyToMid.index,
        )
        val result = indexUpdater.update(repo).noError()
        assertEquals(IndexUpdateResult.Processed, result)
        assertDbEquals(repoId, TestDataMidV2.index)

        // check that format version got upgraded
        val updatedRepo = repoDao.getRepository(repoId) ?: fail()
        assertEquals(TWO, updatedRepo.formatVersion)
    }

    private fun prepareUpdate(
        repoId: Long,
        entryPath: String,
        jsonPath: String,
        entryFileV2: EntryFileV2,
    ): Repository {
        val entryFile = tmpFolder.newFile()
        val indexFile = tmpFolder.newFile()
        val repo = repoDao.getRepository(repoId) ?: fail()
        val entryUri = Uri.parse("${repo.address}/entry.jar")
        val indexUri = Uri.parse("${repo.address}/${entryFileV2.name.trimStart('/')}")

        assets.open(entryPath).use { inputStream ->
            entryFile.outputStream().use { inputStream.copyTo(it) }
        }
        assets.open(jsonPath).use { inputStream ->
            indexFile.outputStream().use { inputStream.copyTo(it) }
        }

        every { tempFileProvider.createTempFile() } returnsMany listOf(entryFile, indexFile)
        every {
            downloaderFactory.createWithTryFirstMirror(repo, entryUri, entryFile)
        } returns downloader
        every { downloader.download(-1) } just Runs
        every {
            downloaderFactory.createWithTryFirstMirror(repo, indexUri, indexFile)
        } returns downloader
        every { downloader.download(entryFileV2.size, entryFileV2.sha256) } just Runs

        return repo
    }

    /**
     * Easier for debugging, if we throw the index error.
     */
    private fun IndexUpdateResult.noError(): IndexUpdateResult {
        if (this is IndexUpdateResult.Error) throw e
        return this
    }

}
