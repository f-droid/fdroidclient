package org.fdroid.index

import org.fdroid.index.IndexUtils.getsig
import org.fdroid.index.IndexUtils.md5
import org.fdroid.index.IndexUtils.toHex
import org.junit.Test
import kotlin.test.assertEquals

internal class IndexUtilsTest {
    /**
     * Test the replacement for the ancient fingerprint algorithm.
     *
     * @see org.fdroid.fdroid.data.Apk.sig
     * @see org.fdroid.fdroid.Utils.getsig
     */
    @Deprecated("Only here for backwards compatibility when writing out index-v1.json")
    @Test
    fun testGetsig() {
        /*
         * I don't fully understand the loop used here. I've copied it verbatim
         * from getsig.java bundled with FDroidServer. I *believe* it is taking
         * the raw byte encoding of the certificate & converting it to a byte
         * array of the hex representation of the original certificate byte
         * array. This is then MD5 sum'd. It's a really bad way to be doing this
         * if I'm right... If I'm not right, I really don't know! see lines
         * 67->75 in getsig.java bundled with Fdroidserver
         */
        for (length in intArrayOf(256, 345, 1233, 4032, 12092)) {
            val rawCertBytes = ByteArray(length)
            java.util.Random().nextBytes(rawCertBytes)
            val fdroidSig = ByteArray(rawCertBytes.size * 2)
            for (j in rawCertBytes.indices) {
                val v = rawCertBytes[j].toInt()
                var d = ((v shr 4) and 0x000F).toByte() // Java: int d = (v >> 4) & 0xF;
                fdroidSig[j * 2] = (if (d >= 10) 'a'.code + d - 10 else '0'.code + d).toByte()
                d = (v and 0x000F).toByte() // Java: d = v & 0xF
                fdroidSig[j * 2 + 1] = (if (d >= 10) 'a'.code + d - 10 else '0'.code + d).toByte()
            }
            val sig = md5(fdroidSig).toHex()
            assertEquals(sig, getsig(rawCertBytes))
            assertEquals(sig, md5(rawCertBytes.toHex().toByteArray()).toHex())
        }
    }
}
