package org.fdroid.download

import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.config
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.utils.buildHeaders
import io.ktor.http.HttpHeaders.ContentLength
import io.ktor.http.HttpHeaders.ETag
import io.ktor.http.HttpHeaders.LastModified
import io.ktor.http.HttpHeaders.Range
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.PartialContent
import io.ktor.http.headersOf
import io.ktor.utils.io.core.internal.ChunkBuffer
import io.ktor.utils.io.core.writeFully
import org.fdroid.TestByteReadChannel
import org.fdroid.get
import org.fdroid.getIndexFile
import org.fdroid.getRandomString
import org.fdroid.runSuspend
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

private const val TOR_SOCKS_PORT = 9050

@Suppress("BlockingMethodInNonBlockingContext", "DEPRECATION")
internal class HttpDownloaderTest {

    @get:Rule
    var folder = TemporaryFolder()

    private val userAgent = getRandomString()
    private val mirror1 = Mirror("http://example.org")
    private val mirrors = listOf(mirror1)
    private val downloadRequest = DownloadRequest(getIndexFile("foo/bar"), mirrors)

    @Test
    fun testDownload() = runSuspend {
        val file = folder.newFile()
        val bytes = Random.nextBytes(1024)

        val mockEngine = MockEngine { respond(bytes) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, file)
        httpDownloader.download()

        assertContentEquals(bytes, file.readBytes())
    }

    @Test
    fun testDownloadWithCorrectHash() = runSuspend {
        val file = folder.newFile()
        val bytes = "We know the hash for this string".encodeToByteArray()
        val indexFile = getIndexFile(
            name = "/foo/bar",
            sha256 = "e3802e5f8ae3dc7bbf5f1f4f7fb825d9bce9d1ddce50ac564fcbcfdeb31f1b90",
            size = bytes.size.toLong(),
        )
        val downloadRequest = DownloadRequest(indexFile, mirrors = mirrors)
        var progressReported = false

        val mockEngine = MockEngine { respond(bytes) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, file)
        httpDownloader.setListener { _, totalBytes ->
            assertEquals(bytes.size.toLong(), totalBytes)
            progressReported = true
        }
        httpDownloader.download()

        assertContentEquals(bytes, file.readBytes())
        assertTrue(progressReported)
    }

    @Test(expected = IOException::class)
    fun testDownloadWithWrongHash() = runSuspend {
        val file = folder.newFile()
        val bytes = "We know the hash for this string".encodeToByteArray()
        val indexFile = getIndexFile(
            name = "/foo/bar",
            sha256 = "This is not the right hash",
            size = bytes.size.toLong(),
        )
        val downloadRequest = DownloadRequest(indexFile, mirrors = mirrors)

        val mockEngine = MockEngine { respond(bytes) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, file)
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
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, file)
        httpDownloader.download()

        assertContentEquals(firstBytes + secondBytes, file.readBytes())
        assertEquals(2, mockEngine.responseHistory.size)
    }

    /**
     * Tests that a failed download in one mirror will be automatically resumed
     * with the next mirror and then restarted if that mirror doesn't support [PartialContent].
     */
    @Test
    fun testMirrorNoResume() = runSuspend {
        // we need at least two mirrors
        val mirror2 = Mirror("http://example.net")
        val mirrors = listOf(mirror1, mirror2)
        val downloadRequest = DownloadRequest(getIndexFile("foo/bar"), mirrors)

        val file = folder.newFile()
        val firstBytes = Random.nextBytes(DEFAULT_BUFFER_SIZE)
        val secondBytes = Random.nextBytes(1024)
        val totalSize = firstBytes.size + secondBytes.size
        val readChannel = object : TestByteReadChannel() {
            var wasRead = 0
            override val availableForRead: Int = DEFAULT_BUFFER_SIZE / 2
            override suspend fun readAvailable(dst: ChunkBuffer): Int {
                // We allow three reads. Only the first two give us the firstBytes.
                // While the third seems to be required for throwing an exception,
                // it isn't filling the buffer when we finally throw,
                // so it isn't considered as transferred bytes.
                if (wasRead == 3) throw SocketTimeoutException("boom!")
                dst.writeFully(
                    source = firstBytes + Random.nextBytes(availableForRead),
                    offset = wasRead * availableForRead,
                    length = availableForRead,
                )
                wasRead++
                return availableForRead
            }
        }

        val mockEngine = MockEngine.config {
            reuseHandlers = false
            // first response reads from channel that errors after sending firstBytes
            addHandler {
                respond(readChannel, OK, headers = headersOf(ContentLength, "$totalSize"))
            }
            // second request tries to resume, but doesn't get PartialContent response
            addHandler {
                assertEquals("bytes=$DEFAULT_BUFFER_SIZE-", it.headers[Range])
                respond(
                    content = firstBytes + secondBytes,
                    status = OK,
                    headers = headersOf(ContentLength, "$totalSize"),
                )
            }
            // download is tried again without resuming
            addHandler {
                respond(
                    content = firstBytes + secondBytes,
                    status = OK,
                    headers = headersOf(ContentLength, "$totalSize"),
                )
            }
        }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = mockEngine)
        val httpDownloader = HttpDownloaderV2(httpManager, downloadRequest, file)
        httpDownloader.download()

        assertContentEquals(firstBytes + secondBytes, file.readBytes())
    }

    /**
     * Tests resuming a download with hash verification.
     * This can fail if the hashing doesn't take the already downloaded bytes into account.
     */
    @Test
    fun testResumeWithHashSuccess() = runSuspend {
        val file = folder.newFile()
        val firstBytes =
            "These are the first bytes that were already downloaded.".encodeToByteArray()
        file.writeBytes(firstBytes)
        val secondBytes =
            "These are the last bytes that still need to be downloaded.".encodeToByteArray()
        val totalSize = firstBytes.size + secondBytes.size
        // specifying the sha256 hash forces its validation
        val sha256 = "efabb260da949061c88173c19f369b4aa0eaa82003c7c2dec08b5dfe75525368"
        val downloadRequest = DownloadRequest(getIndexFile("foo/bar", sha256), mirrors)

        val mockEngine = MockEngine.config {
            reuseHandlers = false
            addHandler {
                respond("", OK, headers = headersOf(ContentLength, "$totalSize"))
            }
            addHandler {
                respond(secondBytes, PartialContent)
            }
        }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = mockEngine)
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, file)
        // this throws if the hash doesn't match while downloading
        httpDownloader.download()

        assertContentEquals(firstBytes + secondBytes, file.readBytes())
    }

    @Test
    fun testResumeError() = runSuspend {
        val file = folder.newFile()
        val firstBytes = Random.nextBytes(1024)
        file.writeBytes(firstBytes)
        val secondBytes = Random.nextBytes(1024)
        val allBytes = firstBytes + secondBytes

        var numRequest = 1
        val mockEngine = MockEngine {
            when (numRequest++) {
                1 -> respond("", OK, headers = headersOf(ContentLength, "2048"))
                2 -> respond(allBytes, OK) // not replying with PartialContent
                3 -> respond(allBytes, OK)
                else -> fail("Unexpected additional request")
            }
        }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, file)
        httpDownloader.download()

        assertContentEquals(allBytes, file.readBytes())
        assertEquals(3, mockEngine.responseHistory.size)
    }

    @Test
    fun testNoETagNotTreatedAsNoChange() = runSuspend {
        val mockEngine = MockEngine { respondOk() }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, folder.newFile())
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
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, folder.newFile())
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
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, folder.newFile())
        // the ETag is calculated, but we expect a real ETag
        httpDownloader.cacheTag = "60a29a-5d55d390de574"
        httpDownloader.download()

        assertEquals(eTag, httpDownloader.cacheTag)
        assertEquals(1, mockEngine.requestHistory.size)
        assertEquals(Head, mockEngine.requestHistory[0].method)
    }

    @Test
    fun testTorProxy() = runSuspend {
        assumeTrue(isTorRunning())

        val file = folder.newFile()

        val httpManager = HttpManager(userAgent, null)
        // tor-project.org
        val torHost = "http://2gzyxa5ihm7nsggfxnu52rck2vv4rvmdlkiu3zzui5du4xyclen53wid.onion"
        val proxy = ProxyBuilder.socks("localhost", TOR_SOCKS_PORT)
        val downloadRequest = DownloadRequest("index.html", listOf(Mirror(torHost)), proxy)
        val httpDownloader = HttpDownloader(httpManager, downloadRequest, file)
        httpDownloader.download()

        assertTrue { file.length() > 1024 }
    }

    private fun isTorRunning(): Boolean = try {
        ServerSocket(TOR_SOCKS_PORT)
        false
    } catch (e: BindException) {
        true
    }

}
