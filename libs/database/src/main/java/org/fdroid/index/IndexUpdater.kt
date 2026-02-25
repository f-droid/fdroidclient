package org.fdroid.index

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import org.fdroid.database.Repository
import org.fdroid.database.RepositoryDaoInt
import org.fdroid.download.Downloader
import org.fdroid.download.NotFoundException

/** The currently known (and supported) format versions of the F-Droid index. */
public enum class IndexFormatVersion {
  ONE,
  TWO,
}

public sealed class IndexUpdateResult {
  public object Unchanged : IndexUpdateResult()

  public object Processed : IndexUpdateResult()

  public object NotFound : IndexUpdateResult()

  public data class Error(public val e: Exception) : IndexUpdateResult()
}

public interface IndexUpdateListener {
  /** If [totalBytes] is 0 or less, it is unknown and indeterminate progress should be shown. */
  public fun onDownloadProgress(repo: Repository, bytesRead: Long, totalBytes: Long)

  public fun onUpdateProgress(repo: Repository, appsProcessed: Int, totalApps: Int)
}

public fun interface RepoUriBuilder {
  /**
   * Returns an [Uri] for downloading a file from the [Repository]. Allowing different
   * implementations for this is useful for exotic repository locations that do not allow for simple
   * concatenation.
   */
  public fun getUri(repo: Repository, vararg pathElements: String): Uri
}

internal val defaultRepoUriBuilder = RepoUriBuilder { repo, pathElements ->
  val builder = repo.address.toUri().buildUpon()
  pathElements.forEach { builder.appendEncodedPath(it) }
  builder.build()
}

public fun interface TempFileProvider {
  @Throws(IOException::class) public fun createTempFile(sha256: String?): File
}

/** A class to update information of a [Repository] in the database with a new downloaded index. */
public abstract class IndexUpdater {
  @VisibleForTesting internal abstract val repoDao: RepositoryDaoInt

  /**
   * The [IndexFormatVersion] used by this updater. One updater usually handles exactly one format
   * version. If you need a higher level of abstraction, check [RepoUpdater].
   */
  public abstract val formatVersion: IndexFormatVersion

  /** Updates an existing [repo] with a known [Repository.certificate]. */
  @WorkerThread
  public fun update(repo: Repository): IndexUpdateResult =
    catchExceptions(repo) {
      updateRepo(repo).also { result ->
        // reset repo errors if repo updated fine again, but is still unchanged
        if (repo.errorCount > 0 && result is IndexUpdateResult.Unchanged) {
          repoDao.resetRepoUpdateError(repo.repoId)
        }
      }
    }

  @WorkerThread protected abstract fun updateRepo(repo: Repository): IndexUpdateResult

  @WorkerThread
  private fun catchExceptions(repo: Repository, block: () -> IndexUpdateResult): IndexUpdateResult {
    return try {
      block()
    } catch (e: NotFoundException) {
      onError(repo.repoId, e)
      IndexUpdateResult.NotFound
    } catch (e: Exception) {
      onError(repo.repoId, e)
      IndexUpdateResult.Error(e)
    }
  }

  @WorkerThread
  private fun onError(repoId: Long, e: Exception) {
    val msg = buildString {
      append(e.localizedMessage ?: e.message ?: e.javaClass.simpleName)
      e.cause?.let { cause ->
        append("\n")
        append(cause.localizedMessage ?: cause.message ?: cause.javaClass.simpleName)
      }
    }
    repoDao.trackRepoUpdateError(repoId, msg)
  }
}

internal fun Downloader.setIndexUpdateListener(listener: IndexUpdateListener?, repo: Repository) {
  if (listener != null)
    setListener { bytesRead, totalBytes ->
      listener.onDownloadProgress(repo, bytesRead, totalBytes)
    }
}
