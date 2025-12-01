package org.fdroid.updates

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import mu.KotlinLogging
import org.fdroid.NotificationManager
import org.fdroid.NotificationManager.Companion.NOTIFICATION_ID_APP_INSTALLS
import org.fdroid.NotificationManager.Companion.NOTIFICATION_ID_REPO_UPDATE
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstallNotificationState
import org.fdroid.ui.utils.canStartForegroundService
import java.util.concurrent.TimeUnit

@HiltWorker
class AppUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val nm: NotificationManager,
    private val updatesManager: UpdatesManager,
    private val appInstallManager: AppInstallManager,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private val TAG = AppUpdateWorker::class.simpleName

        @VisibleForTesting
        internal const val UNIQUE_WORK_NAME_APP_UPDATE = "autoAppUpdate"

        @JvmStatic
        fun scheduleOrCancel(context: Context, doAutoUpdates: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (doAutoUpdates) {
                Log.i(TAG, "scheduleOrCancel: enqueueUniquePeriodicWork")
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .setRequiresDeviceIdle(true)
                    .setRequiredNetworkType(NetworkType.UNMETERED)
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
                    uniqueWorkName = UNIQUE_WORK_NAME_APP_UPDATE,
                    existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
                    request = workRequest,
                )
            } else {
                Log.w(TAG, "Cancelling job due to settings!")
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME_APP_UPDATE)
            }
        }

        fun getAutoUpdateWorkInfo(context: Context): Flow<WorkInfo?> {
            return WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(
                UNIQUE_WORK_NAME_APP_UPDATE
            ).map { it.getOrNull(0) }
        }
    }

    private val log = KotlinLogging.logger { }

    override suspend fun doWork(): Result {
        log.info {
            if (SDK_INT >= 31) {
                "doWork $this stopReason: ${this.stopReason} runAttemptCount: $runAttemptCount"
            } else {
                "doWork $this runAttemptCount: $runAttemptCount"
            }
        }
        try {
            if (canStartForegroundService(applicationContext)) setForeground(getForegroundInfo())
        } catch (e: Exception) {
            log.error(e) { "Error while running setForeground: " }
        }
        return try {
            currentCoroutineContext().ensureActive()
            nm.cancelAppUpdatesAvailableNotification()
            // Updating apps will try start a foreground service
            // and it will "share" the same notification.
            // This is easier than trying to tell the [AppInstallManager]
            // not to start a foreground service in this specific case.
            updatesManager.updateAll().joinAll()
            // show success notification, if at least one app got installed
            val notificationState = appInstallManager.installNotificationState
            if (notificationState.numInstalled > 0) {
                nm.showInstallSuccessNotification(notificationState)
            }
            Result.success()
        } catch (e: Exception) {
            log.error(e) { "Error updating apps: " }
            if (runAttemptCount <= 3) {
                Result.retry()
            } else {
                log.warn { "Not retrying, already tried $runAttemptCount times." }
                Result.failure()
            }
        } finally {
            log.info {
                if (SDK_INT >= 31) "finished doWork $this (stopReason: ${this.stopReason})"
                else "finished doWork $this"
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val n = nm.getAppInstallNotification(InstallNotificationState()).build()
        return if (SDK_INT >= 29) {
            ForegroundInfo(
                NOTIFICATION_ID_APP_INSTALLS,
                n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID_REPO_UPDATE, n)
        }
    }
}
