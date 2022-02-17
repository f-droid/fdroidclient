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
     *
     * If this mirror is not in [mirrors], e.g. when the user has disabled it,
     * then setting this has no effect.
     */
    val tryFirstMirror: Mirror? = null,
) {
    val hasCredentials: Boolean = username != null && password != null
}
