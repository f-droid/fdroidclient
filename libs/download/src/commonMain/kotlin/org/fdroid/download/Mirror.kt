package org.fdroid.download

import io.ktor.http.URLBuilder
import io.ktor.http.URLParserException
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import mu.KotlinLogging

public data class Mirror @JvmOverloads constructor(
    val baseUrl: String,
    val location: String? = null,
    /**
     * If this is true, this as an IPFS HTTP gateway that only accepts CIDv1 and not regular paths.
     * So use this mirror only, if you have a CIDv1 available for supplying it to [getUrl].
     */
    val isIpfsGateway: Boolean = false,
) {
    public val url: Url by lazy {
        try {
            URLBuilder(baseUrl.trimEnd('/')).build()
            // we fall back to a non-existent URL if someone tries to sneak in an invalid mirror URL to crash us
            // to make it easier for potential callers
        } catch (e: URLParserException) {
            val log = KotlinLogging.logger {}
            log.warn { "Someone gave us an invalid URL: $baseUrl" }
            URLBuilder("http://127.0.0.1:64335").build()
        } catch (e: IllegalArgumentException) {
            val log = KotlinLogging.logger {}
            log.warn { "Someone gave us an invalid URL: $baseUrl" }
            URLBuilder("http://127.0.0.1:64335").build()
        }
    }

    public fun getUrl(path: String): Url {
        // Since Ktor 2.0 this adds double slash if not trimming slash from path
        return URLBuilder(url).appendPathSegments(path.trimStart('/')).build()
    }

    public fun isOnion(): Boolean = url.isOnion()

    public fun isLocal(): Boolean = url.isLocal()

    public fun isHttp(): Boolean = url.protocol.name.startsWith("http")

    public companion object {
        @JvmStatic
        public fun fromStrings(list: List<String>): List<Mirror> = list.map { Mirror(it) }
    }
}

internal fun Mirror?.isLocal(): Boolean = this?.isLocal() == true

internal fun Url.isOnion(): Boolean = host.endsWith(".onion")

/**
 * Returns true when no proxy should be used for connecting to this [Url].
 */
internal fun Url.isLocal(): Boolean {
    if (!host.matches(Regex("[0-9.]{7,15}"))) return false
    if (host.startsWith("172.")) {
        val second = host.substring(4..6)
        if (!second.endsWith('.')) return false
        val num = second.trimEnd('.').toIntOrNull() ?: return false
        return num in 16..31
    }
    return host.startsWith("169.254.") ||
        host.startsWith("10.") ||
        host.startsWith("192.168.") ||
        host == "127.0.0.1"
}
