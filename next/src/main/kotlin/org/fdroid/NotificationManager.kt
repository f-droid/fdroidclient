package org.fdroid

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.app.NotificationCompat.CATEGORY_SERVICE
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.content.ContextCompat.checkSelfPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import mu.KotlinLogging
import org.fdroid.install.InstallNotificationState
import org.fdroid.ui.IntentRouter.Companion.ACTION_MY_APPS
import org.fdroid.updates.UpdateNotificationState
import javax.inject.Inject

class NotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val log = KotlinLogging.logger {}
    private val nm = NotificationManagerCompat.from(context)
    private var lastRepoUpdateNotification = 0L

    companion object {
        const val NOTIFICATION_ID_REPO_UPDATE: Int = 0
        const val NOTIFICATION_ID_APP_INSTALLS: Int = 1
        const val NOTIFICATION_ID_APP_INSTALL_SUCCESS: Int = 2
        const val NOTIFICATION_ID_APP_UPDATES_AVAILABLE: Int = 3
        private const val CHANNEL_UPDATES = "update-channel"
        private const val CHANNEL_INSTALLS = "install-channel"
        private const val CHANNEL_INSTALL_SUCCESS = "install-success-channel"
        private const val CHANNEL_UPDATES_AVAILABLE = "updates-available-channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannelCompat.Builder(CHANNEL_UPDATES, IMPORTANCE_LOW)
                .setName(s(R.string.notification_channel_updates_title))
                .setDescription(s(R.string.notification_channel_updates_description))
                .build(),
            NotificationChannelCompat.Builder(CHANNEL_INSTALLS, IMPORTANCE_LOW)
                .setName(s(R.string.notification_channel_installs_title))
                .setDescription(s(R.string.notification_channel_installs_description))
                .build(),
            NotificationChannelCompat.Builder(CHANNEL_INSTALL_SUCCESS, IMPORTANCE_LOW)
                .setName(s(R.string.notification_channel_install_success_title))
                .setDescription(s(R.string.notification_channel_install_success_description))
                .build(),
            NotificationChannelCompat.Builder(CHANNEL_UPDATES_AVAILABLE, IMPORTANCE_DEFAULT)
                .setName(s(R.string.notification_channel_updates_available_title))
                .setDescription(s(R.string.notification_channel_updates_available_description))
                .build(),
        )
        nm.createNotificationChannelsCompat(channels)
    }

    fun showUpdateRepoNotification(msg: String, throttle: Boolean = true, progress: Int? = null) {
        if (!throttle || System.currentTimeMillis() - lastRepoUpdateNotification > 500) {
            val n = getRepoUpdateNotification(msg, progress).build()
            lastRepoUpdateNotification = System.currentTimeMillis()
            if (checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
                nm.notify(NOTIFICATION_ID_REPO_UPDATE, n)
            }
        }
    }

    fun cancelUpdateRepoNotification() {
        nm.cancel(NOTIFICATION_ID_REPO_UPDATE)
    }

    fun getRepoUpdateNotification(
        msg: String? = null,
        progress: Int? = null,
    ) = NotificationCompat.Builder(context, CHANNEL_UPDATES)
        .setSmallIcon(R.drawable.ic_refresh)
        .setCategory(CATEGORY_SERVICE)
        .setContentTitle(context.getString(R.string.banner_updating_repositories))
        .setContentText(msg)
        .setOngoing(true)
        .setProgress(100, progress ?: 0, progress == null)

    fun showAppUpdatesAvailableNotification(notificationState: UpdateNotificationState) {
        val n = getAppUpdatesAvailableNotification(notificationState).build()
        if (checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            nm.notify(NOTIFICATION_ID_APP_UPDATES_AVAILABLE, n)
        }
    }

    private fun getAppUpdatesAvailableNotification(
        state: UpdateNotificationState,
    ): NotificationCompat.Builder {
        val pi = getMyAppsPendingIntent(context)
        return NotificationCompat.Builder(context, CHANNEL_UPDATES_AVAILABLE)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(PRIORITY_HIGH)
            .setContentTitle(state.getTitle(context))
            .setContentIntent(pi)
            .setStyle(BigTextStyle().bigText(state.getBigText()))
            .setOngoing(false)
            .setAutoCancel(true)
    }

    fun cancelAppUpdatesAvailableNotification() {
        log.info { "cancel app updates available notification" }
        nm.cancel(NOTIFICATION_ID_APP_UPDATES_AVAILABLE)
    }

    fun showAppInstallNotification(installNotificationState: InstallNotificationState) {
        // TODO we may need some throttling when many apps download at the same time
        val n = getAppInstallNotification(installNotificationState).build()
        if (checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            nm.notify(NOTIFICATION_ID_APP_INSTALLS, n)
        }
    }

    fun getAppInstallNotification(state: InstallNotificationState): NotificationCompat.Builder {
        val pi = getMyAppsPendingIntent(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_INSTALLS)
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(CATEGORY_SERVICE)
            .setContentTitle(state.getTitle(context))
            .setStyle(BigTextStyle().bigText(state.getBigText(context)))
            .setContentIntent(pi)
            .setOngoing(state.isInstallingSomeApp)
            .apply {
                if (state.isInstallingSomeApp) {
                    setProgress(100, state.percent ?: 0, state.percent == null)
                }
            }
        return builder
    }

    fun cancelAppInstallNotification() {
        nm.cancel(NOTIFICATION_ID_APP_INSTALLS)
    }

    fun showInstallSuccessNotification(installNotificationState: InstallNotificationState) {
        val n = getInstallSuccessNotification(installNotificationState).build()
        if (checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            nm.notify(NOTIFICATION_ID_APP_INSTALL_SUCCESS, n)
        }
    }

    fun getInstallSuccessNotification(state: InstallNotificationState): NotificationCompat.Builder {
        val pi = getMyAppsPendingIntent(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_INSTALL_SUCCESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(CATEGORY_SERVICE)
            .setContentTitle(state.getSuccessTitle(context))
            .setStyle(BigTextStyle().bigText(state.getSuccessBigText()))
            .setContentIntent(pi)
            .setAutoCancel(true)
        return builder
    }

    private fun getMyAppsPendingIntent(context: Context): PendingIntent {
        val i = Intent(ACTION_MY_APPS).apply {
            setClass(context, MainActivity::class.java)
        }
        val flags = FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, i, flags)
    }

    private fun s(@StringRes id: Int): String {
        return context.getString(id)
    }
}
