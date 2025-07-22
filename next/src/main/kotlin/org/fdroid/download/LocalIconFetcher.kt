package org.fdroid.download

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import mu.KotlinLogging
import org.fdroid.download.coil.DownloadRequestFetcher
import javax.inject.Inject

data class PackageName(val packageName: String, val iconDownloadRequest: DownloadRequest?)

class LocalIconFetcher(
    private val packageManager: PackageManager,
    private val data: PackageName,
    private val downloadRequestFetcher: Fetcher?,
) : Fetcher {

    private val log = KotlinLogging.logger { }

    override suspend fun fetch(): FetchResult? {
        val drawable = try {
            val info = packageManager.getApplicationInfo(data.packageName, 0)
            info.loadUnbadgedIcon(packageManager)
        } catch (e: PackageManager.NameNotFoundException) {
            log.error(e) { "Error getting icon from packageManager: " }
            return downloadRequestFetcher?.fetch()
        }

        if (SDK_INT >= 30 && packageManager.isDefaultApplicationIcon(drawable)) {
            log.warn {
                "Could not extract image for ${data.packageName}"
            }
            return downloadRequestFetcher?.fetch()
        }
        return ImageFetchResult(
            image = drawable.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory @Inject constructor(
        private val context: Context,
        private val downloadRequestFetcherFactory: DownloadRequestFetcher.Factory,
    ) : Fetcher.Factory<PackageName> {
        override fun create(
            data: PackageName,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? = LocalIconFetcher(
            packageManager = context.packageManager,
            data = data,
            downloadRequestFetcher = data.iconDownloadRequest?.let {
                downloadRequestFetcherFactory.create(it, options, imageLoader)
            },
        )
    }
}
