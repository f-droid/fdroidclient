package org.fdroid.index

import org.fdroid.index.IndexUtils.sha256
import org.fdroid.index.IndexUtils.toHex
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile

public abstract class JarIndexVerifier(
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

    protected abstract val jsonFileName: String

    @Throws(SigningException::class)
    protected abstract fun checkAttributes(attributes: Attributes)

    /**
     * Opens the [jarFile], verifies it and then gets signing certificate
     * as well as the index stream for further processing.
     * The caller does not need to close the stream.
     */
    @Throws(IOException::class, SigningException::class)
    public fun <T> getStreamAndVerify(certificateAndStream: (InputStream) -> T): Pair<String, T> {
        return JarFile(jarFile, true).use { file ->
            val indexEntry = file.getEntry(jsonFileName) as? JarEntry
                ?: throw SigningException("No entry for $jsonFileName")
            if (indexEntry.attributes == null) {
                throw SigningException("No attributes for $jsonFileName")
            } else {
                checkAttributes(indexEntry.attributes)
            }
            val t = try {
                file.getInputStream(indexEntry).use { inputSteam ->
                    certificateAndStream(inputSteam)
                }
            } catch (e: SecurityException) {
                throw SigningException(e)
            }
            val x509Certificate = getX509Certificate(indexEntry)
            Pair(verifyAndGetSigningCertificate(x509Certificate), t)
        }
    }

    /**
     * Returns the [X509Certificate] for the given [jarEntry].
     *
     * F-Droid's JAR is signed using a particular format
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
     * Verifies that the fingerprint of the signing certificate used to sign [jsonFileName]
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
    public constructor(msg: String) : this(msg, null)
    public constructor(e: Throwable) : this(null, e)
}
