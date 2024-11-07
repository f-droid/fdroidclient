package org.fdroid.download.glide

import org.fdroid.fdroid.isMatching
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * An [InputStream] that automatically verifies the [expectedHash] in lower-case SHA-256 format
 * and ensures that not more than [maxBytesToRead] are read from the stream.
 * This is useful to put an upper bound on data read to not exhaust memory.
 */
internal class AutoVerifyingInputStream(
    inputStream: InputStream,
    private val expectedHash: String,
    private val maxBytesToRead: Long = Runtime.getRuntime().maxMemory() / 2,
) : DigestInputStream(inputStream, MessageDigest.getInstance("SHA-256")) {

    private var bytesRead = 0

    override fun read(): Int {
        val readByte = super.read()
        if (readByte != -1) {
            bytesRead++
            if (bytesRead > maxBytesToRead) {
                throw IOException("Read $bytesRead bytes, above maximum allowed.")
            }
        } else {
            if (!digest.isMatching(expectedHash)) throw IOException("Hash not matching.")
        }
        return readByte
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        val read = super.read(b, off, len)
        if (read != -1) {
            bytesRead += read
            if (bytesRead > maxBytesToRead) {
                throw IOException("Read $bytesRead bytes, above maximum allowed.")
            }
        } else {
            if (!digest.isMatching(expectedHash)) throw IOException("Hash not matching.")
        }
        return read
    }
}
