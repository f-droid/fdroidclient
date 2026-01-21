package org.fdroid.updates

import android.content.Context
import org.fdroid.R

data class UpdateNotificationState(
    private val updates: List<AppUpdate>,
) {
    fun getTitle(context: Context) = context.resources.getQuantityString(
        R.plurals.notification_summary_app_updates,
        updates.size, updates.size,
    )

    fun getBigText(): String {
        return StringBuilder().apply {
            updates.forEach { update ->
                append("• ${update.name}")
                append(" ${update.currentVersionName} → ${update.updateVersionName}\n")
            }
        }.toString()
    }
}

data class AppUpdate(
    val packageName: String,
    val name: String,
    val currentVersionName: String,
    val updateVersionName: String,
)
