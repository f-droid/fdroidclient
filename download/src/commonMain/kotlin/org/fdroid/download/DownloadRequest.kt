package org.fdroid.download

import io.ktor.client.engine.ProxyConfig
import kotlin.jvm.JvmOverloads

public data class DownloadRequest @JvmOverloads constructor(
    val path: String,
    val mirrors: List<Mirror>,
    val proxy: ProxyConfig? = null,
    val username: String? = null,
    val password: String? = null,
    /**
     * Signals the [MirrorChooser] that this mirror should be tried before all other mirrors.
     * This could be useful for index updates for repositories with mirrors that update infrequently,
     * so that the official repository can be tried first to get updates fast.
     */
    val tryFirstMirror: Mirror? = null,
) {
    init {
        require(tryFirstMirror == null || mirrors.contains(tryFirstMirror)) {
            "$tryFirstMirror not in mirrors."
        }
    }

    val hasCredentials: Boolean = username != null && password != null
}
