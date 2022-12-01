package org.fdroid.index

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

public object IndexUtils {
    public fun getFingerprint(certificate: String): String {
        return sha256(certificate.decodeHex()).toHex()
    }

    public fun getPackageSignature(signatureBytes: ByteArray): String {
        return sha256(signatureBytes).toHex()
    }

    internal fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    internal fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte ->
        "%02x".format(eachByte)
    }

    internal fun sha256(bytes: ByteArray): ByteArray {
        val messageDigest: MessageDigest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        }
        messageDigest.update(bytes)
        return messageDigest.digest()
    }
}
