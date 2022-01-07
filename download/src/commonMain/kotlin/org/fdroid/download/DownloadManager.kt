package org.fdroid.download

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.ResponseException
import io.ktor.client.features.UserAgent
import io.ktor.client.features.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.Connection
import io.ktor.http.HttpHeaders.ETag
import io.ktor.http.HttpHeaders.LastModified
import io.ktor.http.contentLength
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlin.jvm.JvmOverloads

public open class DownloadManager(
    private val userAgent: String,
    queryString: String? = null,
) {

    private val httpClient by lazy {
        HttpClient {
            followRedirects = false
            expectSuccess = true
            developmentMode = true // TODO remove
            engine {
                proxy = null // TODO use proxy except when swap
                threadsCount = 4
                pipelining = true
            }
            install(UserAgent) {
                agent = userAgent
            }
        }
    }
    private val parameters = queryString?.split('&')?.map { p ->
        val (key, value) = p.split('=')
        Pair(key, value)
    }

    // TODO try to force onion addresses over proxy like NetCipher.getHttpURLConnection()

    /**
     * Performs a HEAD request and returns [HeadInfo].
     *
     * This is useful for checking if the repository index has changed before downloading it again.
     * However, due to non-standard ETags on mirrors, change detection is unreliable.
     */
    suspend fun head(request: DownloadRequest, eTag: String?): HeadInfo? {
        val authString = constructBasicAuthValue(request)
        val response: HttpResponse = try {
            httpClient.head(request.url) {
                // add authorization header from username / password if set
                if (authString != null) header(Authorization, authString)
            }
        } catch (e: ResponseException) {
            println(e)
            return null
        }
        val contentLength = response.contentLength()
        val lastModified = response.headers[LastModified]
        if (eTag != null && response.headers[ETag] == eTag) {
            return HeadInfo(false, contentLength, lastModified)
        }
        return HeadInfo(true, contentLength, lastModified)
    }

    @JvmOverloads
    suspend fun get(request: DownloadRequest, skipFirstBytes: Long? = null): ByteReadChannel {
        val authString = constructBasicAuthValue(request)
        val response: HttpResponse = httpClient.get(request.url) {
            // add query string parameters if existing
            parameters?.forEach { (key, value) ->
                parameter(key, value)
            }
            // add authorization header from username / password if set
            if (authString != null) header(Authorization, authString)
            // add range header if set
            if (skipFirstBytes != null) header("Range", "bytes=${skipFirstBytes}-")
            // avoid keep-alive for swap due to strange errors observed in the past
            if (request.isSwap) header(Connection, "Close")

            onDownload { bytesSentTotal, contentLength ->
                println("Received $bytesSentTotal bytes from $contentLength")
            }
        }
        return response.receive() // 2.0 .bodyAsChannel()
    }

    @OptIn(InternalAPI::class) // 2.0 remove
    private fun constructBasicAuthValue(request: DownloadRequest): String? {
        if (request.username == null || request.password == null) return null
        val authString = "${request.username}:${request.password}"
        val authBuf = authString.toByteArray(Charsets.UTF_8).encodeBase64()
        return "Basic $authBuf"
    }

}
