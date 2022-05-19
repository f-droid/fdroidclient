package org.fdroid.index

import org.fdroid.database.IndexFormatVersion
import org.fdroid.database.Repository
import org.fdroid.download.Downloader
import org.fdroid.download.NotFoundException
import java.io.File
import java.io.IOException

public sealed class IndexUpdateResult {
    public object Unchanged : IndexUpdateResult()
    public object Processed : IndexUpdateResult()
    public object NotFound : IndexUpdateResult()
    public class Error(public val e: Exception) : IndexUpdateResult()
}

public interface IndexUpdateListener {
    public fun onDownloadProgress(repo: Repository, bytesRead: Long, totalBytes: Long)
    public fun onUpdateProgress(repo: Repository, appsProcessed: Int, totalApps: Int)
}

public fun interface TempFileProvider {
    @Throws(IOException::class)
    public fun createTempFile(): File
}

public abstract class IndexUpdater {

    public abstract val formatVersion: IndexFormatVersion

    public fun updateNewRepo(
        repo: Repository,
        expectedSigningFingerprint: String?,
    ): IndexUpdateResult = catchExceptions {
        update(repo, null, expectedSigningFingerprint)
    }

    public fun update(
        repo: Repository,
    ): IndexUpdateResult = catchExceptions {
        require(repo.certificate != null) { "Repo ${repo.address} had no certificate" }
        update(repo, repo.certificate, null)
    }

    private fun catchExceptions(block: () -> IndexUpdateResult): IndexUpdateResult {
        return try {
            block()
        } catch (e: NotFoundException) {
            IndexUpdateResult.NotFound
        } catch (e: Exception) {
            IndexUpdateResult.Error(e)
        }
    }

    protected abstract fun update(
        repo: Repository,
        certificate: String?,
        fingerprint: String?,
    ): IndexUpdateResult
}

internal fun Downloader.setIndexUpdateListener(
    listener: IndexUpdateListener?,
    repo: Repository,
) {
    if (listener != null) setListener { bytesRead, totalBytes ->
        listener.onDownloadProgress(repo, bytesRead, totalBytes)
    }
}
