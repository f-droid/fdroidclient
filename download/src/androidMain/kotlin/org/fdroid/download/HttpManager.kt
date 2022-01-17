package org.fdroid.download

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.Dns
import java.net.InetAddress

actual fun getHttpClientEngineFactory(): HttpClientEngineFactory<*> {
    return object : HttpClientEngineFactory<OkHttpConfig> {
        override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine = OkHttp.create {
            block()
            if (proxy.isTor()) { // don't allow DNS requests when using Tor
                config {
                    dns(NoDns())
                }
            }
        }
    }
}

/**
 * Prevent DNS requests.
 * Important when proxying all requests over Tor to not leak DNS queries.
 */
private class NoDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return listOf(InetAddress.getByAddress(hostname, ByteArray(4)))
    }
}
