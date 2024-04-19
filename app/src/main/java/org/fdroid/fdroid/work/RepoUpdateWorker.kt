package org.fdroid.fdroid.work

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.UiThread
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.NOTIFICATION_ID_REPO_UPDATE
import org.fdroid.fdroid.NotificationManager
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.Preferences.OVER_NETWORK_ALWAYS
import org.fdroid.fdroid.Preferences.OVER_NETWORK_NEVER
import org.fdroid.fdroid.Preferences.UPDATE_INTERVAL_DISABLED
import org.fdroid.fdroid.R
import org.fdroid.fdroid.net.ConnectivityMonitorService.FLAG_NET_UNAVAILABLE
import org.fdroid.fdroid.net.ConnectivityMonitorService.getNetworkState
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES

private val TAG = RepoUpdateWorker::class.java.simpleName

class RepoUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME_AUTO_UPDATE = "autoUpdate"

        /**
         * Use this to trigger a manual repo update if the app is currently in the foreground.
         *
         * @param repoId The optional ID of the repo to update.
         * If no ID is given, all (enabled) repos will be updated.
         */
        @UiThread
        @JvmStatic
        @JvmOverloads
        fun updateNow(context: Context, repoId: Long = -1) {
            if (FDroidApp.networkState > 0 && !Preferences.get().isOnDemandDownloadAllowed()) {
                Toast.makeText(context, R.string.updates_disabled_by_settings, LENGTH_LONG).show()
                return
            } else if (getNetworkState(context) == FLAG_NET_UNAVAILABLE) {
                Toast.makeText(context, R.string.warning_no_internet, LENGTH_LONG).show()
                return
            }

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
            val prefs = Preferences.get()
            val workManager = WorkManager.getInstance(context)
            val doUpdateChecks = prefs.updateInterval != UPDATE_INTERVAL_DISABLED
                && !(prefs.overData == OVER_NETWORK_NEVER && prefs.overWifi == OVER_NETWORK_NEVER)
            if (doUpdateChecks) {
                val networkType = if (prefs.overData == OVER_NETWORK_ALWAYS
                    && prefs.overWifi == OVER_NETWORK_ALWAYS
                ) {
                    NetworkType.CONNECTED
                } else {
                    NetworkType.UNMETERED
                }
                val constraints = Constraints.Builder()
                    .setRequiresDeviceIdle(true)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .setRequiredNetworkType(networkType)
                    .build()
                val workRequest = PeriodicWorkRequestBuilder<RepoUpdateWorker>(
                    repeatInterval = prefs.updateInterval,
                    repeatIntervalTimeUnit = MILLISECONDS,
                    flexTimeInterval = 15,
                    flexTimeIntervalUnit = MINUTES,
                )
                    .setConstraints(constraints)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME_AUTO_UPDATE, UPDATE, workRequest
                )
            } else {
                Log.w(TAG, "Not scheduling job due to settings!")
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME_AUTO_UPDATE)
            }
        }

        fun getAutoUpdateWorkInfo(context: Context): Flow<WorkInfo?> {
            return WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(
                UNIQUE_WORK_NAME_AUTO_UPDATE
            ).map { it.getOrNull(0) }
        }
    }

    private val nm = NotificationManager(appContext)
    private val repoUpdateManager = FDroidApp.getRepoUpdateManager(appContext)

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.e(TAG, "Error while running setForeground", e)
        }
        val repoId = inputData.getLong("repoId", -1)
        return try {
            if (repoId >= 0) repoUpdateManager.updateRepo(repoId)
            else repoUpdateManager.updateRepos()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating repos", e)
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
