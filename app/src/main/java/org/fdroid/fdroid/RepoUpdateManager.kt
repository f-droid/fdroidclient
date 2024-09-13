package org.fdroid.fdroid

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.WorkerThread
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.fdroid.data.App
import org.fdroid.fdroid.data.DBHelper
import org.fdroid.fdroid.net.DownloaderFactory
import org.fdroid.fdroid.work.RepoUpdateWorker
import org.fdroid.index.IndexUpdateListener
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.RepoManager
import org.fdroid.index.RepoUpdater
import org.fdroid.index.RepoUriBuilder
import org.fdroid.index.v1.IndexV1Updater
import java.io.File

private val TAG = RepoUpdateManager::class.java.simpleName

class RepoUpdateManager @JvmOverloads constructor(
    private val context: Context,
    private val db: FDroidDatabase,
    private val repoManager: RepoManager = FDroidApp.getRepoManager(context),
    private val notificationManager: NotificationManager = NotificationManager(context),
    forceIndexV1: Boolean = Preferences.get().isForceOldIndexEnabled,
) : IndexUpdateListener {

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()
    val isUpdatingLiveData = _isUpdating.asLiveData()

    /**
     * The time in milliseconds of the (earliest!) next automatic repo update check.
     * This is [Long.MAX_VALUE], if no time is known.
     */
    val nextUpdateFlow = RepoUpdateWorker.getAutoUpdateWorkInfo(context).map { workInfo ->
        workInfo?.nextScheduleTimeMillis ?: Long.MAX_VALUE
    }
    val nextUpdateLiveData = nextUpdateFlow.asLiveData()

    private val uriBuilder = RepoUriBuilder { repository, pathElements ->
        val repoAddress = Utils.getRepoAddress(repository)
        Utils.getUri(repoAddress, *pathElements)
    }
    private val compatibilityChecker = CompatibilityCheckerImpl(
        packageManager = context.packageManager,
        forceTouchApps = Preferences.get().forceTouchApps(),
    )
    private val repoUpdater: RepoUpdater = RepoUpdater(
        tempDir = context.cacheDir,
        db = DBHelper.getDb(context),
        downloaderFactory = DownloaderFactory.INSTANCE,
        repoUriBuilder = uriBuilder,
        compatibilityChecker = compatibilityChecker,
        listener = this@RepoUpdateManager,
    )
    private val indexV1Updater: IndexV1Updater? = if (forceIndexV1) IndexV1Updater(
        database = db,
        tempFileProvider = { File.createTempFile("dl-", "", context.cacheDir) },
        downloaderFactory = DownloaderFactory.INSTANCE,
        repoUriBuilder = uriBuilder,
        compatibilityChecker = compatibilityChecker,
        listener = this,
    ) else null
    private val fdroidPrefs = Preferences.get()

    @WorkerThread
    fun updateRepos() {
        _isUpdating.value = true
        try {
            var reposUpdated = false
            val repoErrors = mutableListOf<Pair<Repository, Exception>>()
            // always get repos fresh from DB, because
            // * when an update is requested early at app start,
            //   the repos above might not be available, yet
            // * when an update is requested when adding a new repo,
            //   it might not be in the FDroidApp list, yet
            db.getRepositoryDao().getRepositories().forEach { repo ->
                if (!repo.enabled) return@forEach

                // show notification
                if (fdroidPrefs.isUpdateNotificationEnabled) {
                    val msg = context.getString(R.string.status_connecting_to_repo, repo.address)
                    notificationManager.showUpdateRepoNotification(msg, throttle = false)
                }

                // indexV1Updater only gets used directly if forceIndexV1 was true
                val result = indexV1Updater?.update(repo) ?: repoUpdater.update(repo)

                if (result is IndexUpdateResult.Processed) reposUpdated = true
                else if (result is IndexUpdateResult.Error) {
                    Log.e(TAG, "Error updating repository ${repo.address}", result.e)
                    repoErrors.add(Pair(repo, result.e))
                }
            }
            fdroidPrefs.lastUpdateCheck = System.currentTimeMillis()
            if (repoErrors.isNotEmpty()) showRepoErrors(repoErrors)
            if (reposUpdated) {
                val appUpdateStatusManager = AppUpdateStatusManager.getInstance(context)
                if (fdroidPrefs.isAutoDownloadEnabled && fdroidPrefs.isBackgroundDownloadAllowed) {
                    appUpdateStatusManager.checkForUpdatesAndInstall()
                } else {
                    appUpdateStatusManager.checkForUpdates()
                }
            }
        } finally {
            notificationManager.cancelUpdateRepoNotification()
            _isUpdating.value = false
        }
    }

    @WorkerThread
    fun updateRepo(repoId: Long): IndexUpdateResult {
        val repo = repoManager.getRepository(repoId) ?: return IndexUpdateResult.NotFound
        _isUpdating.value = true
        try {
            // show notification
            if (fdroidPrefs.isUpdateNotificationEnabled) {
                val msg = context.getString(R.string.status_connecting_to_repo, repo.address)
                notificationManager.showUpdateRepoNotification(msg, throttle = false)
            }

            // indexV1Updater only gets used directly if forceIndexV1 was true
            return indexV1Updater?.update(repo) ?: repoUpdater.update(repo)
        } finally {
            notificationManager.cancelUpdateRepoNotification()
            _isUpdating.value = false
        }
    }

    private fun showRepoErrors(repoErrors: List<Pair<Repository, Exception>>) {
        val msgBuilder = StringBuilder()
        for ((repo, e) in repoErrors) {
            if (msgBuilder.isNotEmpty()) msgBuilder.append('\n')
            val cause = e.cause
            val repoName = repo.getName(App.getLocales()) ?: repo.address
            if (cause == null) {
                msgBuilder.append("$repoName: ${e.localizedMessage}")
            } else {
                msgBuilder.append("$repoName: ${e.localizedMessage} ${cause.localizedMessage}")
            }
        }
        // can't show Toast from background thread, so we need to move this to UiThread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msgBuilder.toString(), LENGTH_LONG).show()
        }
    }

    override fun onDownloadProgress(repo: Repository, bytesRead: Long, totalBytes: Long) {
        Log.d(TAG, "Downloading ${repo.address} ($bytesRead/$totalBytes)")
        if (!fdroidPrefs.isUpdateNotificationEnabled) return

        val percent = if (totalBytes > 0) {
            Utils.getPercent(bytesRead, totalBytes)
        } else {
            -1
        }
        val size = Utils.getFriendlySize(bytesRead)
        val message: String = if (totalBytes == -1L) {
            context.getString(R.string.status_download_unknown_size, repo.address, size)
        } else {
            val totalSize = Utils.getFriendlySize(totalBytes)
            context.getString(R.string.status_download, repo.address, size, totalSize, percent)
        }
        notificationManager.showUpdateRepoNotification(msg = message, progress = percent)
    }

    /**
     * If an updater is unable to know how many apps it has to process (i.e. it
     * is streaming apps to the database or performing a large database query
     * which touches all apps, but is unable to report progress), then it call
     * this listener with [totalApps] = 0. Doing so will result in a message of
     * "Saving app details" sent to the user. If you know how many apps you have
     * processed, then a message of "Saving app details (x/total)" is displayed.
     */
    override fun onUpdateProgress(repo: Repository, appsProcessed: Int, totalApps: Int) {
        Log.d(TAG, "Committing ${repo.address} ($appsProcessed/$totalApps)")
        if (!fdroidPrefs.isUpdateNotificationEnabled) return

        if (totalApps > 0) notificationManager.showUpdateRepoNotification(
            msg = context.getString(
                R.string.status_inserting_x_apps,
                appsProcessed,
                totalApps,
                repo.address,
            ),
            progress = Utils.getPercent(appsProcessed.toLong(), totalApps.toLong())
        ) else notificationManager.showUpdateRepoNotification(
            msg = context.getString(R.string.status_inserting_apps),
        )
    }
}
