package org.fdroid.fdroid

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_SERVICE
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.content.ContextCompat.getSystemService
import org.fdroid.fdroid.NotificationHelper.CHANNEL_UPDATES
import org.fdroid.fdroid.views.main.MainActivity

const val NOTIFICATION_ID_REPO_UPDATE: Int = 0

class NotificationManager(
    private val context: Context,
) {

    private val nm = getSystemService(context, NotificationManager::class.java)
        ?: error("No NotificationManager")
    private var lastRepoUpdateNotification = 0L

    fun showUpdateRepoNotification(msg: String, throttle: Boolean = true, progress: Int? = null) {
        if (!throttle || System.currentTimeMillis() - lastRepoUpdateNotification > 500) {
            val n = getRepoUpdateNotification(msg, progress).build()
            lastRepoUpdateNotification = System.currentTimeMillis()
            nm.notify(NOTIFICATION_ID_REPO_UPDATE, n)
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
        nm.notify(NOTIFICATION_ID_REPO_UPDATE, n)
    }

    private fun getAppUpdatesAvailableNotification(numUpdates: Int): NotificationCompat.Builder {
        val title = context.resources.getQuantityString(
            R.plurals.notification_summary_app_updates,
            numUpdates, numUpdates,
        )
        val text = context.getString(R.string.notification_title_summary_app_update_available)
        val i = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_VIEW_UPDATES, true)
            putExtra(MainActivity.EXTRA_DO_UPDATES, true)
        }
        val pi = PendingIntent.getActivity(context, 42, i, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(PRIORITY_HIGH)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(false)
            .setContentIntent(pi)
            .setAutoCancel(true)
    }
}
