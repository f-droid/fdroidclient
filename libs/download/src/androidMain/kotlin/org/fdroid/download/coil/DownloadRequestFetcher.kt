package org.fdroid.download.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import io.ktor.utils.io.jvm.javaio.toInputStream
import okio.FileSystem
import okio.buffer
import okio.source
import org.fdroid.download.DownloadRequest
import org.fdroid.download.HttpManager
import org.fdroid.download.glide.AutoVerifyingInputStream
import javax.inject.Inject

public class DownloadRequestFetcher(
    private val httpManager: HttpManager,
    private val downloadRequest: DownloadRequest,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        // TODO use channel directly and auto-verify hash without InputStream wrapper
        //  may need https://github.com/Kotlin/kotlinx-io/blob/master/integration/kotlinx-io-okio/Module.md
        val inputStream = httpManager.getChannel(downloadRequest).toInputStream()
        val sha256 = downloadRequest.indexFile.sha256
        val resultStream = if (sha256 == null) {
            inputStream
        } else {
            AutoVerifyingInputStream(inputStream, sha256)
        }
        return SourceFetchResult(
            source = ImageSource(
                source = resultStream.source().buffer(),
                fileSystem = FileSystem.SYSTEM,
                metadata = null,
            ),
            mimeType = null,
            dataSource = DataSource.NETWORK,
        )
    }

    public class Factory @Inject constructor(
        private val httpManager: HttpManager,
    ) : Fetcher.Factory<DownloadRequest> {
        override fun create(
            data: DownloadRequest,
            options: coil3.request.Options,
            imageLoader: ImageLoader
        ): Fetcher? = DownloadRequestFetcher(httpManager, data)
    }
}
