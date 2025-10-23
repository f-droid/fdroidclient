package org.fdroid.repo

import android.content.Context
import android.text.format.Formatter
import android.util.Log
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.fdroid.CompatibilityChecker
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.NotificationManager
import org.fdroid.R
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.download.DownloaderFactory
import org.fdroid.index.IndexUpdateListener
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.RepoManager
import org.fdroid.index.RepoUpdater
import org.fdroid.index.v1.IndexV1Updater
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val TAG = RepoUpdateManager::class.java.simpleName
private const val MIN_UPDATE_INTERVAL_MILLIS = 15_000

@Singleton
class RepoUpdateManager(
    private val context: Context,
    private val db: FDroidDatabase,
    private val repoManager: RepoManager,
    private val updatesManager: UpdatesManager,
    private val settingsManager: SettingsManager,
    private val downloaderFactory: DownloaderFactory,
    private val notificationManager: NotificationManager,
    private val compatibilityChecker: CompatibilityChecker = CompatibilityCheckerImpl(
        packageManager = context.packageManager,
        forceTouchApps = false,
    ),
    private val indexUpdateListener: IndexUpdateListener = object : IndexUpdateListener {
        override fun onDownloadProgress(repo: Repository, bytesRead: Long, totalBytes: Long) {
            Log.d(TAG, "Downloading ${repo.address} ($bytesRead/$totalBytes)")

            val percent = if (totalBytes > 0) {
                getPercent(bytesRead, totalBytes)
            } else {
                -1
            }
            val size = Formatter.formatFileSize(context, bytesRead)
            val message: String = if (totalBytes == -1L) {
                context.getString(R.string.status_download_unknown_size, repo.address, size)
            } else {
                val totalSize = Formatter.formatFileSize(context, totalBytes)
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

            if (totalApps > 0) notificationManager.showUpdateRepoNotification(
                msg = context.getString(
                    R.string.status_inserting_x_apps,
                    appsProcessed,
                    totalApps,
                    repo.address,
                ),
                progress = getPercent(appsProcessed.toLong(), totalApps.toLong())
            ) else notificationManager.showUpdateRepoNotification(
                msg = context.getString(R.string.status_inserting_apps),
            )
        }

        fun getPercent(current: Long, total: Long): Int {
            return (100L * current / total).toInt()
        }
    },
    private val repoUpdater: RepoUpdater = RepoUpdater(
        tempDir = context.cacheDir,
        db = db,
        downloaderFactory = downloaderFactory,
        compatibilityChecker = compatibilityChecker,
        listener = indexUpdateListener,
    ),
    private val indexV1Updater: IndexV1Updater? = if (false) {
        IndexV1Updater(
            database = db,
            tempFileProvider = { File.createTempFile("dl-", "", context.cacheDir) },
            downloaderFactory = downloaderFactory,
            compatibilityChecker = compatibilityChecker,
            listener = indexUpdateListener,
        )
    } else null,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        db: FDroidDatabase,
        repositoryManager: RepoManager,
        updatesManager: UpdatesManager,
        settingsManager: SettingsManager,
        downloaderFactory: DownloaderFactory,
        notificationManager: NotificationManager,
    ) : this(
        context = context,
        db = db,
        repoManager = repositoryManager,
        updatesManager = updatesManager,
        settingsManager = settingsManager,
        downloaderFactory = downloaderFactory,
        notificationManager = notificationManager,
    )

    private val log = KotlinLogging.logger { }
    private val isUpdateNotificationEnabled = true
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    /**
     * The time in milliseconds of the (earliest!) next automatic repo update check.
     * This is [Long.MAX_VALUE], if no time is known.
     */
    val nextUpdateFlow = RepoUpdateWorker.getAutoUpdateWorkInfo(context).map { workInfo ->
        workInfo?.nextScheduleTimeMillis ?: Long.MAX_VALUE
    }

    @WorkerThread
    suspend fun updateRepos() {
        if (isUpdating.value) log.warn { "Already updating repositories: updateRepos()" }
        val timeSinceLastCheck = System.currentTimeMillis() - settingsManager.lastRepoUpdate
        if (timeSinceLastCheck < MIN_UPDATE_INTERVAL_MILLIS) {
            log.info { "Not updating, only $timeSinceLastCheck ms since last check." }
            return
        }
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
                if (isUpdateNotificationEnabled) {
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
            db.getRepositoryDao().walCheckpoint()
            settingsManager.lastRepoUpdate = System.currentTimeMillis()
            if (repoErrors.isNotEmpty()) showRepoErrors(repoErrors)
            if (reposUpdated) {
                updatesManager.loadUpdates().join()
                val numUpdates = updatesManager.numUpdates.value
                if (numUpdates > 0) {
                    val states = updatesManager.notificationStates
                    notificationManager.showAppUpdatesAvailableNotification(states)
                }
            }
        } finally {
            notificationManager.cancelUpdateRepoNotification()
            _isUpdating.value = false
        }
    }

    @WorkerThread
    fun updateRepo(repoId: Long): IndexUpdateResult {
        if (isUpdating.value) log.warn { "Already updating repositories: updateRepo($repoId)" }

        val repo = repoManager.getRepository(repoId) ?: return IndexUpdateResult.NotFound
        _isUpdating.value = true
        return try {
            // show notification
            if (isUpdateNotificationEnabled) {
                val msg = context.getString(R.string.status_connecting_to_repo, repo.address)
                notificationManager.showUpdateRepoNotification(msg, throttle = false)
            }

            // indexV1Updater only gets used directly if forceIndexV1 was true
            val result = indexV1Updater?.update(repo) ?: repoUpdater.update(repo)
            if (result is IndexUpdateResult.Processed) {
                updatesManager.loadUpdates()
            }
            result
        } finally {
            notificationManager.cancelUpdateRepoNotification()
            _isUpdating.value = false
            db.getRepositoryDao().walCheckpoint()
        }
    }

    private fun showRepoErrors(repoErrors: List<Pair<Repository, Exception>>) {
        // TODO
    }

}
