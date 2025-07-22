package org.fdroid

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_SERVICE
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.content.ContextCompat.checkSelfPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import org.fdroid.next.R
import javax.inject.Inject

const val NOTIFICATION_ID_REPO_UPDATE: Int = 0
const val CHANNEL_UPDATES = "update-channel"

class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val nm = NotificationManagerCompat.from(context)
    private var lastRepoUpdateNotification = 0L

    init {
        val updateChannel = NotificationChannelCompat.Builder(
            CHANNEL_UPDATES, IMPORTANCE_LOW
        ).setName(context.getString(R.string.notification_channel_updates_title))
            .setDescription(context.getString(R.string.notification_channel_updates_description))
            .build()
        nm.createNotificationChannel(updateChannel)
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

    fun getAppUpdateNotification(
        msg: String? = null,
    ) = NotificationCompat.Builder(context, CHANNEL_UPDATES)
        .setSmallIcon(R.drawable.ic_refresh)
        .setCategory(CATEGORY_SERVICE)
        .setContentTitle(context.getString(R.string.banner_updating_apps))
        .setContentText(msg)
        .setOngoing(true)
        .setProgress(100, 0, true)

    fun showAppUpdatesAvailableNotification(numUpdates: Int) {
        val n = getAppUpdatesAvailableNotification(numUpdates).build()
        if (checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            nm.notify(NOTIFICATION_ID_REPO_UPDATE, n)
        }
    }

    private fun getAppUpdatesAvailableNotification(numUpdates: Int): NotificationCompat.Builder {
        val title = context.resources.getQuantityString(
            R.plurals.notification_summary_app_updates,
            numUpdates, numUpdates,
        )
        val text = context.getString(R.string.notification_title_summary_app_update_available)

        return NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(PRIORITY_HIGH)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(false)
            .setAutoCancel(true)
    }
}
