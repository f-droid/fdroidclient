package org.fdroid.download

import io.ktor.client.engine.ProxyConfig
import org.fdroid.IndexFile

public data class DownloadRequest @JvmOverloads constructor(
    val indexFile: IndexFile,
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
    @JvmOverloads
    @Deprecated("Use other constructor instead")
    public constructor(
        path: String,
        mirrors: List<Mirror>,
        proxy: ProxyConfig? = null,
        username: String? = null,
        password: String? = null,
        tryFirstMirror: Mirror? = null,
    ) : this(object : IndexFile {
        override val name = path
        override val sha256: String? = null
        override val size = 0L
        override val ipfsCidV1: String? = null
        override fun serialize(): String {
            throw NotImplementedError("Serialization is not implemented.")
        }
    }, mirrors, proxy, username, password, tryFirstMirror)

    val hasCredentials: Boolean = username != null && password != null
}
