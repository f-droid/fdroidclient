package org.fdroid.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

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
