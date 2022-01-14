package org.fdroid.download

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.pathComponents
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

data class Mirror @JvmOverloads constructor(
    val baseUrl: String,
    val location: String? = null,
) {
    fun getUrl(path: String): Url {
        return URLBuilder(baseUrl).pathComponents(path).build()
    }

    companion object {
        @JvmStatic
        fun fromStrings(list: List<String>): List<Mirror> = list.map { Mirror(it) }
    }
}

fun Url.isLocal(): Boolean {
    return (port > 1023 // only root can use <= 1023, so never a swap repo
            && host.matches(Regex("[0-9.]+")) // host must be an IP address
            // TODO check if IP is link or site local
            )
}
