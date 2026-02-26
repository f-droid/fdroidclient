package org.fdroid.download

import android.content.ContentResolver.SCHEME_FILE
import android.net.Uri
import org.fdroid.IndexFile
import org.fdroid.database.Repository
import org.fdroid.index.IndexFormatVersion
import org.fdroid.settings.SettingsManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloaderFactoryImpl @Inject constructor(
    private val httpManager: HttpManager,
    private val settingsManager: SettingsManager,
    private val interceptor: DownloadRequestInterceptor,
) : DownloaderFactory() {
    override fun create(
        repo: Repository,
        uri: Uri,
        indexFile: IndexFile,
        destFile: File
    ): Downloader {
        return create(repo, repo.getMirrors(), uri, indexFile, destFile, null)
    }

    override fun create(
        repo: Repository,
        mirrors: List<Mirror>,
        uri: Uri,
        indexFile: IndexFile,
        destFile: File,
        tryFirst: Mirror?
    ): Downloader {
        val request = DownloadRequest(
            indexFile = indexFile,
            mirrors = mirrors,
            proxy = settingsManager.proxyConfig,
            username = repo.username,
            password = repo.password,
            tryFirstMirror = tryFirst,
        )
        val v1OrUnknown = repo.formatVersion == null || repo.formatVersion == IndexFormatVersion.ONE
        return if (uri.scheme == SCHEME_FILE) {
            LocalFileDownloader(uri, indexFile, destFile)
        } else if (v1OrUnknown) {
            @Suppress("DEPRECATION") // v1 only
            HttpDownloader(httpManager, request, destFile)
        } else {
            HttpDownloaderV2(httpManager, interceptor.intercept(request), destFile)
        }
    }
}
