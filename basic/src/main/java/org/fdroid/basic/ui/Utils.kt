package org.fdroid.basic.ui

import android.text.format.DateUtils
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun Long.asRelativeTimeString(): String {
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
    ).toString()
}

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
