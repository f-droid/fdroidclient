/**
 * Contains disk cache related code from https://github.com/coil-kt/coil
 * coil-network-core/src/commonMain/kotlin/coil3/network/NetworkFetcher.kt
 * under Apache-2.0 license.
 */

package org.fdroid.download.coil

import coil3.ImageLoader
import coil3.annotation.InternalCoilApi
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.util.MimeTypeMap
import io.ktor.utils.io.jvm.javaio.toInputStream
import okio.BufferedSource
import okio.FileSystem
import okio.buffer
import okio.source
import org.fdroid.download.DownloadRequest
import org.fdroid.download.HttpManager
import org.fdroid.download.glide.AutoVerifyingInputStream
import org.fdroid.download.glide.getKey
import javax.inject.Inject

public class DownloadRequestFetcher(
    private val httpManager: HttpManager,
    private val downloadRequest: DownloadRequest,
    private val options: Options,
    private val diskCache: Lazy<DiskCache?>,
) : Fetcher {

    private val fileSystem: FileSystem
        get() = diskCache.value?.fileSystem ?: options.fileSystem

    private val diskCacheKey: String
        get() = options.diskCacheKey ?: downloadRequest.getKey()

    @OptIn(InternalCoilApi::class)
    private val mimeType: String?
        get() = MimeTypeMap.getMimeTypeFromUrl(downloadRequest.indexFile.name)

    override suspend fun fetch(): FetchResult? {
        var snapshot = readFromDiskCache()
        try {
            if (snapshot != null) {
                // we have the request cached, so return it right away
                return SourceFetchResult(
                    source = snapshot.toImageSource(),
                    mimeType = mimeType,
                    dataSource = DataSource.DISK,
                )
            }
            // TODO use channel directly and auto-verify hash without InputStream wrapper
            //  may need https://github.com/Kotlin/kotlinx-io/blob/master/integration/kotlinx-io-okio/Module.md
            val inputStream = httpManager.getChannel(downloadRequest).toInputStream()
            val sha256 = downloadRequest.indexFile.sha256
            val bufferedSource = if (sha256 == null) {
                inputStream
            } else {
                AutoVerifyingInputStream(inputStream, sha256)
            }.source().buffer()
            snapshot = writeToDiskCache(snapshot, bufferedSource)
            if (snapshot == null) {
                // we couldn't write the snapshot, so try returning directly
                return SourceFetchResult(
                    source = ImageSource(
                        source = bufferedSource,
                        fileSystem = FileSystem.SYSTEM,
                        metadata = null,
                    ),
                    mimeType = mimeType,
                    dataSource = DataSource.NETWORK,
                )
            }
            return SourceFetchResult(
                source = snapshot.toImageSource(),
                mimeType = mimeType,
                dataSource = DataSource.NETWORK,
            )
        } finally {
            snapshot?.close()
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            diskCache.value?.openSnapshot(downloadRequest.getKey())
        } else {
            null
        }
    }

    private fun writeToDiskCache(
        snapshot: DiskCache.Snapshot?,
        bufferedSource: BufferedSource,
    ): DiskCache.Snapshot? {
        // Short circuit if we're not allowed to cache this response.
        if (!options.diskCachePolicy.writeEnabled) return null

        // Open a new editor. Return null if we're unable to write to this entry.
        val editor = if (snapshot != null) {
            snapshot.closeAndOpenEditor()
        } else {
            diskCache.value?.openEditor(diskCacheKey)
        } ?: return null

        return try {
            fileSystem.write(editor.data) {
                writeAll(bufferedSource)
            }
            editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (_: Exception) {
                // ignore
            }
            throw e
        }
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(
            file = data,
            fileSystem = fileSystem,
            diskCacheKey = diskCacheKey,
            closeable = this,
        )
    }

    public class Factory @Inject constructor(
        private val httpManager: HttpManager,
    ) : Fetcher.Factory<DownloadRequest> {
        override fun create(
            data: DownloadRequest,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? = DownloadRequestFetcher(
            httpManager = httpManager,
            downloadRequest = data,
            options = options,
            diskCache = lazy { imageLoader.diskCache },
        )
    }
}
