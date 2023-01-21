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

    /**
     * Get the fingerprint used to represent an APK signing key in F-Droid.
     * This is a custom fingerprint algorithm that was kind of accidentally
     * created.  It is now here only for backwards compatibility.  It should
     * only ever be used for writing the `sig` value out to
     * `index-v1.json`.
     *
     * @see getPackageSigner
     * @see org.fdroid.fdroid.Utils.getPackageSigner
     * @see org.fdroid.fdroid.data.Apk
     */
    @Deprecated("Only here for backwards compatibility when writing out index-v1.json")
    public fun getsig(signerBytes: ByteArray): String {
        return md5(signerBytes.toHex().encodeToByteArray()).toHex()
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

    @Deprecated("Only here for backwards compatibility when writing out index-v1.json")
    internal fun md5(bytes: ByteArray): ByteArray {
        val messageDigest: MessageDigest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        }
        messageDigest.update(bytes)
        return messageDigest.digest()
    }
}
