package org.fdroid.basic.ui.main.discover

import org.fdroid.download.DownloadRequest

class AppDiscoverItem(
    val packageName: String,
    val name: String,
    val iconDownloadRequest: DownloadRequest? = null,
    val isNew: Boolean,
    val lastUpdated: Long = -1,
)
