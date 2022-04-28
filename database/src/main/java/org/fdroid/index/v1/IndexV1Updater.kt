package org.fdroid.index.v1

import android.content.Context
import org.fdroid.CompatibilityChecker
import org.fdroid.database.DbV1StreamReceiver
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.FDroidDatabaseInt
import org.fdroid.download.DownloaderFactory
import java.io.File
import java.io.IOException

internal const val SIGNED_FILE_NAME = "index-v1.jar"

// TODO should this live here and cause a dependency on download lib or in dedicated module?
public class IndexV1Updater(
    private val context: Context,
    database: FDroidDatabase,
    private val downloaderFactory: DownloaderFactory,
    private val compatibilityChecker: CompatibilityChecker,
) {

    private val db: FDroidDatabaseInt = database as FDroidDatabaseInt

    @Throws(IOException::class, InterruptedException::class)
    public fun updateNewRepo(
        repoId: Long,
        expectedSigningFingerprint: String?,
        updateListener: IndexUpdateListener? = null,
    ): IndexUpdateResult {
        return update(repoId, null, expectedSigningFingerprint, updateListener)
    }

    @Throws(IOException::class, InterruptedException::class)
    public fun update(
        repoId: Long,
        certificate: String,
        updateListener: IndexUpdateListener? = null,
    ): IndexUpdateResult {
        return update(repoId, certificate, null, updateListener)
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun update(
        repoId: Long,
        certificate: String?,
        fingerprint: String?,
        updateListener: IndexUpdateListener?,
    ): IndexUpdateResult {
        val repo =
            db.getRepositoryDao().getRepository(repoId) ?: error("Unexpected repoId: $repoId")
        val uri = repo.getCanonicalUri()
        val file = File.createTempFile("dl-", "", context.cacheDir)
        val downloader = downloaderFactory.createWithTryFirstMirror(repo, uri, file).apply {
            cacheTag = repo.lastETag
            updateListener?.let { setListener(updateListener::onDownloadProgress) }
        }
        try {
            downloader.download()
            // TODO in MirrorChooser don't try again on 404
            //  when tryFirstMirror is set == isRepoDownload
            if (!downloader.hasChanged()) return IndexUpdateResult.UNCHANGED
            val eTag = downloader.cacheTag

            val verifier = IndexV1Verifier(file, certificate, fingerprint)
            db.runInTransaction {
                val cert = verifier.getStreamAndVerify { inputStream ->
                    updateListener?.onStartProcessing() // TODO maybe do more fine-grained reporting
                    val streamReceiver = DbV1StreamReceiver(db, compatibilityChecker, repoId)
                    val streamProcessor = IndexV1StreamProcessor(streamReceiver, certificate)
                    streamProcessor.process(inputStream)
                }
                // update certificate, if we didn't have any before
                if (certificate == null) {
                    db.getRepositoryDao().updateRepository(repoId, cert)
                }
                // update RepositoryPreferences with timestamp and ETag (for v1)
                val updatedPrefs = repo.preferences.copy(
                    lastUpdated = System.currentTimeMillis(),
                    lastETag = eTag,
                )
                db.getRepositoryDao().updateRepositoryPreferences(updatedPrefs)
            }
        } finally {
            file.delete()
        }
        return IndexUpdateResult.PROCESSED
    }

}
