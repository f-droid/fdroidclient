package org.fdroid.ui.apps

import org.fdroid.download.DownloadRequest
import org.fdroid.index.v2.PackageVersion

data class AppUpdateItem(
    val packageName: String,
    val name: String,
    val installedVersionName: String,
    val update: PackageVersion,
    val whatsNew: String?,
    val iconDownloadRequest: DownloadRequest? = null,
)
