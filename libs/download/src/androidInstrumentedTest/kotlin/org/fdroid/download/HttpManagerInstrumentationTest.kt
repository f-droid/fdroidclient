package org.fdroid.download

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.ConnectionSpec
import okhttp3.ConnectionSpec.Companion.MODERN_TLS
import okhttp3.ConnectionSpec.Companion.RESTRICTED_TLS
import okhttp3.TlsVersion.TLS_1_2
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
        val mirror = Mirror("https://check.tls.support")
        val downloadRequest = DownloadRequest("/", listOf(mirror))

        val json = JSONObject(httpManager.getBytes(downloadRequest).decodeToString())
        assertEquals(userAgent, json.getString("user_agent"))
        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals("TLS 1.3", json.getString("tls_version"))
        } else {
            assertEquals("TLS 1.2", json.getString("tls_version"))
        }
        assertEquals(0, json.getJSONObject("weak_cipher_suites").length())
        assertEquals(0, json.getJSONObject("broken_cipher_suites").length())
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
        val mirror = Mirror("https://check.tls.support")
        val downloadRequest = DownloadRequest("/", listOf(mirror))

        val json = JSONObject(httpManager.getBytes(downloadRequest).decodeToString())
        assertEquals(userAgent, json.getString("user_agent"))
        assertEquals("TLS 1.2", json.getString("tls_version"))
        assertEquals(0, json.getJSONObject("weak_cipher_suites").length())
        assertEquals(0, json.getJSONObject("broken_cipher_suites").length())
    }
}
