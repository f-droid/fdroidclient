package org.fdroid.install

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mu.KotlinLogging
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.download.DownloaderFactory
import org.fdroid.getCacheKey
import org.fdroid.utils.IoDispatcher
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInstallManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloaderFactory: DownloaderFactory,
    private val sessionInstallManager: SessionInstallManager,
    @param:IoDispatcher private val scope: CoroutineScope,
) {

    private val log = KotlinLogging.logger { }
    private val queue = ConcurrentLinkedQueue<AppVersion>()
    private val apps = ConcurrentHashMap<String, MutableStateFlow<InstallState>>()
    private val jobs = ConcurrentHashMap<String, Job>()

    fun getAppFlow(packageName: String): StateFlow<InstallState> {
        return apps.getOrPut(packageName) {
            MutableStateFlow(InstallState.Unknown)
        }
    }

    @UiThread
    suspend fun install(
        appMetadata: AppMetadata,
        version: AppVersion,
        repo: Repository,
        iconDownloadRequest: DownloadRequest?,
    ): InstallState? {
        val flow = apps.getOrPut(appMetadata.packageName) {
            MutableStateFlow(InstallState.Starting)
        }
        val job = scope.async {
            installInt(flow, appMetadata, version, repo, iconDownloadRequest)
        }
        // keep track of this job, in case we want to cancel it
        jobs.put(appMetadata.packageName, job)
        // wait for job to return
        val result = try {
            job.await()
        } catch (_: CancellationException) {
            InstallState.UserAborted
        } finally {
            // remove job as it has completed
            jobs.remove(appMetadata.packageName)
        }
        flow.update { result }
        return result
    }

    @WorkerThread
    private suspend fun installInt(
        flow: MutableStateFlow<InstallState>,
        appMetadata: AppMetadata,
        version: AppVersion,
        repo: Repository,
        iconDownloadRequest: DownloadRequest?,
    ): InstallState {
        flow.update { InstallState.Starting }
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
                flow.update { InstallState.PreApproved(preApprovalResult) }
                val sessionId = (preApprovalResult as? PreApprovalResult.Success)?.sessionId
                coroutineContext.ensureActive()
                // download file
                val file = File(context.cacheDir, version.file.sha256)
                val downloader =
                    downloaderFactory.create(repo, android.net.Uri.EMPTY, version.file, file)
                downloader.setListener { bytesRead, totalBytes ->
                    coroutineContext.ensureActive()
                    flow.update {
                        InstallState.Downloading(sessionId, bytesRead, totalBytes)
                    }
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
                flow.update { InstallState.Installing(sessionId) }
                val result = sessionInstallManager.install(sessionId, version.packageName, file)
                if (result is InstallState.PreApprovalFailed) {
                    // if pre-approval failed (e.g. due to app label mismatch),
                    // then try to install again, this time not using the pre-approved session
                    sessionInstallManager.install(null, version.packageName, file)
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
        val flow = apps[packageName] ?: error("No state for $packageName $installState")
        if (flow.value !is InstallState.UserConfirmationNeeded) {
            log.error { "Unexpected state: ${flow.value}" }
            return null
        }
        val result = sessionInstallManager.requestUserConfirmation(installState)
        flow.update { result }
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
        val flow = apps[packageName] ?: error("No state for $packageName $installState")
        if (flow.value !is InstallState.UserConfirmationNeeded) {
            log.debug { "State has changed. Now: ${flow.value}" }
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

    @UiThread
    fun cleanUp(packageName: String) {
        val flow = apps[packageName] ?: return
        if (!flow.value.showProgress) {
            log.info { "Cleaning up state for $packageName ${flow.value}" }
            jobs.remove(packageName)?.cancel()
            apps.remove(packageName)
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

}
