package org.fdroid.download

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.utils.buildHeaders
import io.ktor.http.HttpHeaders.ContentLength
import io.ktor.http.HttpHeaders.ETag
import io.ktor.http.HttpHeaders.LastModified
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.PartialContent
import io.ktor.http.headersOf
import org.fdroid.getRandomString
import org.fdroid.runSuspend
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@Suppress("BlockingMethodInNonBlockingContext")
class HttpDownloaderTest {

    @get:Rule
    var folder = TemporaryFolder()

    private val userAgent = getRandomString()
    private val mirror1 = Mirror("http://example.org")
    private val mirrors = listOf(mirror1)

    @Test
    fun testDownload() = runSuspend {
        val file = folder.newFile()
        val bytes = Random.nextBytes(1024)

        val mockEngine = MockEngine { respond(bytes) }
        val httpManager = HttpManager(userAgent, null, httpClientEngine = mockEngine)
        val httpDownloader = HttpDownloader(httpManager, "foo/bar", file, mirrors)
        httpDownloader.download()

        assertContentEquals(bytes, file.readBytes())
    }

    @Test
    fun testResumeSuccess() = runSuspend {
        val file = folder.newFile()
        val firstBytes = Random.nextBytes(1024)
        file.writeBytes(firstBytes)
        val secondBytes = Random.nextBytes(1024)

        var numRequest = 1
        val mockEngine = MockEngine {
            if (numRequest++ == 1) respond("", OK, headers = headersOf(ContentLength, "2048"))
            else respond(secondBytes, PartialContent)
        }
        val httpManager = HttpManager(userAgent, null, httpClientEngine = mockEngine)
        val httpDownloader = HttpDownloader(httpManager, "foo/bar", file, mirrors)
        httpDownloader.download()

        assertContentEquals(firstBytes + secondBytes, file.readBytes())
    }

    @Test
    fun testNoETagNotTreatedAsNoChange() = runSuspend {
        val mockEngine = MockEngine { respondOk() }
        val httpManager = HttpManager(userAgent, null, httpClientEngine = mockEngine)
        val httpDownloader = HttpDownloader(httpManager, "foo/bar", folder.newFile(), mirrors)
        httpDownloader.cacheTag = null
        httpDownloader.download()

        assertEquals(2, mockEngine.requestHistory.size)
        val headRequest = mockEngine.requestHistory[0]
        val getRequest = mockEngine.requestHistory[1]
        assertEquals(Head, headRequest.method)
        assertEquals(Get, getRequest.method)
    }

    @Test
    fun testExpectedETagSkipsDownload() = runSuspend {
        val eTag = getRandomString()

        val mockEngine = MockEngine { respond("", OK, headers = headersOf(ETag, eTag)) }
        val httpManager = HttpManager(userAgent, null, httpClientEngine = mockEngine)
        val httpDownloader = HttpDownloader(httpManager, "foo/bar", folder.newFile(), mirrors)
        httpDownloader.cacheTag = eTag
        httpDownloader.download()

        assertEquals(eTag, httpDownloader.cacheTag)
        assertEquals(1, mockEngine.requestHistory.size)
        assertEquals(Head, mockEngine.requestHistory[0].method)
    }

    @Test
    @Ignore("We can not yet handle this scenario. See: #1708")
    fun testCalculatedETagSkipsDownload() = runSuspend {
        val eTag = "61de7e31-60a29a"
        val headers = buildHeaders {
            append(ETag, eTag)
            append(LastModified, "Wed, 12 Jan 2022 07:07:29 GMT")
            append(ContentLength, "6333082")
        }

        val mockEngine = MockEngine { respond("", OK, headers = headers) }
        val httpManager = HttpManager(userAgent, null, httpClientEngine = mockEngine)
        val httpDownloader = HttpDownloader(httpManager, "foo/bar", folder.newFile(), mirrors)
        // the ETag is calculated, but we expect a real ETag
        httpDownloader.cacheTag = "60a29a-5d55d390de574"
        httpDownloader.download()

        assertEquals(eTag, httpDownloader.cacheTag)
        assertEquals(1, mockEngine.requestHistory.size)
        assertEquals(Head, mockEngine.requestHistory[0].method)
    }


}
