package org.fdroid.index.v2

import org.fdroid.index.JarIndexVerifier
import org.fdroid.index.SigningException
import java.io.File
import java.util.jar.Attributes

public const val DATA_FILE_NAME: String = "entry.json"
private val FORBIDDEN_DIGESTS = listOf(
    "MD5-Digest",
    "SHA1-Digest",
)

/**
 * Verifies the `entry.jar` file of Index V2.
 *
 * @param jarFile the signed `entry.jar` file to verify.
 * @param expectedSigningCertificate The signing certificate of the repo encoded in lower case hex,
 * if it is known already. This should only be null if the repo is unknown.
 * Then we trust it on first use (TOFU).
 * @param expectedSigningFingerprint The fingerprint, a SHA 256 hash of the
 * [expectedSigningCertificate]'s byte encoding as a lower case hex string.
 * Even if [expectedSigningFingerprint] is null, the fingerprint might be known and can be used to
 * verify that it matches the signing certificate.
 */
public class EntryVerifier(
    jarFile: File,
    expectedSigningCertificate: String?,
    expectedSigningFingerprint: String?,
) : JarIndexVerifier(jarFile, expectedSigningCertificate, expectedSigningFingerprint) {

    protected override val jsonFileName: String = DATA_FILE_NAME

    @Throws(SigningException::class)
    protected override fun checkAttributes(attributes: Attributes) {
        attributes.keys.forEach { key ->
            if (key.toString() in FORBIDDEN_DIGESTS) {
                throw SigningException("Unsupported digest: $key")
            }
        }
    }
}
