package org.fdroid.index.v1

import org.fdroid.index.SigningException
import org.fdroid.test.VerifierConstants.CERTIFICATE
import org.fdroid.test.VerifierConstants.FINGERPRINT
import org.fdroid.test.VerifierConstants.VERIFICATION_DIR
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class IndexV1VerifierTest {

    @Test
    fun testNoCertWithFingerprint() {
        val file = File("$VERIFICATION_DIR/valid-v1.jar")
        assertFailsWith<IllegalArgumentException> {
            IndexV1Verifier(file, CERTIFICATE, FINGERPRINT)
        }
    }

    @Test
    fun testValid() {
        val file = File("$VERIFICATION_DIR/valid-v1.jar")

        val verifier = IndexV1Verifier(file, null, null)
        val (certificate, _) = verifier.getStreamAndVerify { inputStream ->
            assertEquals("foo\n", inputStream.readBytes().decodeToString())
        }
        assertEquals(CERTIFICATE, certificate)
    }

    @Test
    fun testValidMatchesFingerprint() {
        val file = File("$VERIFICATION_DIR/valid-v1.jar")

        val verifier = IndexV1Verifier(file, null, FINGERPRINT)
        val (certificate, _) = verifier.getStreamAndVerify { inputStream ->
            assertEquals("foo\n", inputStream.readBytes().decodeToString())
        }
        assertEquals(CERTIFICATE, certificate)
    }

    @Test
    fun testValidWrongFingerprint() {
        val file = File("$VERIFICATION_DIR/valid-v1.jar")

        val verifier = IndexV1Verifier(file, null, "foo bar")
        val e = assertFailsWith<SigningException> {
            verifier.getStreamAndVerify { inputStream ->
                assertEquals("foo\n", inputStream.readBytes().decodeToString())
            }
        }
        assertTrue(e.message!!.contains("fingerprint"))
    }

    @Test
    fun testValidWithExpectedCertificate() {
        val file = File("$VERIFICATION_DIR/valid-v1.jar")

        val verifier = IndexV1Verifier(file, CERTIFICATE, null)
        val (certificate, _) = verifier.getStreamAndVerify { inputStream ->
            assertEquals("foo\n", inputStream.readBytes().decodeToString())
        }
        assertEquals(CERTIFICATE, certificate)
    }

    @Test
    fun testValidWithWrongCertificate() {
        val file = File("$VERIFICATION_DIR/valid-v1.jar")

        val verifier = IndexV1Verifier(file, FINGERPRINT, null)
        val e = assertFailsWith<SigningException> {
            verifier.getStreamAndVerify { inputStream ->
                assertEquals("foo\n", inputStream.readBytes().decodeToString())
            }
        }
        assertTrue(e.message!!.contains("certificate"))
    }

    @Test
    fun testUnsigned() {
        val file = File("$VERIFICATION_DIR/unsigned.jar")

        val verifier = IndexV1Verifier(file, null, null)
        assertFailsWith<SigningException> {
            verifier.getStreamAndVerify { inputStream ->
                assertEquals("foo\n", inputStream.readBytes().decodeToString())
            }
        }
    }

    @Test
    fun testInvalid() {
        val file = File("$VERIFICATION_DIR/invalid-v1.jar")

        val verifier = IndexV1Verifier(file, null, null)
        assertFailsWith<SigningException> {
            verifier.getStreamAndVerify { inputStream ->
                assertEquals("foo\n", inputStream.readBytes().decodeToString())
            }
        }
    }

    @Test
    fun testWrongEntry() {
        val file = File("$VERIFICATION_DIR/invalid-wrong-entry-v1.jar")

        val verifier = IndexV1Verifier(file, null, null)
        val e = assertFailsWith<SigningException> {
            verifier.getStreamAndVerify { inputStream ->
                assertEquals("foo\n", inputStream.readBytes().decodeToString())
            }
        }
        assertTrue(e.message!!.contains(DATA_FILE_NAME))
    }

    @Test
    fun testMD5Digest() {
        val file = File("$VERIFICATION_DIR/invalid-MD5-SHA1withRSA-v1.jar")

        val verifier = IndexV1Verifier(file, null, null)
        val e = assertFailsWith<SigningException> {
            verifier.getStreamAndVerify { inputStream ->
                assertEquals("foo\n", inputStream.readBytes().decodeToString())
            }
        }
        assertTrue(e.message!!.contains("Unsupported digest"))
    }

    @Test
    fun testMD5SignatureAlgo() {
        val file = File("$VERIFICATION_DIR/invalid-MD5-MD5withRSA-v1.jar")

        val verifier = IndexV1Verifier(file, null, null)
        val e = assertFailsWith<SigningException> {
            verifier.getStreamAndVerify { inputStream ->
                assertEquals("foo\n", inputStream.readBytes().decodeToString())
            }
        }
        assertTrue(e.message!!.contains("Unsupported digest"))
    }

}
