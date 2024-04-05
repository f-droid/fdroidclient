package org.fdroid.download

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import kotlinx.coroutines.delay
import okhttp3.ConnectionSpec
import okhttp3.ConnectionSpec.Companion.MODERN_TLS
import okhttp3.ConnectionSpec.Companion.RESTRICTED_TLS
import okhttp3.TlsVersion.TLS_1_2
import org.fdroid.IndexFile
import org.fdroid.getIndexFile
import org.fdroid.getRandomString
import org.fdroid.runSuspend
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
@Suppress("BlockingMethodInNonBlockingContext")
internal class HttpManagerInstrumentationTest {

    private val userAgent = getRandomString()

    @Test
    fun testCleartext() = runSuspend {
        val httpManager = HttpManager(userAgent, null)
        val mirror = Mirror("http://neverssl.com")
        val downloadRequest = DownloadRequest("/", listOf(mirror))

        httpManager.getBytes(downloadRequest)
    }

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
        val mirror = Mirror("https://www.howsmyssl.com")
        val indexFile: IndexFile = getIndexFile("/a/check")
        val downloadRequest = DownloadRequest(indexFile, listOf(mirror))

        val json = JSONObject(httpManager.getBytes(downloadRequest).decodeToString())
        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals("TLS 1.3", json.getString("tls_version"))
        } else {
            assertEquals("TLS 1.2", json.getString("tls_version"))
        }
        assertEquals(0, json.getJSONObject("insecure_cipher_suites").length())
    }

    @Test
    fun checkTls12Support() = runSuspend {
        val clientFactory = object : HttpClientEngineFactory<OkHttpConfig> {
            override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine = OkHttp.create {
                block()
                config {
                    val restricted12 = ConnectionSpec.Builder(RESTRICTED_TLS)
                        .tlsVersions(TLS_1_2)
                        .build()
                    val modern12 = ConnectionSpec.Builder(MODERN_TLS)
                        .tlsVersions(TLS_1_2)
                        .build()
                    connectionSpecs(listOf(restricted12, modern12))
                }
            }
        }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = clientFactory)
        val mirror = Mirror("https://www.howsmyssl.com")
        val indexFile: IndexFile = getIndexFile("/a/check")
        val downloadRequest = DownloadRequest(indexFile, listOf(mirror))

        val json = JSONObject(httpManager.getBytes(downloadRequest).decodeToString())
        assertEquals("TLS 1.2", json.getString("tls_version"))
        assertEquals(0, json.getJSONObject("insecure_cipher_suites").length())
    }

    @Test
    fun checkSessionResumeShort() = runSuspend {
        assumeTrue(
            "tlsprivacy.nervuri.net uses Let's Encrypt, which does not work on old Androids",
            Build.VERSION.SDK_INT >= 26
        )
        val httpManager = HttpManager(userAgent, null)
        val mirror = Mirror("https://tlsprivacy.nervuri.net")
        val indexFile: IndexFile = getIndexFile("/json/v1")
        val downloadRequest = DownloadRequest(indexFile, listOf(mirror))

        // first request had no session to resume
        JSONObject(httpManager.getBytes(downloadRequest).decodeToString()).let { json ->
            val connectionInfo = json.getJSONObject("connection_info")
            assertFalse(connectionInfo.getBoolean("session_resumed"))
        }
        // second request right after resumed session
        JSONObject(httpManager.getBytes(downloadRequest).decodeToString()).let { json ->
            val connectionInfo = json.getJSONObject("connection_info")
            assumeTrue(
                "Session was not resumed at all",
                connectionInfo.getBoolean("session_resumed")
            )
        }
        delay(10_100)
        // third request after 10s did not resume session
        JSONObject(httpManager.getBytes(downloadRequest).decodeToString()).let { json ->
            val connectionInfo = json.getJSONObject("connection_info")
            assertFalse(connectionInfo.getBoolean("session_resumed"))
        }
    }
}
