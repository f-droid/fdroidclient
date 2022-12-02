package org.fdroid.download

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.config
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.respondRedirect
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ETag
import io.ktor.http.HttpHeaders.Range
import io.ktor.http.HttpHeaders.UserAgent
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.PartialContent
import io.ktor.http.HttpStatusCode.Companion.TemporaryRedirect
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.utils.io.core.internal.ChunkBuffer
import io.ktor.utils.io.core.writeFully
import org.fdroid.TestByteReadChannel
import org.fdroid.download.HttpManager.Companion.READ_BUFFER
import org.fdroid.get
import org.fdroid.getRandomString
import org.fdroid.runSuspend
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

internal class HttpManagerTest {

    private val userAgent = getRandomString()
    private val mirrors = listOf(Mirror("http://example.org"), Mirror("http://example.net/"))
    private val downloadRequest = DownloadRequest("foo", mirrors)

    @Test
    fun testUserAgent() = runSuspend {
        val mockEngine = MockEngine { respondOk() }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        httpManager.head(downloadRequest)
        httpManager.getBytes(downloadRequest)

        mockEngine.requestHistory.forEach { request ->
            assertEquals(userAgent, request.headers[UserAgent])
        }
    }

    @Test
    fun testQueryString() = runSuspend {
        val id = getRandomString()
        val version = getRandomString()
        val queryString = "id=$id&client_version=$version"
        val mockEngine = MockEngine { respondOk() }
        val httpManager =
            HttpManager(userAgent, queryString, httpClientEngineFactory = get(mockEngine))

        httpManager.head(downloadRequest)
        httpManager.getBytes(downloadRequest)

        mockEngine.requestHistory.forEach { request ->
            assertEquals(id, request.url.parameters["id"])
            assertEquals(version, request.url.parameters["client_version"])
        }
    }

    @Test
    fun testBasicAuth() = runSuspend {
        val downloadRequest = DownloadRequest("foo", mirrors, null, "Foo", "Bar")

        val mockEngine = MockEngine { respondOk() }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        httpManager.head(downloadRequest)
        httpManager.getBytes(downloadRequest)

        mockEngine.requestHistory.forEach { request ->
            assertEquals("Basic Rm9vOkJhcg==", request.headers[Authorization])
        }
    }

    @Test
    fun testHeadETagCheck() = runSuspend {
        val eTag = getRandomString()
        val headers = headersOf(ETag, eTag)
        val mockEngine = MockEngine { respond("", headers = headers) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        // ETag is considered changed when none (null) passed into the request
        assertTrue(httpManager.head(downloadRequest)!!.eTagChanged)
        // Random ETag will be different than what we expect
        assertTrue(httpManager.head(downloadRequest, getRandomString())!!.eTagChanged)
        // Expected ETag should match response, so it hasn't changed
        assertFalse(httpManager.head(downloadRequest, eTag)!!.eTagChanged)
    }

    @Test
    fun testDownload() = runSuspend {
        val content = Random.nextBytes(1024)

        val mockEngine = MockEngine { respond(content) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        assertContentEquals(content, httpManager.getBytes(downloadRequest))
    }

    @Test
    fun testResumeDownload() = runSuspend {
        val skipBytes = Random.nextInt(0, 1024)
        val content = Random.nextBytes(1024)

        var requestNum = 1
        val mockEngine = MockEngine { request ->
            val from = request.getByteRangeFrom()
            assertEquals(skipBytes, from)
            if (requestNum++ == 1) respond(content.copyOfRange(from, content.size), PartialContent)
            else respond(content, OK)
        }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        // first request gets only the skipped bytes
        assertContentEquals(
            content.copyOfRange(skipBytes, content.size),
            httpManager.getBytes(downloadRequest, skipBytes.toLong())
        )
        // second request fails, because it responds with OK and full content
        assertFailsWith<NoResumeException> {
            httpManager.getBytes(downloadRequest, skipBytes.toLong())
        }
    }

    @Test
    fun testResumeDownloadWhenMirrorFailOver() = runSuspend {
        val failBytes = READ_BUFFER
        val content = Random.nextBytes(failBytes * 2)

        val readChannel = object : TestByteReadChannel() {
            var wasRead = 0
            override val availableForRead: Int = 4096
            override suspend fun readAvailable(dst: ChunkBuffer): Int {
                // We allow three reads. Only the first two give us the first half of content.
                // While the third seems to be required, it isn't filling the buffer
                // before we throw the exception, so it isn't considered.
                if (wasRead == 3) throw SocketTimeoutException("boom!")
                dst.writeFully(content, wasRead * 4096, 4096)
                wasRead++
                return 4096
            }
        }

        val mockEngine = MockEngine.config {
            reuseHandlers = false
            addHandler {
                respond(readChannel, OK)
            }
            addHandler { request ->
                val from = request.getByteRangeFrom()
                assertEquals(failBytes, from)
                respond(content.copyOfRange(from, content.size), PartialContent)
            }
        }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = mockEngine)

        var chunk = 0
        httpManager.get(downloadRequest) { bytes, _ ->
            // we expect two chunks: 0 and 1
            // the first is the first half of content and the second is the second half
            val offset = chunk * READ_BUFFER
            val expectedBytes = content.copyOfRange(offset, offset + READ_BUFFER)
            assertContentEquals(expectedBytes, bytes)
            chunk++
        }
        assertEquals(2, chunk)
    }

    @Test
    fun testMirrorFallback() = runSuspend {
        val mockEngine = MockEngine { respondError(InternalServerError) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        assertNull(httpManager.head(downloadRequest))
        assertFailsWith<ServerResponseException> {
            httpManager.getBytes(downloadRequest)
        }

        // assert that URLs for each mirror get tried
        val urls = mockEngine.requestHistory.map { request -> request.url.toString() }.toSet()
        assertEquals(setOf("http://example.org/foo", "http://example.net/foo"), urls)
    }

    @Test
    fun testFirstMirrorSuccess() = runSuspend {
        val mockEngine = MockEngine { respondOk() }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        assertNotNull(httpManager.head(downloadRequest))
        httpManager.getBytes(downloadRequest)

        // assert there is only one request per API call using one of the mirrors
        assertEquals(2, mockEngine.requestHistory.size)
        mockEngine.requestHistory.forEach { request ->
            val url = request.url.toString()
            assertTrue(url == "http://example.org/foo" || url == "http://example.net/foo")
        }
    }

    @Test
    fun testNoMoreMirrorsWhenForbiddenWithCredentials() = runSuspend {
        val downloadRequest =
            downloadRequest.copy(username = getRandomString(), password = getRandomString())
        val mockEngine = MockEngine { respond("", Forbidden) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        assertTrue(downloadRequest.hasCredentials)

        assertNull(httpManager.head(downloadRequest))
        val e = assertFailsWith<ClientRequestException> {
            httpManager.getBytes(downloadRequest)
        }

        // assert that the exception reflects the forbidden
        assertEquals(Forbidden, e.response.status)
        // assert there is only one request per API call using one of the mirrors
        assertEquals(2, mockEngine.requestHistory.size)
        mockEngine.requestHistory.forEach { request ->
            val url = request.url.toString()
            assertTrue(url == "http://example.org/foo" || url == "http://example.net/foo")
        }
    }

    @Test
    fun testNoMoreMirrorsWhenRepoDownloadNotFound() = runSuspend {
        val downloadRequest = downloadRequest.copy(tryFirstMirror = mirrors[0])
        val mockEngine = MockEngine { respond("", NotFound) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        assertTrue(downloadRequest.tryFirstMirror != null)

        assertFailsWith<NotFoundException> {
            httpManager.head(downloadRequest)
        }
        val e = assertFailsWith<ClientRequestException> {
            httpManager.getBytes(downloadRequest)
        }

        // assert that the exception reflects the NotFound error
        assertEquals(NotFound, e.response.status)
        // assert there is only one request per API call using one of the mirrors
        assertEquals(2, mockEngine.requestHistory.size)
        mockEngine.requestHistory.forEach { request ->
            val url = request.url.toString()
            assertTrue(url == "http://example.org/foo" || url == "http://example.net/foo")
        }
    }

    @Test
    fun testNoRedirect() = runSuspend {
        val mockEngine = MockEngine { respondRedirect("http://example.com") }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))

        assertNull(httpManager.head(downloadRequest))
        assertFailsWith<RedirectResponseException> {
            httpManager.getBytes(downloadRequest)
        }

        // HEAD and GET try another mirror, so 4 requests
        assertEquals(4, mockEngine.requestHistory.size)
        mockEngine.responseHistory.forEach { response ->
            assertEquals(TemporaryRedirect, response.statusCode)
        }
    }

    @Test
    fun testProxyGetsApplied() = runSuspend {
        val proxyConfig = ProxyBuilder.http(Url("http://127.0.0.1:5050"))
        val proxyRequest = DownloadRequest("foo", mirrors, proxyConfig)
        val noProxyRequest = DownloadRequest("foo", mirrors)

        var numRequests = 0
        val factory = object : HttpClientEngineFactory<MockEngineConfig> {
            override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine {
                return when (++numRequests) {
                    1 -> MockEngine { respondOk() }
                    2 -> MockEngine { respondOk() }
                    3 -> MockEngine { respondOk() }
                    else -> fail("Too many engine creations")
                }
            }
        }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = factory)
        assertNull(httpManager.currentProxy)

        // does not need a new engine, because also doesn't use a proxy
        assertNotNull(httpManager.head(noProxyRequest))
        assertNull(httpManager.currentProxy)

        // now wants proxy, creates new engine (2)
        assertNotNull(httpManager.head(proxyRequest))
        assertEquals(proxyConfig, httpManager.currentProxy)

        // no more proxy, creates new engine (3)
        httpManager.getBytes(noProxyRequest)
        assertNull(httpManager.currentProxy)

        assertEquals(3, numRequests)
    }

    @Test
    fun testNoProxyWithLocalMirror() = runSuspend {
        val mirror = Mirror("http://192.168.49.5")
        assertTrue(mirror.isLocal())
        val proxyConfig = ProxyBuilder.http(Url("http://127.0.0.1:5050"))
        val localRequest = DownloadRequest("foo", listOf(mirror), proxyConfig)
        val internetRequest = DownloadRequest("foo", mirrors, proxyConfig)

        var numEngines = 0
        val factory = object : HttpClientEngineFactory<MockEngineConfig> {
            override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine {
                return when (++numEngines) {
                    1 -> MockEngine { respondOk() }
                    2 -> MockEngine { respondOk() }
                    else -> fail("Too many engine creations")
                }
            }
        }
        val httpManager =
            HttpManager(userAgent, null, proxyConfig, httpClientEngineFactory = factory)
        assertEquals(proxyConfig, httpManager.currentProxy)

        // does not need a new engine, because also does use a proxy (1)
        assertNotNull(httpManager.head(internetRequest))
        assertEquals(proxyConfig, httpManager.currentProxy)

        // now no proxy, because local mirror, creates new engine (2)
        assertNotNull(httpManager.head(localRequest))
        assertNull(httpManager.currentProxy)

        // still no proxy, because local mirror as well, should not create new engine
        assertNotNull(httpManager.getBytes(localRequest))
        assertNull(httpManager.currentProxy)

        assertEquals(2, numEngines)
    }

    private fun HttpRequestData.getByteRangeFrom(): Int {
        val (fromStr, endStr) = (headers[Range] ?: fail("No Range header"))
            .replace("bytes=", "")
            .split('-')
        assertEquals("", endStr)
        return fromStr.toIntOrNull() ?: fail("No valid content range ${headers[Range]}")
    }

}
