package org.fdroid.ui.apps

import org.fdroid.database.AppListSortOrder
import org.fdroid.install.InstallState

interface MyAppsInfo {
    val model: MyAppsModel
    fun refresh()
    fun changeSortOrder(sort: AppListSortOrder)
    fun search(query: String)
    fun confirmAppInstall(packageName: String, state: InstallState.UserConfirmationNeeded)
}

data class MyAppsModel(
    val installingApps: List<InstallingAppItem>,
    val appUpdates: List<AppUpdateItem>? = null,
    val installedApps: List<InstalledAppItem>? = null,
    val sortOrder: AppListSortOrder = AppListSortOrder.NAME,
)
