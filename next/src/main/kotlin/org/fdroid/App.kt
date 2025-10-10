package org.fdroid

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.tooling.ComposeStackTraceMode
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
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat.JSON
import org.acra.ktx.initAcra
import org.fdroid.download.DownloadRequest
import org.fdroid.download.LocalIconFetcher
import org.fdroid.download.PackageName
import org.fdroid.download.coil.DownloadRequestFetcher
import org.fdroid.next.BuildConfig
import org.fdroid.next.BuildConfig.APPLICATION_ID
import org.fdroid.next.BuildConfig.VERSION_NAME
import org.fdroid.next.R
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.ui.CrashActivity
import org.fdroid.updates.AppUpdateWorker
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

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initAcra {
            reportFormat = JSON
            reportContent = listOf(
                ReportField.USER_COMMENT,
                ReportField.PACKAGE_NAME,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PRODUCT,
                ReportField.BRAND,
                ReportField.PHONE_MODEL,
                ReportField.DISPLAY,
                ReportField.TOTAL_MEM_SIZE,
                ReportField.AVAILABLE_MEM_SIZE,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE_HASH,
                ReportField.STACK_TRACE,
            )
            reportSendFailureToast = getString(R.string.crash_report_error)
            sendReportsInDevMode = true
            dialog {
                reportDialogClass = CrashActivity::class.java
            }
            mailSender {
                mailTo = BuildConfig.ACRA_REPORT_EMAIL
                subject = "$APPLICATION_ID $VERSION_NAME: Crash Report"
                reportFileName = "ACRA-report.stacktrace.json"
            }
        }
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)
        }
        // bail out here if we are the ACRA process to not initialize anything in crash process
        if (ACRA.isACRASenderServiceProcess()) return

        RepoUpdateWorker.scheduleOrCancel(applicationContext)
        AppUpdateWorker.scheduleOrCancel(applicationContext)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                val downloadRequestKeyer = object : Keyer<DownloadRequest> {
                    override fun key(data: DownloadRequest, options: Options): String {
                        return data.getCacheKey()
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

fun DownloadRequest.getCacheKey() = indexFile.sha256 ?: (mirrors[0].baseUrl + indexFile.name)
