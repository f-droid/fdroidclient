package org.fdroid.fdroid

import android.graphics.Bitmap
import android.text.format.DateUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.DisplayCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.encode.Contents
import com.google.zxing.encode.QRCodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.fdroid.Utils.debugLog
import kotlin.math.min

private const val TAG = "Utils"

/**
 * Same as the Java function Utils.generateQrBitmap, but using coroutines instead of Single and Disposable.
 */
suspend fun generateQrBitmapKt(
    activity: AppCompatActivity,
    qrData: String,
): Bitmap = withContext(Dispatchers.Default) {
    val displayMode = DisplayCompat.getMode(activity, activity.windowManager.getDefaultDisplay())
    val qrCodeDimension = min(displayMode.physicalWidth, displayMode.physicalHeight)
    debugLog(TAG, "generating QRCode Bitmap of " + qrCodeDimension + "x" + qrCodeDimension)

    val encoder = QRCodeEncoder(
        qrData,
        null,
        Contents.Type.TEXT,
        BarcodeFormat.QR_CODE.toString(),
        qrCodeDimension,
    )
    return@withContext try {
        encoder.encodeAsBitmap()
    } catch (e: WriterException) {
        Log.e(TAG, "Could not encode QR as bitmap", e)
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
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

fun Long.asRelativeTimeString(): String {
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
    ).toString()
}
