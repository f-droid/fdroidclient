package org.fdroid.index

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

public object IndexUtils {
    public fun getFingerprint(certificate: String): String {
        return sha256(certificate.decodeHex()).toHex()
    }

    /**
     * Get the standard, lowercase SHA-256 fingerprint used to represent an
     * APK or JAR signing key. **NOTE**: this does not handle signers that
     * have multiple X.509 signing certificates.
     * <p>
     * Calling the X.509 signing certificate the "signature" is incorrect, e.g.
     * [android.content.pm.PackageInfo.signatures] or [android.content.pm.Signature].
     * The Android docs about APK signatures call this the "signer".
     *
     * @see org.fdroid.fdroid.data.Apk#signer
     * @see android.content.pm.PackageInfo#signatures
     * @see <a href="https://source.android.com/docs/security/features/apksigning/v2">APK Signature Scheme v2</a>
     */
    public fun getPackageSigner(signerBytes: ByteArray): String {
        return sha256(signerBytes).toHex()
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
