package org.fdroid.download

import android.net.Uri
import android.util.Log
import org.fdroid.IndexFile
import org.fdroid.database.Repository
import java.io.File
import java.io.IOException

/**
 * This is in the database library, because only that knows about the [Repository] class.
 */
public abstract class DownloaderFactory {

    /**
     * Same as [create], but trying canonical address first.
     *
     * See https://gitlab.com/fdroid/fdroidclient/-/issues/1708 for why this is still needed.
     */
    @Throws(IOException::class)
    public fun createWithTryFirstMirror(
        repo: Repository,
        uri: Uri,
        indexFile: IndexFile,
        destFile: File,
    ): Downloader {
        val tryFirst = repo.getMirrors().find { mirror ->
            mirror.baseUrl == repo.address
        }
        if (tryFirst == null) {
            Log.w("DownloaderFactory", "Try-first mirror not found, disabled by user?")
        }
        val mirrors: List<Mirror> = repo.getMirrors()
        return create(repo, mirrors, uri, indexFile, destFile, tryFirst)
    }

    @Throws(IOException::class)
    public abstract fun create(
        repo: Repository,
        uri: Uri,
        indexFile: IndexFile,
        destFile: File,
    ): Downloader

    @Throws(IOException::class)
    protected abstract fun create(
        repo: Repository,
        mirrors: List<Mirror>,
        uri: Uri,
        indexFile: IndexFile,
        destFile: File,
        tryFirst: Mirror?,
    ): Downloader

}
