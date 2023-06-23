/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.fdroid.fdroid

import java.io.FilterInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * A [FilterInputStream] that updated the given [messageDigest] while reading from the stream.
 */
@Suppress("MemberVisibilityCanBePrivate")
public class DigestInputStream(
    inputStream: InputStream,
    private val messageDigest: MessageDigest,
) : FilterInputStream(inputStream) {

    override fun read(): Int {
        val b = `in`.read()
        if (b != -1) messageDigest.update(b.toByte())
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val numOfBytesRead = `in`.read(b, off, len)
        if (numOfBytesRead != -1) {
            messageDigest.update(b, off, numOfBytesRead)
        }
        return numOfBytesRead
    }

    override fun markSupported(): Boolean {
        return false
    }

    override fun mark(readlimit: Int) {
    }

    override fun reset() {
        throw NotImplementedError()
    }

    /**
     * Completes the hash computation by performing final operations such as padding
     * and returns the resulting hash as a hex string.
     * The digest is reset after this call is made,
     * so call this only once and hang on to the result.
     */
    public fun getDigestHex(): String {
        return messageDigest.digest().toHex()
    }
}
