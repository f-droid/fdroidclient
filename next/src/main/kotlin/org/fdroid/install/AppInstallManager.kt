package org.fdroid.install

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_DELETE
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.LocaleListCompat
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import mu.KotlinLogging
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.NotificationManager
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.download.DownloaderFactory
import org.fdroid.getCacheKey
import org.fdroid.utils.IoDispatcher
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInstallManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloaderFactory: DownloaderFactory,
    private val sessionInstallManager: SessionInstallManager,
    private val notificationManager: NotificationManager,
    @param:IoDispatcher private val scope: CoroutineScope,
) {

    private val log = KotlinLogging.logger { }
    private val apps = MutableStateFlow<Map<String, InstallState>>(emptyMap())
    private val jobs = ConcurrentHashMap<String, Job>()
    val appInstallStates = apps.asStateFlow()

    fun getAppFlow(packageName: String): Flow<InstallState> {
        return apps.map { it[packageName] ?: InstallState.Unknown }
    }

    @UiThread
    suspend fun install(
        appMetadata: AppMetadata,
        version: AppVersion,
        currentVersionName: String?,
        repo: Repository,
        iconDownloadRequest: DownloadRequest?,
    ): InstallState {
        val packageName = appMetadata.packageName
        val job = scope.async {
            installInt(appMetadata, version, currentVersionName, repo, iconDownloadRequest)
        }
        // keep track of this job, in case we want to cancel it
        jobs.put(packageName, job)
        // wait for job to return
        val result = try {
            job.await()
        } catch (_: CancellationException) {
            InstallState.UserAborted
        } finally {
            // remove job as it has completed
            jobs.remove(packageName)
        }
        apps.updateApp(packageName) { result }
        onStatesUpdated()
        return result
    }

    @WorkerThread
    private suspend fun installInt(
        appMetadata: AppMetadata,
        version: AppVersion,
        currentVersionName: String?,
        repo: Repository,
        iconDownloadRequest: DownloadRequest?,
    ): InstallState {
        apps.updateApp(appMetadata.packageName) {
            InstallState.Starting(
                name = appMetadata.name.getBestLocale(LocaleListCompat.getDefault()) ?: "Unknown",
                versionName = version.versionName,
                currentVersionName = currentVersionName,
                lastUpdated = version.added,
                iconDownloadRequest = iconDownloadRequest,
            )
        }
        onStatesUpdated()
        val coroutineContext = currentCoroutineContext()
        // get the icon for pre-approval (usually in memory cache, so should be quick)
        coroutineContext.ensureActive()
        val icon = getIcon(iconDownloadRequest)
        // request pre-approval from user (if available)
        coroutineContext.ensureActive()
        val preApprovalResult = sessionInstallManager.requestPreapproval(appMetadata, icon)
        // continue depending on result, abort early if no approval was given
        return when (preApprovalResult) {
            is PreApprovalResult.Error -> InstallState.Error(preApprovalResult.errorMsg)
            is PreApprovalResult.UserAborted -> InstallState.UserAborted
            else -> {
                apps.checkAndUpdateApp(appMetadata.packageName) {
                    InstallState.PreApproved(
                        name = it.name,
                        versionName = it.versionName,
                        currentVersionName = it.currentVersionName,
                        lastUpdated = it.lastUpdated,
                        iconDownloadRequest = it.iconDownloadRequest,
                        result = preApprovalResult,
                    )
                }
                val sessionId = (preApprovalResult as? PreApprovalResult.Success)?.sessionId
                coroutineContext.ensureActive()
                // download file
                val file = File(context.cacheDir, version.file.sha256)
                val downloader =
                    downloaderFactory.create(repo, Uri.EMPTY, version.file, file)
                val now = System.currentTimeMillis()
                downloader.setListener { bytesRead, totalBytes ->
                    coroutineContext.ensureActive()
                    apps.checkAndUpdateApp(appMetadata.packageName) {
                        InstallState.Downloading(
                            name = it.name,
                            versionName = it.versionName,
                            currentVersionName = it.currentVersionName,
                            lastUpdated = it.lastUpdated,
                            iconDownloadRequest = it.iconDownloadRequest,
                            downloadedBytes = bytesRead,
                            totalBytes = totalBytes,
                            startMillis = now,
                        )
                    }
                    onStatesUpdated()
                }
                try {
                    downloader.download()
                    log.debug { "Download completed" }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    log.error(e) { "Error downloading ${version.file}" }
                    val msg = "Download failed: ${e::class.java.simpleName} ${e.message}"
                    return InstallState.Error(msg)
                }
                coroutineContext.ensureActive()
                val newState = apps.checkAndUpdateApp(appMetadata.packageName) {
                    InstallState.Installing(
                        name = it.name,
                        versionName = it.versionName,
                        currentVersionName = it.currentVersionName,
                        lastUpdated = it.lastUpdated,
                        iconDownloadRequest = it.iconDownloadRequest,
                    )
                }
                val result =
                    sessionInstallManager.install(sessionId, version.packageName, newState, file)
                if (result is InstallState.PreApproved &&
                    result.result is PreApprovalResult.Error
                ) {
                    // if pre-approval failed (e.g. due to app label mismatch),
                    // then try to install again, this time not using the pre-approved session
                    sessionInstallManager.install(null, version.packageName, newState, file)
                } else {
                    result
                }
            }
        }
    }

    /**
     * Request user confirmation for installation and suspend until we get a result.
     */
    @UiThread
    suspend fun requestUserConfirmation(
        packageName: String,
        installState: InstallState.UserConfirmationNeeded,
    ): InstallState? {
        val state = apps.value[packageName] ?: error("No state for $packageName $installState")
        if (state !is InstallState.UserConfirmationNeeded) {
            log.error { "Unexpected state: $state" }
            return null
        }
        log.info { "Requesting user confirmation for $packageName" }
        val result = sessionInstallManager.requestUserConfirmation(installState)
        log.info { "User confirmation for $packageName $result" }
        apps.updateApp(packageName) { result }
        onStatesUpdated()
        return result
    }

    /**
     * A workaround for Android 10, 11, 12 and 13 where tapping outside the confirmation dialog
     * dismisses it without any feedback for us.
     * So when our activity resumes while we are in state [InstallState.UserConfirmationNeeded]
     * we need to call this method, so we can manually check if our session progressed or not.
     * If it didn't progress and the state hasn't changed, we fire up the confirmation intent again.
     */
    @UiThread
    fun checkUserConfirmation(
        packageName: String,
        installState: InstallState.UserConfirmationNeeded,
    ) {
        val state = apps.value[packageName] ?: error("No state for $packageName $installState")
        if (state !is InstallState.UserConfirmationNeeded) {
            log.debug { "State has changed. Now: $state" }
            return
        }
        val sessionInfo =
            context.packageManager.packageInstaller.getSessionInfo(installState.sessionId)
                ?: run {
                    log.error { "Session ${installState.sessionId} does not exist anymore" }
                    return
                }
        if (sessionInfo.progress <= installState.progress) {
            log.info {
                "Session did not progress: ${sessionInfo.progress} <= ${installState.progress}"
            }
            // we fire up intent again to force the user to do a proper yes/no decision,
            // so our session and our coroutine above don't get stuck
            installState.intent.send()
        } else {
            log.debug { "Session has progressed, doing nothing" }
        }
    }

    fun cancel(packageName: String) {
        val job = jobs[packageName]
        log.debug { "Canceling job for $packageName $job" }
        job?.cancel()
    }

    /**
     * Must be called after receiving the result from the [ACTION_DELETE] uninstall Intent.
     *
     * Note: We are not using [android.content.pm.PackageInstaller.uninstall],
     * because on Android 10 to 13 (at least) we don't get feedback
     * when the user taps outside the confirmation dialog.
     * Using this non-deprecated API ([ACTION_DELETE]) seems to work
     * without issues everywhere.
     */
    @UiThread
    fun onUninstallResult(packageName: String, activityResult: ActivityResult): InstallState {
        val result = when (activityResult.resultCode) {
            Activity.RESULT_OK -> InstallState.Uninstalled
            Activity.RESULT_FIRST_USER -> InstallState.UserAborted
            else -> InstallState.UserAborted
        }
        val code = activityResult.data?.getIntExtra("android.intent.extra.INSTALL_RESULT", -1)
        log.info { "Uninstall result received: ${activityResult.resultCode} => $result ($code)" }
        apps.updateApp(packageName) { result }
        return result
    }

    @UiThread
    fun cleanUp(packageName: String) {
        val state = apps.value[packageName] ?: return
        if (!state.showProgress) {
            log.info { "Cleaning up state for $packageName $state" }
            jobs.remove(packageName)?.cancel()
            apps.update { oldApps ->
                oldApps.toMutableMap().apply {
                    remove(packageName)
                }
            }
        }
    }

    private fun onStatesUpdated() {
        val appStates = mutableListOf<AppState>()
        var numBytesDownloaded = 0L
        var numTotalBytes = 0L
        // go throw all apps that have active state
        apps.value.toMap().forEach { packageName, state ->
            // assign a category to each in progress state
            val appStateCategory = when (state) {
                is InstallState.Installing, is InstallState.PreApproved,
                is InstallState.Starting -> AppStateCategory.INSTALLING
                is InstallState.Downloading -> {
                    numBytesDownloaded += state.downloadedBytes
                    numTotalBytes += state.totalBytes
                    AppStateCategory.INSTALLING
                }
                is InstallState.Installed -> AppStateCategory.INSTALLED
                is InstallState.UserConfirmationNeeded -> AppStateCategory.NEEDS_CONFIRMATION
                else -> null
            }
            // track app state for in progress apps
            val appState = appStateCategory?.let {
                // all states that get a category above must be InstallStateWithInfo
                state as InstallStateWithInfo
                AppState(
                    packageName = packageName,
                    category = it,
                    name = state.name,
                    installVersionName = state.versionName,
                    currentVersionName = state.currentVersionName,
                )
            }
            if (appState != null) appStates.add(appState)
        }
        val notificationState = InstallNotificationState(
            apps = appStates,
            numBytesDownloaded = numBytesDownloaded,
            numTotalBytes = numTotalBytes,
        )
        val serviceIntent = Intent(context, AppInstallService::class.java)
        // stop foreground service, if no app is installing and it is still running
        if (!notificationState.isInstallingSomeApp && AppInstallService.isServiceRunning) {
            context.stopService(serviceIntent)
        }
        if (notificationState.isInProgress) {
            // start foreground service if at least one app is installing and not already running
            if (notificationState.isInstallingSomeApp && !AppInstallService.isServiceRunning) {
                context.startService(serviceIntent)
            }
            notificationManager.showAppInstallNotification(notificationState)
        } else {
            // cancel notification if no more apps are in progress
            notificationManager.cancelAppInstallNotification()
        }
    }

    /**
     * Gets icon for preapproval from memory cache.
     * In the unlikely event, that the icon isn't in the cache,
     * we we download it with the given [iconDownloadRequest].
     */
    private suspend fun getIcon(iconDownloadRequest: DownloadRequest?): Bitmap? {
        return iconDownloadRequest?.let { downloadRequest ->
            // try memory cache first and download, if not found
            val memoryCache = SingletonImageLoader.get(context).memoryCache
            val key = downloadRequest.getCacheKey()
            memoryCache?.get(MemoryCache.Key(key))?.image?.toBitmap() ?: run {
                // not found in cache, download icon
                val request = ImageRequest.Builder(context)
                    .data(downloadRequest)
                    .size(Size.ORIGINAL)
                    .build()
                SingletonImageLoader.get(context).execute(request).image?.toBitmap()
            }
        }
    }

    private fun MutableStateFlow<Map<String, InstallState>>.updateApp(
        packageName: String,
        function: (InstallState) -> InstallState,
    ) = update { oldMap ->
        val newMap = oldMap.toMutableMap()
        newMap[packageName] = function(newMap[packageName] ?: InstallState.Unknown)
        newMap
    }

    private fun MutableStateFlow<Map<String, InstallState>>.checkAndUpdateApp(
        packageName: String,
        function: (InstallStateWithInfo) -> InstallStateWithInfo,
    ): InstallStateWithInfo {
        return updateAndGet { oldMap ->
            val oldState = oldMap[packageName]
            check(oldState is InstallStateWithInfo) {
                "State for $packageName was $oldState"
            }
            val newMap = oldMap.toMutableMap()
            newMap[packageName] = function(oldState)
            newMap
        }[packageName] as InstallStateWithInfo
    }

}
