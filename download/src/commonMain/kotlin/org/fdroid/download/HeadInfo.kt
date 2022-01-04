package org.fdroid.download

data class HeadInfo(
    val eTagChanged: Boolean,
    val contentLength: Long?,
    val lastModified: String?,
)
