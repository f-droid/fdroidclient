package org.fdroid.fdroid

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import org.fdroid.fdroid.NotificationHelper.CHANNEL_UPDATES

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

}
