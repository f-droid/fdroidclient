package org.fdroid.index.v1

import android.content.Context
import org.fdroid.database.DbV1StreamReceiver
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.Downloader
import org.fdroid.index.IndexV1StreamProcessor
import java.io.File
import java.io.IOException

// TODO should this live here and cause a dependency on download lib or in dedicated module?
public class IndexV1Updater(
    context: Context,
    private val file: File,
    private val downloader: Downloader,
    ) {

    private val db: FDroidDatabase = FDroidDatabase.getDb(context, "test") // TODO final name

    @Throws(IOException::class, InterruptedException::class)
    fun update(address: String, expectedSigningFingerprint: String?) {
        val repoId = db.getRepositoryDaoInt().insertEmptyRepo(address)
        try {
            update(repoId, null, expectedSigningFingerprint)
        } catch (e: Throwable) {
            db.getRepositoryDaoInt().deleteRepository(repoId)
            throw e
        }
        db.getRepositoryDaoInt().getRepositories().forEach { println(it) }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun update(repoId: Long, certificate: String) {
        update(repoId, certificate, null)
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun update(repoId: Long, certificate: String?, fingerprint: String?) {
        downloader.download()
        val verifier = IndexV1Verifier(file, certificate, fingerprint)
        db.runInTransaction {
            verifier.getStreamAndVerify { inputStream ->
                val streamProcessor = IndexV1StreamProcessor(DbV1StreamReceiver(db))
                streamProcessor.process(repoId, inputStream)
            }
        }
    }

}
