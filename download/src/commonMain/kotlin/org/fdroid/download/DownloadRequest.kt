package org.fdroid.download

import kotlin.jvm.JvmOverloads

data class DownloadRequest @JvmOverloads constructor(
    val path: String,
    val mirrors: List<Mirror>,
    val username: String? = null,
    val password: String? = null,
    @Deprecated("One of the mirrors might be swap, we should check when/after selecting the mirror.")
    val isSwap: Boolean = false,
)
