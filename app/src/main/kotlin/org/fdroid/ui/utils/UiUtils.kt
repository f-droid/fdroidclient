package org.fdroid.ui.utils

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.PowerManager
import android.text.format.DateUtils
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.UriHandler
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.database.Repository
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

fun applyNewTheme(theme: String) {
    val mode = when (theme) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode)
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

/**
 * Same as the Java function Utils.generateQrBitmap, but using coroutines instead of Single and Disposable.
 */
suspend fun generateQrBitmap(qrData: String): Bitmap? = withContext(Dispatchers.Default) {
    return@withContext try {
        val bitMatrix = QRCodeWriter().encode(qrData, BarcodeFormat.QR_CODE, 800, 800)
        val qrCodeWidth = bitMatrix.width
        val qrCodeHeight = bitMatrix.height
        val pixels = IntArray(qrCodeWidth * qrCodeHeight)
        for (y in 0 until qrCodeHeight) {
            val offset = y * qrCodeWidth
            for (x in 0 until qrCodeWidth) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        createBitmap(qrCodeWidth, qrCodeHeight).apply {
            setPixels(pixels, 0, qrCodeWidth, 0, 0, qrCodeWidth, qrCodeHeight)
        }
    } catch (e: Exception) {
        Log.e("generateQrBitmap", "Could not encode QR as bitmap", e)
        null
    }
}

fun Long.asRelativeTimeString(): String {
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
    ).toString()
}

val String.flagEmoji: String?
    get() {
        if (this.length != 2) {
            return null
        }
        val chars = this.uppercase().toCharArray()
        val first = chars[0].code - 0x41 + 0x1F1E6
        val second = chars[1].code - 0x41 + 0x1F1E6
        val flagEmoji = String(Character.toChars(first) + Character.toChars(second))
        return flagEmoji
    }

val Repository.addressForUi: String
    get() = address.replaceFirst("https://", "")
        .replaceFirst("/repo", "")

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
