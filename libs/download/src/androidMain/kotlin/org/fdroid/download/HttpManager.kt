package org.fdroid.download

import android.util.Log
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.InputStream
import java.net.InetAddress
import java.security.DigestInputStream
import java.security.MessageDigest
import okhttp3.ConnectionSpec.Companion.CLEARTEXT
import okhttp3.ConnectionSpec.Companion.MODERN_TLS
import okhttp3.ConnectionSpec.Companion.RESTRICTED_TLS
import okhttp3.Dns
import okhttp3.internal.tls.OkHostnameVerifier

internal actual fun getHttpClientEngineFactory(customDns: Dns?): HttpClientEngineFactory<*> {
  return object : HttpClientEngineFactory<OkHttpConfig> {
    private val connectionSpecs =
      listOf(
        RESTRICTED_TLS, // order matters here, so we put restricted before modern
        MODERN_TLS,
        CLEARTEXT, // needed for swap connections, allowed in fdroidclient:app as well
      )

    override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine =
      OkHttp.create {
        block()
        config {
          if (proxy.isTor()) { // don't allow DNS requests when using Tor
            dns(NoDns())
          } else if (customDns != null) {
            dns(customDns)
          }
          hostnameVerifier { hostname, session ->
            try {
              session?.sessionContext?.sessionTimeout = 10
            } catch (e: NullPointerException) {
              // com.android.org.conscrypt.AbstractSessionContext.setSessionTimeout()
              // can throw this internally, so let's not crash due to this
              Log.e("HttpManager", "Error setting session timeout: ", e)
            }
            // use default hostname verifier
            OkHostnameVerifier.verify(hostname, session)
          }
          connectionSpecs(connectionSpecs)
        }
      }
  }
}

public suspend fun HttpManager.getInputStream(request: DownloadRequest): InputStream {
  return getChannel(request).toInputStream()
}

/**
 * Gets the [InputStream] for the given [request] as a [DigestInputStream], so you can verify the
 * SHA-256 hash. If you don't need to verify the hash, use [getInputStream] instead.
 */
public suspend fun HttpManager.getDigestInputStream(request: DownloadRequest): DigestInputStream {
  val digest = MessageDigest.getInstance("SHA-256")
  val inputStream = getChannel(request).toInputStream()
  return DigestInputStream(inputStream, digest)
}

/** Prevent DNS requests. Important when proxying all requests over Tor to not leak DNS queries. */
private class NoDns : Dns {
  override fun lookup(hostname: String): List<InetAddress> {
    return listOf(InetAddress.getByAddress(hostname, ByteArray(4)))
  }
}
