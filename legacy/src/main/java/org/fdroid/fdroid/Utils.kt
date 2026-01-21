package org.fdroid.fdroid

import android.graphics.Bitmap
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.core.view.DisplayCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

@Composable
fun PaddingValues.copy(
    start: Dp? = null,
    top: Dp? = null,
    end: Dp? = null,
    bottom: Dp? = null,
): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = start ?: calculateStartPadding(dir),
        top = top ?: calculateTopPadding(),
        end = end ?: calculateEndPadding(dir),
        bottom = bottom ?: calculateBottomPadding(),
    )
}

class UiUtils {
    companion object {
        /**
         * Apply system bar insets to the given view.
         * This is commonly used for edge-to-edge, to offset elements from the system bars.
         *
         * By default, insets are applied on all sides.
         * You can pass false to disable modifying a side's padding.
         */
        @JvmStatic
        @JvmOverloads
        fun setupEdgeToEdge(view: View, top: Boolean = true, bottom: Boolean = true) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val i = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                v.updatePadding(
                    left = i.left,
                    right = i.right,
                )
                if (top) {
                    v.updatePadding(top = i.top)
                }
                if (bottom) {
                    v.updatePadding(bottom = i.bottom)
                }
                WindowInsetsCompat.CONSUMED
            }
        }
    }
}
