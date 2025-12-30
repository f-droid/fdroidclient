package org.fdroid.ui.apps

import org.fdroid.database.AppListSortOrder
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallConfirmationState

interface MyAppsInfo {
    val model: MyAppsModel
    fun updateAll()
    fun changeSortOrder(sort: AppListSortOrder)
    fun search(query: String)
    fun confirmAppInstall(packageName: String, state: InstallConfirmationState)
    fun ignoreAppIssue(item: AppWithIssueItem)
}

data class MyAppsModel(
    val appToConfirm: InstallingAppItem? = null,
    val appUpdates: List<AppUpdateItem>? = null,
    val installingApps: List<InstallingAppItem>,
    val appsWithIssue: List<AppWithIssueItem>? = null,
    val installedApps: List<InstalledAppItem>? = null,
    val sortOrder: AppListSortOrder = AppListSortOrder.NAME,
    val networkState: NetworkState,
    val appUpdatesBytes: Long? = null,
)
