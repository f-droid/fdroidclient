package org.fdroid.download

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.ResponseException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.UserAgent
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.Connection
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpHeaders.ETag
import io.ktor.http.HttpHeaders.LastModified
import io.ktor.http.HttpHeaders.Range
import io.ktor.http.HttpStatusCode.Companion.PartialContent
import io.ktor.http.contentLength
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.close
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writeFully
import mu.KotlinLogging
import kotlin.jvm.JvmOverloads

internal expect fun getHttpClientEngine(): HttpClientEngine

public open class HttpManager @JvmOverloads constructor(
    private val userAgent: String,
    queryString: String? = null,
    private val mirrorChooser: MirrorChooser = MirrorChooser(),
    httpClientEngine: HttpClientEngine = getHttpClientEngine(),
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val httpClient by lazy {
        HttpClient(httpClientEngine) {
            followRedirects = false
            expectSuccess = true
            engine {
                proxy = null // TODO use proxy except when swap
                threadsCount = 4
                pipelining = true
            }
            install(UserAgent) {
                agent = userAgent
            }
            defaultRequest {
                // add query string parameters if existing
                parameters?.forEach { (key, value) ->
                    parameter(key, value)
                }
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
    suspend fun head(request: DownloadRequest, eTag: String? = null): HeadInfo? {
        val authString = constructBasicAuthValue(request)
        val response: HttpResponse = try {
            mirrorChooser.mirrorRequest(request) { url ->
                log.debug { "URL: $url" }
                httpClient.head(url) {
                    // add authorization header from username / password if set
                    if (authString != null) header(Authorization, authString)
                }
            }
        } catch (e: ResponseException) {
            log.warn(e) { "Error getting HEAD" }
            return null
        }
        val contentLength = response.contentLength()
        val lastModified = response.headers[LastModified]
        if (eTag != null && response.headers[ETag] == eTag) {
            return HeadInfo(false, response.headers[ETag], contentLength, lastModified)
        }
        return HeadInfo(true, response.headers[ETag], contentLength, lastModified)
    }

    @JvmOverloads
    suspend fun get(
        request: DownloadRequest,
        skipFirstBytes: Long? = null,
        receiver: suspend (ByteArray) -> Unit,
    ) {
        val authString = constructBasicAuthValue(request)
        mirrorChooser.mirrorRequest(request) { url ->
            httpClient.get<HttpStatement>(url) {
                // add authorization header from username / password if set
                if (authString != null) header(Authorization, authString)
                // add range header if set
                if (skipFirstBytes != null) header(Range, "bytes=${skipFirstBytes}-")
                // avoid keep-alive for swap due to strange errors observed in the past
                // TODO still needed?
                if (request.isSwap) header(Connection, "Close")
            }
        }.execute { response ->
            if (skipFirstBytes != null && response.status != PartialContent) {
                throw ServerResponseException(response, "expected 206")
            }
            val channel: ByteReadChannel = response.receive()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(8L * 1024L)
                while (!packet.isEmpty) {
                    receiver(packet.readBytes())
                }
            }
        }
    }

    /**
     * Same as [get], but returns all bytes.
     * Use this only when you are sure that a response will be small.
     * Thus, this is intentionally visible internally only.
     */
    @JvmOverloads
    internal suspend fun getBytes(request: DownloadRequest, skipFirstBytes: Long? = null): ByteArray {
        val channel = ByteChannel()
        get(request, skipFirstBytes) { bytes ->
            channel.writeFully(bytes)
        }
        channel.close()
        return channel.toByteArray()
    }

    suspend fun post(url: String, json: String) {
        httpClient.post<HttpResponse>(url) {
            header(ContentType, "application/json; utf-8")
            body = json
        }
    }

    @OptIn(InternalAPI::class) // ktor 2.0 remove
    private fun constructBasicAuthValue(request: DownloadRequest): String? {
        if (request.username == null || request.password == null) return null
        val authString = "${request.username}:${request.password}"
        val authBuf = authString.toByteArray(Charsets.UTF_8).encodeBase64()
        return "Basic $authBuf"
    }

}
