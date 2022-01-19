package org.fdroid.download

import io.ktor.http.URLBuilder
import io.ktor.http.URLParserException
import io.ktor.http.Url
import io.ktor.http.pathComponents
import mu.KotlinLogging
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

data class Mirror @JvmOverloads constructor(
    private val baseUrl: String,
    val location: String? = null,
) {
    val url by lazy {
        try {
            URLBuilder(baseUrl).build()
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

    fun getUrl(path: String): Url {
        return URLBuilder(url).pathComponents(path).build()
    }

    fun isOnion(): Boolean = url.isOnion()

    fun isLocal(): Boolean = url.isLocal()

    companion object {
        @JvmStatic
        fun fromStrings(list: List<String>): List<Mirror> = list.map { Mirror(it) }
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
