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
        return LoadData(downloadRequest.getObjectKey(), HttpFetcher(httpManager, downloadRequest))
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

internal fun DownloadRequest.getObjectKey(): ObjectKey {
    return ObjectKey(getKey())
}

internal fun DownloadRequest.getKey(): String {
    return indexFile.sha256 ?: (mirrors[0].baseUrl + indexFile.name)
}
