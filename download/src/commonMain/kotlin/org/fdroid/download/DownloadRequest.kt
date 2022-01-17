package org.fdroid.download

import io.ktor.client.engine.ProxyConfig
import kotlin.jvm.JvmOverloads

data class DownloadRequest @JvmOverloads constructor(
    val path: String,
    val mirrors: List<Mirror>,
    val proxy: ProxyConfig? = null,
    val username: String? = null,
    val password: String? = null,
)
