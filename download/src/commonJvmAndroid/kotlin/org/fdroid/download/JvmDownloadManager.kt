package org.fdroid.download

import io.ktor.client.features.ResponseException
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.util.Date

// FIXME ideally we can get rid of this wrapper, only need it for Java 7 right now (SDK < 24)
public class JvmDownloadManager(
    userAgent: String,
    queryString: String?,
) : DownloadManager(userAgent, queryString) {

    @Throws(IOException::class)
    fun headBlocking(request: DownloadRequest, eTag: String?): HeadInfo = runBlocking {
        val headInfo = head(request, eTag) ?: throw IOException()
        if (eTag == null) return@runBlocking headInfo
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
            Date.parse(headInfo.lastModified) / 1000
        } catch (e: Exception) {
            0L
        }
        val calculatedEtag: String =
            String.format("\"%x-%x\"", lastModified, headInfo.contentLength)
        if (calculatedEtag == eTag) headInfo.copy(eTagChanged = false)
        else headInfo
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun getBlocking(request: DownloadRequest, skipFirstBytes: Long? = null): InputStream =
        runBlocking {
            try {
                get(request, skipFirstBytes).toInputStream()
            } catch (e: ResponseException) {
                throw IOException(e)
            }
        }

}
