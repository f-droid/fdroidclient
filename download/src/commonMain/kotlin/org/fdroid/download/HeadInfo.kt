package org.fdroid.download

data class HeadInfo(
    val eTagChanged: Boolean,
    val eTag: String?,
    val contentLength: Long?,
    val lastModified: String?,
)
