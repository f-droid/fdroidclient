package org.fdroid.fdroid.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.UiThread
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.NOTIFICATION_ID_REPO_UPDATE
import org.fdroid.fdroid.NotificationManager
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.Preferences.OVER_NETWORK_NEVER
import org.fdroid.fdroid.Preferences.UPDATE_INTERVAL_DISABLED
import org.fdroid.fdroid.R
import org.fdroid.fdroid.net.ConnectivityMonitorService.FLAG_NET_UNAVAILABLE
import org.fdroid.fdroid.net.ConnectivityMonitorService.getNetworkState
import java.util.concurrent.TimeUnit

private val TAG = AppUpdateWorker::class.java.simpleName

class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        // We are using two unique work names,
        // because [ExistingPeriodicWorkPolicy.UPDATE] doesn't update succeeded work
        // and we don't want to get into the business of canceling and re-enqueuing work.
        internal const val UNIQUE_WORK_NAME_APP_UPDATE = "appUpdate"
        private const val UNIQUE_WORK_NAME_AUTO_APP_UPDATE = "autoAppUpdate"

        /**
         * Use this to trigger a manual update of all apps without further constraints.
         */
        @UiThread
        @JvmStatic
        fun updateAppsNow(context: Context) {
            Log.i(TAG, "Update apps now!")
            if (FDroidApp.networkState > 0 && !Preferences.get().isOnDemandDownloadAllowed()) {
                Toast.makeText(context, R.string.updates_disabled_by_settings, LENGTH_LONG).show()
                Log.i(TAG, "  Updates disabled by data/wifi settings.")
                return
            } else if (getNetworkState(context) == FLAG_NET_UNAVAILABLE) {
                Toast.makeText(context, R.string.warning_no_internet, LENGTH_LONG).show()
                Log.i(TAG, "  No internet connection.")
                return
            }

            val request = OneTimeWorkRequestBuilder<AppUpdateWorker>()
                .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME_APP_UPDATE, ExistingWorkPolicy.REPLACE, request)
        }

        @JvmStatic
        fun scheduleOrCancel(context: Context) {
            val prefs = Preferences.get()
            val workManager = WorkManager.getInstance(context)
            val doAutoUpdates = prefs.isAutoDownloadEnabled &&
                prefs.updateInterval != UPDATE_INTERVAL_DISABLED &&
                !(prefs.overData == OVER_NETWORK_NEVER && prefs.overWifi == OVER_NETWORK_NEVER)
            if (doAutoUpdates) {
                Log.i(TAG, "scheduleOrCancel: enqueueUniquePeriodicWork")
                val networkType = if (prefs.overData == Preferences.OVER_NETWORK_ALWAYS &&
                    prefs.overWifi == Preferences.OVER_NETWORK_ALWAYS
                ) {
                    NetworkType.CONNECTED
                } else {
                    NetworkType.UNMETERED
                }
                // FIXME: inspection warning:
                //  Some devices are never considered charging and idle at the same time.
                //  Consider removing one of these constraints.
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)
                    .setRequiredNetworkType(networkType)
                    .build()
                val workRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(
                    repeatInterval = TimeUnit.HOURS.toMillis(24),
                    repeatIntervalTimeUnit = TimeUnit.MILLISECONDS,
                    flexTimeInterval = 60,
                    flexTimeIntervalUnit = TimeUnit.MINUTES,
                )
                    .setConstraints(constraints)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    uniqueWorkName = UNIQUE_WORK_NAME_AUTO_APP_UPDATE,
                    existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
                    request = workRequest,
                )
            } else {
                Log.w(TAG, "Not scheduling job due to settings!")
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME_AUTO_APP_UPDATE)
            }
        }
    }

    private val nm = NotificationManager(appContext)
    private val appUpdateManager = FDroidApp.getAppUpdateManager(appContext)

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.e(TAG, "Error while running setForeground", e)
        }
        return try {
            appUpdateManager.updateApps()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating repos", e)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val n = nm.getAppUpdateNotification().build()
        return if (SDK_INT >= 29) {
            ForegroundInfo(
                NOTIFICATION_ID_REPO_UPDATE,
                n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID_REPO_UPDATE, n)
        }
    }
}
