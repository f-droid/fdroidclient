@file:Suppress("DEPRECATION")

package org.fdroid.index.v1

import mu.KotlinLogging
import org.fdroid.CompatibilityChecker
import org.fdroid.database.DbV1StreamReceiver
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.FDroidDatabaseInt
import org.fdroid.database.Repository
import org.fdroid.download.DownloaderFactory
import org.fdroid.index.IndexFormatVersion
import org.fdroid.index.IndexFormatVersion.ONE
import org.fdroid.index.IndexUpdateListener
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.IndexUpdater
import org.fdroid.index.RepoUriBuilder
import org.fdroid.index.TempFileProvider
import org.fdroid.index.defaultRepoUriBuilder
import org.fdroid.index.setIndexUpdateListener
import org.fdroid.index.v2.FileV2

public const val SIGNED_FILE_NAME: String = "index-v1.jar"

public class IndexV1Updater(
    database: FDroidDatabase,
    private val tempFileProvider: TempFileProvider,
    private val downloaderFactory: DownloaderFactory,
    private val repoUriBuilder: RepoUriBuilder = defaultRepoUriBuilder,
    private val compatibilityChecker: CompatibilityChecker,
    private val listener: IndexUpdateListener? = null,
) : IndexUpdater() {

    private val log = KotlinLogging.logger {}
    public override val formatVersion: IndexFormatVersion = ONE
    private val db: FDroidDatabaseInt = database as FDroidDatabaseInt

    override fun update(
        repo: Repository,
        certificate: String?,
        fingerprint: String?,
    ): IndexUpdateResult {
        // Normally, we shouldn't allow repository downgrades and assert the condition below.
        // However, F-Droid is concerned that late v2 bugs will require users to downgrade to v1,
        // as it happened already with the migration from v0 to v1.
        if (repo.formatVersion != null && repo.formatVersion != ONE) {
            log.error { "Format downgrade for ${repo.address}" }
        }
        val file = tempFileProvider.createTempFile()
        val downloader = downloaderFactory.createWithTryFirstMirror(
            repo = repo,
            uri = repoUriBuilder.getUri(repo, SIGNED_FILE_NAME),
            indexFile = FileV2.fromPath("/$SIGNED_FILE_NAME"),
            destFile = file,
        ).apply {
            cacheTag = repo.lastETag
            setIndexUpdateListener(listener, repo)
        }
        try {
            downloader.download()
            if (!downloader.hasChanged()) return IndexUpdateResult.Unchanged
            val eTag = downloader.cacheTag

            val verifier = IndexV1Verifier(file, certificate, fingerprint)
            db.runInTransaction {
                val (cert, _) = verifier.getStreamAndVerify { inputStream ->
                    listener?.onUpdateProgress(repo, 0, 0)
                    val streamReceiver = DbV1StreamReceiver(db, repo.repoId, compatibilityChecker)
                    val streamProcessor =
                        IndexV1StreamProcessor(streamReceiver, certificate, repo.timestamp)
                    streamProcessor.process(inputStream)
                }
                // update certificate, if we didn't have any before
                val repoDao = db.getRepositoryDao()
                if (certificate == null) {
                    repoDao.updateRepository(repo.repoId, cert)
                }
                // update RepositoryPreferences with timestamp and ETag (for v1)
                val updatedPrefs = repo.preferences.copy(
                    lastUpdated = System.currentTimeMillis(),
                    lastETag = eTag,
                )
                repoDao.updateRepositoryPreferences(updatedPrefs)
            }
        } catch (e: OldIndexException) {
            if (e.isSameTimestamp) return IndexUpdateResult.Unchanged
            else throw e
        } finally {
            file.delete()
        }
        return IndexUpdateResult.Processed
    }
}
