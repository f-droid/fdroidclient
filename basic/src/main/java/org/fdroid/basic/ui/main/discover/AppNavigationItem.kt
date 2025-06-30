package org.fdroid.basic.ui.main.discover

import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.download.DownloadRequest

class AppNavigationItem(
    override val packageName: String,
    override val name: String,
    val iconDownloadRequest: DownloadRequest? = null,
    val summary: String,
    val isNew: Boolean,
    val lastUpdated: Long = -1,
) : MinimalApp {
    override val icon: String? = null
}
