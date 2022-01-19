package org.fdroid.download

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.ProxyConfig
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

internal expect fun getHttpClientEngineFactory(): HttpClientEngineFactory<*>

public open class HttpManager @JvmOverloads constructor(
    private val userAgent: String,
    queryString: String? = null,
    proxyConfig: ProxyConfig? = null,
    private val mirrorChooser: MirrorChooser = MirrorChooserRandom(),
    private val httpClientEngineFactory: HttpClientEngineFactory<*> = getHttpClientEngineFactory(),
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private var httpClient = getNewHttpClient(proxyConfig)

    /**
     * Only exists because KTor doesn't keep a reference to the proxy its client uses.
     * Should only get set in [getNewHttpClient].
     */
    internal var currentProxy: ProxyConfig? = null
        private set

    private val parameters = queryString?.split('&')?.map { p ->
        val (key, value) = p.split('=')
        Pair(key, value)
    }

    private fun getNewHttpClient(proxyConfig: ProxyConfig? = null): HttpClient {
        currentProxy = proxyConfig
        return HttpClient(httpClientEngineFactory) {
            followRedirects = false
            expectSuccess = true
            engine {
                threadsCount = 4
                pipelining = true
                proxy = proxyConfig
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

    /**
     * Performs a HEAD request and returns [HeadInfo].
     *
     * This is useful for checking if the repository index has changed before downloading it again.
     * However, due to non-standard ETags on mirrors, change detection is unreliable.
     */
    suspend fun head(request: DownloadRequest, eTag: String? = null): HeadInfo? {
        val authString = constructBasicAuthValue(request)
        val response: HttpResponse = try {
            mirrorChooser.mirrorRequest(request) { mirror, url ->
                resetProxyIfNeeded(request.proxy, mirror)
                log.debug { "HEAD $url" }
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
        mirrorChooser.mirrorRequest(request) { mirror, url ->
            resetProxyIfNeeded(request.proxy, mirror)
            log.debug { "GET $url" }
            httpClient.get<HttpStatement>(url) {
                // add authorization header from username / password if set
                if (authString != null) header(Authorization, authString)
                // add range header if set
                if (skipFirstBytes != null) header(Range, "bytes=${skipFirstBytes}-")
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

    suspend fun post(url: String, json: String, proxy: ProxyConfig? = null) {
        resetProxyIfNeeded(proxy)
        httpClient.post<HttpResponse>(url) {
            header(ContentType, "application/json; utf-8")
            body = json
        }
    }

    private fun resetProxyIfNeeded(proxyConfig: ProxyConfig?, mirror: Mirror? = null) {
        // force no-proxy when trying to hit a local mirror
        val newProxy = if (mirror.isLocal() && proxyConfig != null) {
            if (currentProxy != null) log.info { "Forcing mirror to null, because mirror is local: $mirror" }
            null
        } else proxyConfig
        if (currentProxy != newProxy) {
            log.info { "Switching proxy from [$currentProxy] to [$newProxy]"}
            httpClient.close()
            httpClient = getNewHttpClient(newProxy)
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
