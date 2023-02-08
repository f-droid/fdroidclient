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
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Date

/**
 * Download files over HTTP, with support for proxies, `.onion` addresses, HTTP Basic Auth, etc.
 */
@Deprecated("Only for v1 repos")
public class HttpDownloader constructor(
    private val httpManager: HttpManager,
    private val request: DownloadRequest,
    destFile: File,
) : Downloader(request.indexFile, destFile) {

    private companion object {
        val log = KotlinLogging.logger {}
    }

    private var hasChanged = false
    private var fileSize: Long? = request.indexFile.size

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

    /**
     * Get a remote file, checking the HTTP response code, if it has changed since
     * the last time a download was tried.
     *
     *
     * If the `ETag` does not match, it could be caused by the previous
     * download of the same file coming from a mirror running on a different
     * webserver, e.g. Apache vs Nginx.  `Content-Length` and
     * `Last-Modified` are used to check whether the file has changed since
     * those are more standardized than `ETag`.  Plus, Nginx and Apache 2.4
     * defaults use only those two values to generate the `ETag` anyway.
     * Unfortunately, other webservers and CDNs have totally different methods
     * for generating the `ETag`.  And mirrors that are syncing using a
     * method other than `rsync` could easily have different `Last-Modified`
     * times on the exact same file.  On top of that, some services like GitHub's
     * raw file support `raw.githubusercontent.com` and GitLab's raw file
     * support do not set the `Last-Modified` header at all.  So ultimately,
     * then `ETag` needs to be used first and foremost, then this calculated
     * `ETag` can serve as a common fallback.
     *
     *
     * In order to prevent the `ETag` from being used as a form of tracking
     * cookie, this code never sends the `ETag` to the server.  Instead, it
     * uses a `HEAD` request to get the `ETag` from the server, then
     * only issues a `GET` if the `ETag` has changed.
     *
     *
     * This uses a integer value for `Last-Modified` to avoid enabling the
     * use of that value as some kind of "cookieless cookie".  One second time
     * resolution should be plenty since these files change more on the time
     * space of minutes or hours.
     *
     * @see [update index from any available mirror](https://gitlab.com/fdroid/fdroidclient/issues/1708)
     *
     * @see [Cookieless cookies](http://lucb1e.com/rp/cookielesscookies)
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use only for v1 repos")
    @Throws(IOException::class, InterruptedException::class)
    public override fun download() {
        val headInfo = runBlocking {
            httpManager.head(request, cacheTag) ?: throw IOException()
        }
        val expectedETag = cacheTag
        cacheTag = headInfo.eTag
        fileSize = headInfo.contentLength ?: request.indexFile.size ?: -1

        // If the ETag does not match, it could be because the file is on a mirror
        // running a different webserver, e.g. Apache vs Nginx.
        // Content-Length and Last-Modified could be used as well.
        // Nginx and Apache 2.4 defaults use only those two values to generate the ETag.
        // Unfortunately, other webservers and CDNs have totally different methods.
        // And mirrors that are syncing using a method other than rsync
        // could easily have different Last-Modified times on the exact same file.
        // On top of that, some services like GitHub's and GitLab's raw file support
        // do not set the header at all.
        val lastModified = try {
            // this method is not available multi-platform, so for now only done in JVM
            @Suppress("Deprecation")
            Date.parse(headInfo.lastModified) / 1000
        } catch (e: Exception) {
            0L
        }
        val calculatedEtag: String =
            String.format("%x-%x", lastModified, headInfo.contentLength)

        // !headInfo.eTagChanged: expectedETag == headInfo.eTag (the expected ETag was in server response)
        // calculatedEtag == expectedETag (ETag calculated from server response matches expected ETag)
        if (!headInfo.eTagChanged || calculatedEtag == expectedETag) {
            // ETag has not changed, don't download again
            log.debug { "${request.indexFile.name} cached, not downloading." }
            hasChanged = false
            return
        }

        hasChanged = true
        downloadToFile()
    }

    private fun downloadToFile() {
        var resumable = false
        val fileLength = outputFile.length()
        if (fileLength > (fileSize ?: -1)) {
            if (!outputFile.delete()) log.warn { "Warning: outputFile not deleted" }
        } else if (fileLength == fileSize && outputFile.isFile) {
            log.debug { "Already have outputFile, not downloading: ${outputFile.name}" }
            return // already have it!
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

    protected override fun totalDownloadSize(): Long = fileSize ?: -1L

    @Suppress("DEPRECATION")
    @Deprecated("Only for v1 repos")
    override fun hasChanged(): Boolean {
        return hasChanged
    }

    override fun close() {
    }

}
