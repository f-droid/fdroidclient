package org.fdroid.download.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fdroid.download.DownloadRequest
import org.fdroid.download.HttpManager
import java.io.InputStream

internal class HttpFetcher(
    private val httpManager: HttpManager,
    private val downloadRequest: DownloadRequest,
) : DataFetcher<InputStream> {

    private var job: Job? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                // glide should take care of closing this stream and the underlying channel
                val inputStream = httpManager.getChannel(downloadRequest).toInputStream()
                val sha256 = downloadRequest.indexFile.sha256
                if (sha256 == null) {
                    callback.onDataReady(inputStream)
                } else {
                    callback.onDataReady(AutoVerifyingInputStream(inputStream, sha256))
                }
            } catch (e: Exception) {
                callback.onLoadFailed(e)
            }
        }
    }

    override fun cleanup() {
        job = null
    }

    override fun cancel() {
        job?.cancel()
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.REMOTE
    }
}
