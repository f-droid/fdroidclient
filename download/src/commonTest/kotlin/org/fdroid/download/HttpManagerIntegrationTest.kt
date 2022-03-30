package org.fdroid.download

import io.ktor.client.engine.ProxyBuilder
import io.ktor.http.Url
import io.ktor.utils.io.errors.IOException
import org.fdroid.getRandomString
import org.fdroid.runSuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpManagerIntegrationTest {

    private val userAgent = getRandomString()
    private val mirrors = listOf(Mirror("https://example.org"), Mirror("https://example.net/"))
    private val downloadRequest = DownloadRequest("", mirrors)

    @Test
    fun testResumeOnExample() = runSuspend {
        val httpManager = HttpManager(userAgent, null)

        val lastLine = httpManager.getBytes(downloadRequest, 1248).decodeToString()
        assertEquals("</html>\n", lastLine)
    }

    @Test
    fun testProxy() = runSuspend {
        val proxyRequest = downloadRequest.copy(proxy = ProxyBuilder.http(Url("http://127.0.0.1")))
        val httpManager = HttpManager(userAgent, null)

        val e = assertFailsWith<IOException> {
            httpManager.getBytes(proxyRequest)
        }
        assertEquals("Failed to connect to /127.0.0.1:80", e.message)

        val lastLine = httpManager.getBytes(downloadRequest, 1248).decodeToString()
        assertEquals("</html>\n", lastLine)
    }
}
