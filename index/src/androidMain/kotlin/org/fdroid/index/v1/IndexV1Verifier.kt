package org.fdroid.index.v1

import org.fdroid.index.IndexUtils.sha256
import org.fdroid.index.IndexUtils.toHex
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

private const val JSON_FILE_NAME = "index-v1.json"
private const val SUPPORTED_DIGEST = "SHA1-Digest"

/**
 * Verifies the old Index V1.
 *
 * @param jarFile the signed jar file to verify.
 * @param expectedSigningCertificate The signing certificate of the repo encoded in lower case hex,
 * if it is known already. This should only be null if the repo is unknown.
 * Then we trust it on first use (TOFU).
 * @param expectedSigningFingerprint The fingerprint, a SHA 256 hash of the
 * [expectedSigningCertificate]'s byte encoding as a lower case hex string.
 * Even if [expectedSigningFingerprint] is null, the fingerprint might be known and can be used to
 * verify that it matches the signing certificate.
 */
public class IndexV1Verifier(
    private val jarFile: File,
    private val expectedSigningCertificate: String?,
    private val expectedSigningFingerprint: String?,
) {

    init {
        require(expectedSigningCertificate == null || expectedSigningCertificate.isNotEmpty())
        require(expectedSigningFingerprint == null || expectedSigningFingerprint.isNotEmpty())
        require(expectedSigningCertificate == null || expectedSigningFingerprint == null) {
            "Providing a signing certificate and a fingerprint makes no sense."
        }
    }

    /**
     * Opens the [jarFile], verifies it and then gets signing certificate
     * as well as the index stream for further processes.
     * The caller does not need to close the stream.
     */
    @Throws(IOException::class, SigningException::class)
    public fun getStreamAndVerify(certificateAndStream: (InputStream) -> Unit): String {
        return JarFile(jarFile, true).use { file ->
            val indexEntry = file.getEntry(JSON_FILE_NAME) as? JarEntry
                ?: throw SigningException("No entry for $JSON_FILE_NAME")
            indexEntry.attributes?.keys?.forEach { key ->
                if (key.toString() != SUPPORTED_DIGEST) {
                    throw SigningException("Unsupported digest: $key")
                }
            } ?: throw SigningException("No attributes for $JSON_FILE_NAME")
            try {
                file.getInputStream(indexEntry).use { inputSteam ->
                    certificateAndStream(inputSteam)
                }
            } catch (e: SecurityException) {
                throw SigningException(e)
            }
            val x509Certificate = getX509Certificate(indexEntry)
            verifyAndGetSigningCertificate(x509Certificate)
        }
    }

    /**
     * Returns the [X509Certificate] for the given [jarEntry].
     *
     * F-Droid's index.jar is signed using a particular format
     * and does not allow other signing setups that would be valid for a regular jar
     * @throws SigningException if those restrictions are not met.
     */
    @Throws(SigningException::class)
    private fun getX509Certificate(jarEntry: JarEntry): X509Certificate {
        val codeSigners = jarEntry.codeSigners
        if (codeSigners.isNullOrEmpty()) {
            throw SigningException("No signature found in index, did you read stream until end?")
        }
        if (codeSigners.size != 1) {
            // we could in theory support more than 1, but as of now we do not
            throw SigningException("index.jar must be signed by a single code signer")
        }
        val certs = codeSigners[0].signerCertPath.certificates
        if (certs.size != 1) {
            throw SigningException("index.jar code signers must only have a single certificate")
        }
        return certs[0] as X509Certificate
    }

    /**
     * Verifies that the fingerprint of the signing certificate used to sign [JSON_FILE_NAME]
     * matches the [expectedSigningFingerprint].
     * @return the fingerprint of the given [rawCertFromJar].
     */
    @Throws(SigningException::class)
    private fun verifyAndGetSigningCertificate(rawCertFromJar: X509Certificate): String {
        val certificate: String = rawCertFromJar.encoded.toHex()
        if (certificate.isEmpty()) throw SigningException("No signing certificate")
        if (certificate.length < 512) {
            throw SigningException("Certificate size of ${certificate.length / 2} is too short.")
        }
        // if we have only the fingerprint, compare it against the given certificate
        if (expectedSigningCertificate == null && expectedSigningFingerprint != null) {
            val fingerprintFromJar = sha256(rawCertFromJar.encoded).toHex()
            if (expectedSigningFingerprint != fingerprintFromJar) {
                throw SigningException("Expected certificate fingerprint does not match")
            }
        }
        // if we have the full certificate, compare it to the one from the jar
        if (expectedSigningCertificate != null && expectedSigningCertificate != certificate) {
            throw SigningException("Signing certificate does not match")
        }
        return certificate
    }

}

public class SigningException(msg: String?, cause: Throwable?) : Exception(msg, cause) {
    internal constructor(msg: String) : this(msg, null)
    internal constructor(e: Throwable) : this(null, e)
}
