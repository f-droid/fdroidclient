package org.fdroid.ui.utils

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.ui.platform.UriHandler
import java.text.Normalizer
import java.text.Normalizer.Form.NFKD

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
