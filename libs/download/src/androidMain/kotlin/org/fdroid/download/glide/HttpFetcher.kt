package org.fdroid.download.glide

import android.net.Uri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import io.ktor.client.engine.ProxyConfig
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fdroid.download.DownloadRequest
import org.fdroid.download.HttpManager
import org.fdroid.download.Mirror
import java.io.InputStream

internal class HttpFetcher(
    private val httpManager: HttpManager,
    private val downloadRequest: DownloadRequest,
) : DataFetcher<InputStream> {

    @Deprecated("Use DownloadRequests with other constructor instead")
    constructor(
        httpManager: HttpManager,
        glideUrl: GlideUrl,
        proxy: ProxyConfig?,
    ) : this(httpManager, getDownloadRequest(glideUrl, proxy))

    companion object {
        private fun getDownloadRequest(glideUrl: GlideUrl, proxy: ProxyConfig?): DownloadRequest {
            val (mirror, path) = glideUrl.toStringUrl().split("/repo/")
            return DownloadRequest(Uri.decode(path), listOf(Mirror("$mirror/repo")), proxy)
        }
    }

    var job: Job? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                // glide should take care of closing this stream and the underlying channel
                val inputStream = httpManager.getChannel(downloadRequest).toInputStream()
                callback.onDataReady(inputStream)
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
