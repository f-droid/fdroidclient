package org.fdroid.repo

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.UiThread
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy.UPDATE
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.fdroid.NotificationManager
import org.fdroid.NotificationManager.Companion.NOTIFICATION_ID_REPO_UPDATE
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES

private val TAG = RepoUpdateWorker::class.java.simpleName

@HiltWorker
class RepoUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repoUpdateManager: RepoUpdateManager,
    private val nm: NotificationManager,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME_REPO_AUTO_UPDATE = "repoAutoUpdate"

        /**
         * Use this to trigger a manual repo update if the app is currently in the foreground.
         *
         * @param repoId The optional ID of the repo to update.
         * If no ID is given, all (enabled) repos will be updated.
         * Also triggers a clean cache job if no ID is given
         */
        @UiThread
        @JvmStatic
        @JvmOverloads
        fun updateNow(context: Context, repoId: Long = -1) {
            val request = OneTimeWorkRequestBuilder<RepoUpdateWorker>()
                .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .apply {
                    if (repoId >= 0) setInputData(workDataOf("repoId" to repoId))
                }
                .build()
            WorkManager.getInstance(context)
                .enqueue(request)
        }

        @JvmStatic
        fun scheduleOrCancel(context: Context) {
            val workManager = WorkManager.getInstance(context)
            Log.i(TAG, "scheduleOrCancel: enqueueUniquePeriodicWork")
            val networkType = NetworkType.UNMETERED
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(networkType)
                .build()
            val workRequest = PeriodicWorkRequestBuilder<RepoUpdateWorker>(
                repeatInterval = 4,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 15,
                flexTimeIntervalUnit = MINUTES,
            )
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME_REPO_AUTO_UPDATE,
                UPDATE,
                workRequest,
            )
        }

        fun getAutoUpdateWorkInfo(context: Context): Flow<WorkInfo?> {
            return WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(
                UNIQUE_WORK_NAME_REPO_AUTO_UPDATE
            ).map { it.getOrNull(0) }
        }
    }

    private val log = KotlinLogging.logger { }

    override suspend fun doWork(): Result {
        log.info { "Starting RepoUpdateWorker... $runAttemptCount" }
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            log.error(e) { "Error while running setForeground" }
        }
        val repoId = inputData.getLong("repoId", -1)
        return try {
            if (repoId >= 0) repoUpdateManager.updateRepo(repoId)
            else repoUpdateManager.updateRepos()
            Result.success()
        } catch (e: Exception) {
            log.error(e) { "Error updating repos" }
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val n = nm.getRepoUpdateNotification().build()
        return if (SDK_INT >= 29) {
            ForegroundInfo(NOTIFICATION_ID_REPO_UPDATE, n, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_REPO_UPDATE, n)
        }
    }
}
