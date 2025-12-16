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
    val installingApps: List<InstallingAppItem>,
    val appUpdates: List<AppUpdateItem>? = null,
    val appsWithIssue: List<AppWithIssueItem>? = null,
    val installedApps: List<InstalledAppItem>? = null,
    val sortOrder: AppListSortOrder = AppListSortOrder.NAME,
    val networkState: NetworkState,
    val appUpdatesBytes: Long? = null,
)
