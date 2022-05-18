package org.fdroid.fdroid

import java.security.MessageDigest

internal fun MessageDigest?.isMatching(sha256: String): Boolean {
    if (this == null) return false
    val hexDigest = digest().toHex()
    return hexDigest.equals(sha256, ignoreCase = true)
}

internal fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte ->
    "%02x".format(eachByte)
}
