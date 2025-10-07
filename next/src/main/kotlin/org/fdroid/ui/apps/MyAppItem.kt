package org.fdroid.ui.apps

import org.fdroid.download.DownloadRequest
import org.fdroid.index.v2.PackageVersion
import org.fdroid.install.InstallStateWithInfo

sealed class MyAppItem {
    abstract val packageName: String
    abstract val name: String
    abstract val lastUpdated: Long
    abstract val iconDownloadRequest: DownloadRequest?
}

data class InstallingAppItem(
    override val packageName: String,
    val installState: InstallStateWithInfo,
) : MyAppItem() {
    override val name: String = installState.name
    override val lastUpdated: Long = installState.lastUpdated
    override val iconDownloadRequest: DownloadRequest? = installState.iconDownloadRequest
}

data class AppUpdateItem(
    val repoId: Long,
    override val packageName: String,
    override val name: String,
    val installedVersionName: String,
    val update: PackageVersion,
    val whatsNew: String?,
    override val iconDownloadRequest: DownloadRequest? = null,
) : MyAppItem() {
    override val lastUpdated: Long = update.added
}

data class InstalledAppItem(
    override val packageName: String,
    override val name: String,
    val installedVersionName: String,
    override val lastUpdated: Long,
    override val iconDownloadRequest: DownloadRequest? = null,
) : MyAppItem()
