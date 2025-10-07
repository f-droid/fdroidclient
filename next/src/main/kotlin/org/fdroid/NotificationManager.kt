package org.fdroid

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.app.NotificationCompat.CATEGORY_SERVICE
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.content.ContextCompat.checkSelfPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import org.fdroid.install.InstallNotificationState
import org.fdroid.next.R
import javax.inject.Inject

class NotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val nm = NotificationManagerCompat.from(context)
    private var lastRepoUpdateNotification = 0L

    companion object {
        const val NOTIFICATION_ID_REPO_UPDATE: Int = 0
        const val NOTIFICATION_ID_APP_INSTALLS: Int = 1
        const val NOTIFICATION_ID_APP_INSTALL_SUCCESS: Int = 2
        private const val CHANNEL_UPDATES = "update-channel"
        private const val CHANNEL_INSTALLS = "install-channel"
        private const val CHANNEL_INSTALL_SUCCESS = "install-success-channel"
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

    // TODO pass in bigText with apps and their version changes
    fun showAppUpdatesAvailableNotification(numUpdates: Int) {
        val n = getAppUpdatesAvailableNotification(numUpdates).build()
        if (checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            // TODO different ID
            nm.notify(NOTIFICATION_ID_REPO_UPDATE, n)
        }
    }

    private fun getAppUpdatesAvailableNotification(numUpdates: Int): NotificationCompat.Builder {
        val title = context.resources.getQuantityString(
            R.plurals.notification_summary_app_updates,
            numUpdates, numUpdates,
        )
        val text = context.getString(R.string.notification_title_summary_app_update_available)
        // TODO different channel
        return NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(PRIORITY_HIGH)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(false)
            .setAutoCancel(true)
    }

    fun showAppInstallNotification(installNotificationState: InstallNotificationState) {
        // TODO we may need some throttling when many apps download at the same time
        val n = getAppInstallNotification(installNotificationState).build()
        if (checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            nm.notify(NOTIFICATION_ID_APP_INSTALLS, n)
        }
    }

    fun getAppInstallNotification(state: InstallNotificationState): NotificationCompat.Builder {
        val pi = state.getPendingIntent(context)
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
        val pi = state.getPendingIntent(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_INSTALL_SUCCESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(CATEGORY_SERVICE)
            .setContentTitle(state.getSuccessTitle(context))
            .setStyle(BigTextStyle().bigText(state.getSuccessBigText()))
            .setContentIntent(pi)
            .setAutoCancel(true)
        return builder
    }

    private fun s(@StringRes id: Int): String {
        return context.getString(id)
    }
}
