package org.fdroid.download

import kotlin.jvm.JvmOverloads

data class DownloadRequest @JvmOverloads constructor(
    val url: String,
    val mirrors: List<String>,
    val username: String? = null,
    val password: String? = null,
    val isSwap: Boolean = false,
)
