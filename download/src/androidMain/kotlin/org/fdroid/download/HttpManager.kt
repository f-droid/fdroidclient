package org.fdroid.download

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.ConnectionSpec.Companion.CLEARTEXT
import okhttp3.ConnectionSpec.Companion.MODERN_TLS
import okhttp3.ConnectionSpec.Companion.RESTRICTED_TLS
import okhttp3.Dns
import okhttp3.internal.tls.OkHostnameVerifier
import java.net.InetAddress

internal actual fun getHttpClientEngineFactory(): HttpClientEngineFactory<*> {
    return object : HttpClientEngineFactory<OkHttpConfig> {
        private val connectionSpecs = listOf(
            RESTRICTED_TLS, // order matters here, so we put restricted before modern
            MODERN_TLS,
            CLEARTEXT, // needed for swap connections, allowed in fdroidclient:app as well
        )

        override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine = OkHttp.create {
            block()
            config {
                if (proxy.isTor()) { // don't allow DNS requests when using Tor
                    dns(NoDns())
                }
                hostnameVerifier { hostname, session ->
                    session?.sessionContext?.sessionTimeout = 60
                    // use default hostname verifier
                    OkHostnameVerifier.verify(hostname, session)
                }
                connectionSpecs(connectionSpecs)
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
