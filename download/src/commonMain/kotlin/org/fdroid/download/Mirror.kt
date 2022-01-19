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

    companion object {
        @JvmStatic
        fun fromStrings(list: List<String>): List<Mirror> = list.map { Mirror(it) }
    }
}

internal fun Url.isOnion(): Boolean = host.endsWith(".onion")

fun Url.isLocal(): Boolean {
    return (port > 1023 // only root can use <= 1023, so never a swap repo
            && host.matches(Regex("[0-9.]+")) // host must be an IP address
            // TODO check if IP is link or site local
            )
}
