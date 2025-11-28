package org.fdroid.ui.apps

import org.fdroid.database.AppListSortOrder
import org.fdroid.install.InstallConfirmationState

interface MyAppsInfo {
    val model: MyAppsModel
    fun refresh()
    fun updateAll()
    fun changeSortOrder(sort: AppListSortOrder)
    fun search(query: String)
    fun confirmAppInstall(packageName: String, state: InstallConfirmationState)
}

data class MyAppsModel(
    val installingApps: List<InstallingAppItem>,
    val appUpdates: List<AppUpdateItem>? = null,
    val appsWithIssue: List<AppWithIssueItem>? = null,
    val installedApps: List<InstalledAppItem>? = null,
    val sortOrder: AppListSortOrder = AppListSortOrder.NAME,
)
