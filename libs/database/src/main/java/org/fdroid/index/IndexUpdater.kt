package org.fdroid.index

import android.net.Uri
import org.fdroid.database.Repository
import org.fdroid.download.Downloader
import org.fdroid.download.NotFoundException
import java.io.File
import java.io.IOException

/**
 * The currently known (and supported) format versions of the F-Droid index.
 */
public enum class IndexFormatVersion { ONE, TWO }

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

public fun interface RepoUriBuilder {
    /**
     * Returns an [Uri] for downloading a file from the [Repository].
     * Allowing different implementations for this is useful for exotic repository locations
     * that do not allow for simple concatenation.
     */
    public fun getUri(repo: Repository, vararg pathElements: String): Uri
}

internal val defaultRepoUriBuilder = RepoUriBuilder { repo, pathElements ->
    val builder = Uri.parse(repo.address).buildUpon()
    pathElements.forEach { builder.appendEncodedPath(it) }
    builder.build()
}

public fun interface TempFileProvider {
    @Throws(IOException::class)
    public fun createTempFile(): File
}

/**
 * A class to update information of a [Repository] in the database with a new downloaded index.
 */
public abstract class IndexUpdater {

    /**
     * The [IndexFormatVersion] used by this updater.
     * One updater usually handles exactly one format version.
     * If you need a higher level of abstraction, check [RepoUpdater].
     */
    public abstract val formatVersion: IndexFormatVersion

    /**
     * Updates a new [repo] for the first time.
     */
    public fun updateNewRepo(
        repo: Repository,
        expectedSigningFingerprint: String?,
    ): IndexUpdateResult = catchExceptions {
        update(repo, null, expectedSigningFingerprint)
    }

    /**
     * Updates an existing [repo] with a known [Repository.certificate].
     */
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
