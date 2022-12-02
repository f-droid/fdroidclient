package org.fdroid.download.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import org.fdroid.download.DownloadRequest
import org.fdroid.download.HttpManager
import java.io.InputStream

public class DownloadRequestLoader(
    private val httpManager: HttpManager,
) : ModelLoader<DownloadRequest, InputStream> {

    override fun handles(downloadRequest: DownloadRequest): Boolean {
        return true
    }

    override fun buildLoadData(
        downloadRequest: DownloadRequest,
        width: Int,
        height: Int,
        options: Options,
    ): LoadData<InputStream> {
        return LoadData(downloadRequest.getKey(), HttpFetcher(httpManager, downloadRequest))
    }

    public class Factory(
        private val httpManager: HttpManager,
    ) : ModelLoaderFactory<DownloadRequest, InputStream> {
        override fun build(
            multiFactory: MultiModelLoaderFactory,
        ): ModelLoader<DownloadRequest, InputStream> {
            return DownloadRequestLoader(httpManager)
        }

        override fun teardown() {}
    }

}

internal fun DownloadRequest.getKey(): ObjectKey {
    // TODO should we always choose a unique key
    //  or is it ok for this to work cross-repo based on file path only?
    return ObjectKey(indexFile.sha256 ?: indexFile.name)
}
