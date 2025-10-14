/*
 * Copyright (C) 2014-2017  Peter Serwylo <peter@serwylo.com>
 * Copyright (C) 2014-2018  Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2015-2016  Daniel Martí <mvdan@mvdan.cc>
 * Copyright (c) 2018  Senecto Limited
 * Copyright (C) 2022 Torsten Grote <t at grobox.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.fdroid.download

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode.Companion.NotFound
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.fdroid.fdroid.toHex
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Download files over HTTP, with support for proxies, `.onion` addresses, HTTP Basic Auth, etc.
 */
public class HttpDownloaderV2 constructor(
    private val httpManager: HttpManager,
    private val request: DownloadRequest,
    destFile: File,
) : Downloader(request.indexFile, destFile) {

    private companion object {
        val log = KotlinLogging.logger {}
    }

    override fun getInputStream(resumable: Boolean): InputStream {
        throw NotImplementedError("Use getInputStreamSuspend instead.")
    }

    @Throws(IOException::class, NoResumeException::class, NotFoundException::class)
    protected override suspend fun getBytes(resumable: Boolean, receiver: BytesReceiver) {
        val skipBytes = if (resumable) outputFile.length() else null
        return try {
            httpManager.get(request, skipBytes, receiver)
        } catch (e: ResponseException) {
            if (e.response.status == NotFound) throw NotFoundException(e)
            else throw IOException(e)
        }
    }

    @Throws(IOException::class, InterruptedException::class, NotFoundException::class)
    public override fun download() {
        var resumable = false
        val fileLength = outputFile.length()
        if (fileLength > (request.indexFile.size ?: -1)) {
            // file was larger than expected, so delete and re-download
            if (!outputFile.delete()) log.warn { "Warning: outputFile not deleted" }
        } else if (fileLength == request.indexFile.size && outputFile.isFile) {
            log.debug { "Already have outputFile, not downloading: ${outputFile.name}" }
            if (request.indexFile.sha256 == null) {
                // no way to check file, so we trust that what we have is legit (v1 only)
                return
            } else {
                if (hashFile(outputFile) == request.indexFile.sha256) {
                    // hash matched, so we already have the good file, don't download again
                    return
                } else {
                    log.warn { "Hash mismatch for ${request.indexFile}" }
                    // delete file and continue
                    if (!outputFile.delete()) log.warn { "Warning: outputFile not deleted" }
                }
            }
        } else if (fileLength > 0) {
            resumable = true
        }
        log.debug { "Downloading ${request.indexFile.name} (is resumable: $resumable)" }
        runBlocking {
            try {
                downloadFromBytesReceiver(resumable)
            } catch (e: NoResumeException) {
                if (!outputFile.delete()) log.warn { "Warning: outputFile not deleted" }
                downloadFromBytesReceiver(false)
            }
        }
    }

    protected override fun totalDownloadSize(): Long = request.indexFile.size ?: -1L

    @Deprecated("Only for v1 repos")
    override fun hasChanged(): Boolean {
        error("hasChanged() was called for V2 where it should not be needed.")
    }

    override fun close() {
    }

    private fun hashFile(file: File): String {
        val messageDigest: MessageDigest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        }
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytes = inputStream.read(buffer)
            while (bytes >= 0) {
                messageDigest.update(buffer, 0, bytes)
                bytes = inputStream.read(buffer)
            }
        }
        return messageDigest.digest().toHex()
    }

}
