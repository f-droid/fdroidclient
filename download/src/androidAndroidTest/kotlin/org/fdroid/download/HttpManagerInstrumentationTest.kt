package org.fdroid.download

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.getRandomString
import org.fdroid.runSuspend
import org.json.JSONObject
import org.junit.runner.RunWith
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Suppress("BlockingMethodInNonBlockingContext")
internal class HttpManagerInstrumentationTest {

    private val userAgent = getRandomString()

    @Test(expected = SSLHandshakeException::class)
    fun testNoTls10() = runSuspend {
        val httpManager = HttpManager(userAgent, null)
        val mirror = Mirror("https://tls-v1-0.badssl.com:1010")
        val downloadRequest = DownloadRequest("/", listOf(mirror))

        httpManager.getBytes(downloadRequest)
    }

    @Test(expected = SSLHandshakeException::class)
    fun testNoTls11() = runSuspend {
        val httpManager = HttpManager(userAgent, null)
        val mirror = Mirror("https://tls-v1-1.badssl.com:1010")
        val downloadRequest = DownloadRequest("/", listOf(mirror))

        httpManager.getBytes(downloadRequest)
    }

    @Test
    fun checkTlsSupport() = runSuspend {
        val httpManager = HttpManager(userAgent, null)
        val mirror = Mirror("https://check.tls.support")
        val downloadRequest = DownloadRequest("/", listOf(mirror))

        val json = JSONObject(httpManager.getBytes(downloadRequest).decodeToString())
        assertEquals(userAgent, json.getString("user_agent"))
        assertEquals("TLS 1.3", json.getString("tls_version"))
        assertEquals(0, json.getJSONObject("broken_cipher_suites").length())
    }
}
