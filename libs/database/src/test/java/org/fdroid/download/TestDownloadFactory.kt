package org.fdroid.download

import android.net.Uri
import org.fdroid.IndexFile
import org.fdroid.database.Repository
import java.io.File

internal class TestDownloadFactory(private val httpManager: HttpManager) : DownloaderFactory() {
    override fun create(
        repo: Repository,
        uri: Uri,
        indexFile: IndexFile,
        destFile: File,
    ): Downloader = HttpDownloaderV2(
        httpManager = httpManager,
        request = DownloadRequest(indexFile, repo.getMirrors()),
        destFile = destFile
    )

    override fun create(
        repo: Repository,
        mirrors: List<Mirror>,
        uri: Uri,
        indexFile: IndexFile,
        destFile: File,
        tryFirst: Mirror?,
    ): Downloader = HttpDownloaderV2(
        httpManager = httpManager,
        request = DownloadRequest(indexFile, repo.getMirrors()),
        destFile = destFile
    )
}
