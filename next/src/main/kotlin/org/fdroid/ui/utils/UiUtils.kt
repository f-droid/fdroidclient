package org.fdroid.ui.utils

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.UriHandler
import androidx.core.content.ContextCompat
import java.text.Normalizer
import java.text.Normalizer.Form.NFKD

@Composable
fun getHintOverlayColor() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)

fun Context.startActivitySafe(i: Intent?) {
    if (i == null) return
    try {
        startActivity(i)
    } catch (e: Exception) {
        Log.e("Context", "Error opening $i ", e)
    }
}

fun UriHandler.openUriSafe(uri: String) {
    try {
        openUri(uri)
    } catch (e: Exception) {
        Log.e("UriHandler", "Error opening $uri ", e)
    }
}

private val normalizerRegex = "\\p{M}".toRegex()

/**
 * Normalizes the string by removing any diacritics that may appear.
 */
fun String.normalize(): String {
    if (Normalizer.isNormalized(this, NFKD)) return this
    return Normalizer.normalize(this, NFKD).replace(normalizerRegex, "")
}

fun Long.asRelativeTimeString(): String {
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
    ).toString()
}

fun canStartForegroundService(context: Context): Boolean {
    val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
        ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName) ||
        context.isAppInForeground()
}

fun Context.isAppInForeground(): Boolean {
    val activityManager = ContextCompat.getSystemService(this, ActivityManager::class.java)
    val runningAppProcesses = activityManager?.runningAppProcesses ?: return false
    for (appProcess in runningAppProcesses) {
        if (appProcess.importance == IMPORTANCE_FOREGROUND &&
            appProcess.processName == packageName
        ) return true
    }
    return false
}
