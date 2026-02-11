package org.fdroid.utils

import android.content.Context
import org.fdroid.BuildConfig.FLAVOR
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalStdlibApi::class)
fun sha256(bytes: ByteArray): String {
    val messageDigest: MessageDigest = try {
        MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
        throw AssertionError(e)
    }
    messageDigest.update(bytes)
    return messageDigest.digest().toHexString()
}

fun getLogName(context: Context): String {
    val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val time = sdf.format(Date())
    return "${context.packageName}-$time"
}

val isFull: Boolean get() = FLAVOR.startsWith("full")
val isBasic: Boolean get() = FLAVOR.startsWith("basic")
