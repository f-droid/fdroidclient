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

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import io.ktor.client.features.ResponseException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Date

/**
 * Download files over HTTP, with support for proxies, `.onion` addresses, HTTP Basic Auth, etc.
 */
class HttpDownloader @JvmOverloads constructor(
    private val httpManager: HttpManager,
    private val path: String,
    destFile: File,
    private val mirrors: List<Mirror>,
    private val username: String? = null,
    private val password: String? = null,
) : Downloader(destFile) {

    companion object {
        val log = KotlinLogging.logger {}

        @JvmStatic
        fun isSwapUrl(uri: Uri): Boolean {
            return isSwapUrl(uri.host, uri.port)
        }

        fun isSwapUrl(host: String?, port: Int): Boolean {
            return (port > 1023 // only root can use <= 1023, so never a swap repo
                    && host!!.matches(Regex("[0-9.]+")) // host must be an IP address
                    )
            // TODO check if is local
        }
    }

    private var hasChanged = false
    private var fileSize = -1L

    override fun getInputStream(resumable: Boolean): InputStream {
        throw NotImplementedError("Use getInputStreamSuspend instead.")
    }

    @Throws(IOException::class)
    override suspend fun getBytes(resumable: Boolean, receiver: (ByteArray) -> Unit) {
        val request = DownloadRequest(path, mirrors, username, password)
        val skipBytes = if (resumable) outputFile.length() else null
        return try {
            httpManager.get(request, skipBytes, receiver)
        } catch (e: ResponseException) {
            throw IOException(e)
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
    @OptIn(DelicateCoroutinesApi::class)
    @Throws(IOException::class, InterruptedException::class)
    override fun download() {
        // boolean isSwap = isSwapUrl(sourceUrl);
        val request = DownloadRequest(path, mirrors, username, password)
        val headInfo = runBlocking {
            httpManager.head(request, cacheTag) ?: throw IOException()
        }
        val expectedETag = cacheTag
        cacheTag = headInfo.eTag
        fileSize = headInfo.contentLength ?: -1

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
            log.debug { "$path cached, not downloading." }
            hasChanged = false
            return
        }

        hasChanged = true
        var resumable = false
        val fileLength = outputFile.length()
        if (fileLength > fileSize) {
            if (!outputFile.delete()) log.warn { "Warning: " + outputFile.absolutePath + " not deleted" }
        } else if (fileLength == fileSize && outputFile.isFile) {
            log.debug { "Already have outputFile, not download. ${outputFile.absolutePath}" }
            return  // already have it!
        } else if (fileLength > 0) {
            resumable = true
        }
        log.debug { "downloading $path (is resumable: $resumable)" }
        runBlocking { downloadFromBytesReceiver(resumable) }
    }

    @TargetApi(24)
    public override fun totalDownloadSize(): Long {
        return if (SDK_INT < 24) {
            fileSize.toInt().toLong() // TODO why?
        } else {
            fileSize
        }
    }

    override fun hasChanged(): Boolean {
        return hasChanged
    }

    override fun close() {
    }

}
