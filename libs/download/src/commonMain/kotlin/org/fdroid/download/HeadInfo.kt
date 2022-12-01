package org.fdroid.download

public data class HeadInfo(
    val eTagChanged: Boolean,
    val eTag: String?,
    val contentLength: Long?,
    val lastModified: String?,
)
