package org.fdroid.download

import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.ProxyType.HTTP
import io.ktor.client.engine.ProxyType.SOCKS
import io.ktor.client.engine.resolveAddress
import io.ktor.client.engine.type
import io.ktor.http.hostIsIp
import io.ktor.util.network.port

private const val DEFAULT_PROXY_HOST = "127.0.0.1"
private const val DEFAULT_PROXY_HTTP_PORT = 8118
private const val DEFAULT_PROXY_SOCKS_PORT = 9050

internal fun ProxyConfig?.isTor(): Boolean {
    if (this == null || !hostIsIp(DEFAULT_PROXY_HOST)) return false
    val address = resolveAddress()
    return (type == HTTP && address.port == DEFAULT_PROXY_HTTP_PORT) ||
        (type == SOCKS && address.port == DEFAULT_PROXY_SOCKS_PORT)
}
