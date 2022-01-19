package org.fdroid.download.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import io.ktor.client.engine.ProxyConfig
import mu.KotlinLogging
import org.fdroid.download.HttpManager
import java.io.InputStream

@Deprecated("Use DownloadRequestLoader instead")
class HttpGlideUrlLoader(
    private val httpManager: HttpManager,
    private val proxyGetter: () -> ProxyConfig?,
) : ModelLoader<GlideUrl, InputStream> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun handles(url: GlideUrl): Boolean {
        return true
    }

    override fun buildLoadData(glideUrl: GlideUrl, width: Int, height: Int, options: Options): LoadData<InputStream> {
        log.warn { "Not using mirrors when loading $glideUrl" }
        return LoadData(glideUrl, HttpFetcher(httpManager, glideUrl, proxyGetter()))
    }

    class Factory(
        private val httpManager: HttpManager,
        private val proxyGetter: () -> ProxyConfig?,
    ) : ModelLoaderFactory<GlideUrl, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
            return HttpGlideUrlLoader(httpManager, proxyGetter)
        }

        override fun teardown() {}
    }

}
