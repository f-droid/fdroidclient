package org.fdroid.ui.lists

import org.fdroid.download.DownloadRequest

data class AppListItem(
    val repoId: Long,
    val packageName: String,
    val name: String,
    val summary: String,
    val lastUpdated: Long,
    val isCompatible: Boolean,
    val iconDownloadRequest: DownloadRequest? = null,
    val categoryIds: Set<String>? = null,
)
