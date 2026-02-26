package org.fdroid

import android.app.Application
import android.content.Context
import android.os.Build.VERSION.SDK_INT
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
import coil3.request.crossfade
import coil3.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat.JSON
import org.acra.ktx.initAcra
import org.fdroid.BuildConfig.APPLICATION_ID
import org.fdroid.BuildConfig.VERSION_NAME
import org.fdroid.download.DownloadRequest
import org.fdroid.download.LocalIconFetcher
import org.fdroid.download.PackageName
import org.fdroid.download.coil.DownloadRequestFetcher
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.crash.CrashActivity
import org.fdroid.ui.crash.NoRetryPolicy
import org.fdroid.ui.utils.applyNewTheme
import org.fdroid.updates.AppUpdateWorker
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var settingsManager: SettingsManager

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
            // either sending via email intent works, or it doesn't, but don't keep trying
            retryPolicyClass = NoRetryPolicy::class.java
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
        applyNewTheme(settingsManager.theme)
        // bail out here if we are the ACRA process to not initialize anything in crash process
        if (isAcraProces()) return

        RepoUpdateWorker.scheduleOrCancel(applicationContext, settingsManager.repoUpdates)
        AppUpdateWorker.scheduleOrCancel(applicationContext, settingsManager.autoUpdateApps)
    }

    private fun isAcraProces(): Boolean {
        return if (SDK_INT >= 28) {
            val processName = getProcessName().split(':')
            processName.size > 1 && processName[1] == "acra"
        } else {
            // FIXME this does disk I/O
            ACRA.isACRASenderServiceProcess()
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                val downloadRequestKeyer = Keyer<DownloadRequest> { data, _ -> data.getCacheKey() }
                add(downloadRequestKeyer)
                add(downloadRequestFetcherFactory)

                val packageNameKeyer = Keyer<PackageName> { data, _ -> data.packageName }
                add(packageNameKeyer)
                add(LocalIconFetcher.Factory(context, downloadRequestFetcherFactory))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
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
