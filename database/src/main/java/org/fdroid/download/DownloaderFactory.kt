package org.fdroid.download

import android.net.Uri
import android.util.Log
import org.fdroid.database.Repository
import java.io.File
import java.io.IOException

public abstract class DownloaderFactory {

    /**
     * Same as [create], but trying canonical address first.
     *
     * See https://gitlab.com/fdroid/fdroidclient/-/issues/1708 for why this is still needed.
     */
    @Throws(IOException::class)
    fun createWithTryFirstMirror(repo: Repository, uri: Uri, destFile: File): Downloader {
        val tryFirst = repo.getMirrors().find { mirror ->
            mirror.baseUrl == repo.address
        }
        if (tryFirst == null) {
            Log.w("DownloaderFactory", "Try-first mirror not found, disabled by user?")
        }
        val mirrors: List<Mirror> = repo.getMirrors()
        return create(repo, mirrors, uri, destFile, tryFirst)
    }

    @Throws(IOException::class)
    abstract fun create(repo: Repository, uri: Uri, destFile: File): Downloader

    @Throws(IOException::class)
    protected abstract fun create(
        repo: Repository,
        mirrors: List<Mirror>,
        uri: Uri,
        destFile: File,
        tryFirst: Mirror?,
    ): Downloader

}
