package org.fdroid.basic.ui.main.lists

import org.fdroid.download.DownloadRequest

data class AppListItem(
    val packageName: String,
    val name: String,
    val summary: String,
    val lastUpdated: Long,
    val iconDownloadRequest: DownloadRequest?,
)
