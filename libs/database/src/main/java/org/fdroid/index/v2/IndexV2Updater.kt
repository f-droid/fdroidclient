package org.fdroid.index.v2

import org.fdroid.CompatibilityChecker
import org.fdroid.database.DbV2DiffStreamReceiver
import org.fdroid.database.DbV2StreamReceiver
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.FDroidDatabaseInt
import org.fdroid.database.Repository
import org.fdroid.download.DownloaderFactory
import org.fdroid.index.IndexFormatVersion
import org.fdroid.index.IndexFormatVersion.ONE
import org.fdroid.index.IndexFormatVersion.TWO
import org.fdroid.index.IndexParser
import org.fdroid.index.IndexUpdateListener
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.IndexUpdater
import org.fdroid.index.RepoUriBuilder
import org.fdroid.index.TempFileProvider
import org.fdroid.index.defaultRepoUriBuilder
import org.fdroid.index.parseEntry
import org.fdroid.index.setIndexUpdateListener

public const val SIGNED_FILE_NAME: String = "entry.jar"

public class IndexV2Updater(
    database: FDroidDatabase,
    private val tempFileProvider: TempFileProvider,
    private val downloaderFactory: DownloaderFactory,
    private val repoUriBuilder: RepoUriBuilder = defaultRepoUriBuilder,
    private val compatibilityChecker: CompatibilityChecker,
    private val listener: IndexUpdateListener? = null,
) : IndexUpdater() {

    public override val formatVersion: IndexFormatVersion = TWO
    private val db: FDroidDatabaseInt = database as FDroidDatabaseInt

    override fun update(
        repo: Repository,
        certificate: String?,
        fingerprint: String?,
    ): IndexUpdateResult {
        val (cert, entry) = getCertAndEntry(repo, certificate, fingerprint)
        // don't process repos that we already did process in the past
        if (entry.timestamp <= repo.timestamp) return IndexUpdateResult.Unchanged
        // get diff, if available
        val diff = entry.getDiff(repo.timestamp)
        return if (diff == null || repo.formatVersion == ONE) {
            // no diff found (or this is upgrade from v1 repo), so do full index update
            val streamReceiver = DbV2StreamReceiver(db, repo.repoId, compatibilityChecker)
            val streamProcessor = IndexV2FullStreamProcessor(streamReceiver, cert)
            processStream(repo, entry.index, entry.version, streamProcessor)
        } else {
            // use available diff
            val streamReceiver = DbV2DiffStreamReceiver(db, repo.repoId, compatibilityChecker)
            val streamProcessor = IndexV2DiffStreamProcessor(streamReceiver)
            processStream(repo, diff, entry.version, streamProcessor)
        }
    }

    private fun getCertAndEntry(
        repo: Repository,
        certificate: String?,
        fingerprint: String?,
    ): Pair<String, Entry> {
        val file = tempFileProvider.createTempFile()
        val downloader = downloaderFactory.createWithTryFirstMirror(
            repo = repo,
            uri = repoUriBuilder.getUri(repo, SIGNED_FILE_NAME),
            indexFile = FileV2.fromPath("/$SIGNED_FILE_NAME"),
            destFile = file,
        ).apply {
            setIndexUpdateListener(listener, repo)
        }
        try {
            downloader.download()
            val verifier = EntryVerifier(file, certificate, fingerprint)
            return verifier.getStreamAndVerify { inputStream ->
                IndexParser.parseEntry(inputStream)
            }
        } finally {
            file.delete()
        }
    }

    private fun processStream(
        repo: Repository,
        entryFile: EntryFileV2,
        repoVersion: Long,
        streamProcessor: IndexV2StreamProcessor,
    ): IndexUpdateResult {
        val file = tempFileProvider.createTempFile()
        val downloader = downloaderFactory.createWithTryFirstMirror(
            repo = repo,
            uri = repoUriBuilder.getUri(repo, entryFile.name.trimStart('/')),
            indexFile = entryFile,
            destFile = file,
        ).apply {
            setIndexUpdateListener(listener, repo)
        }
        try {
            downloader.download()
            file.inputStream().use { inputStream ->
                val repoDao = db.getRepositoryDao()
                db.runInTransaction {
                    streamProcessor.process(repoVersion, inputStream) { i ->
                        listener?.onUpdateProgress(repo, i, entryFile.numPackages)
                    }
                    // update RepositoryPreferences with timestamp
                    val repoPrefs = repoDao.getRepositoryPreferences(repo.repoId)
                        ?: error("No repo prefs for ${repo.repoId}")
                    val updatedPrefs = repoPrefs.copy(
                        lastUpdated = System.currentTimeMillis(),
                    )
                    repoDao.updateRepositoryPreferences(updatedPrefs)
                }
            }
        } finally {
            file.delete()
        }
        return IndexUpdateResult.Processed
    }
}
