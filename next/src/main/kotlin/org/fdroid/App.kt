package org.fdroid

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.key.Keyer
import coil3.memory.MemoryCache
import coil3.request.Options
import coil3.request.crossfade
import coil3.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import org.fdroid.download.DownloadRequest
import org.fdroid.download.LocalIconFetcher
import org.fdroid.download.PackageName
import org.fdroid.download.coil.DownloadRequestFetcher
import org.fdroid.next.BuildConfig
import org.fdroid.repo.RepoUpdateWorker
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var downloadRequestFetcherFactory: DownloadRequestFetcher.Factory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun onCreate() {
        super.onCreate()
        Composer.setDiagnosticStackTraceEnabled(BuildConfig.DEBUG)
        RepoUpdateWorker.Companion.scheduleOrCancel(applicationContext)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                val downloadRequestKeyer = object : Keyer<DownloadRequest> {
                    override fun key(
                        data: DownloadRequest,
                        options: Options
                    ): String {
                        return data.indexFile.sha256
                            ?: (data.mirrors[0].baseUrl + data.indexFile.name)
                    }
                }
                add(downloadRequestKeyer)
                add(downloadRequestFetcherFactory)

                val packageNameKeyer = object : Keyer<PackageName> {
                    override fun key(data: PackageName, options: Options): String = data.packageName
                }
                add(packageNameKeyer)
                add(LocalIconFetcher.Factory(context, downloadRequestFetcherFactory))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                // TODO disk cache needs to be manually filled by the Fetcher
                //  this is not automatic like it is with Glide
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
            .build()
    }
}
