package org.fdroid.ui.apps

import org.fdroid.database.AppListSortOrder

interface MyAppsInfo {
    val model: MyAppsModel
    fun refresh()
    fun changeSortOrder(sort: AppListSortOrder)
    fun search(query: String)
}

data class MyAppsModel(
    val appUpdates: List<AppUpdateItem>? = null,
    val installedApps: List<InstalledAppItem>? = null,
    val sortOrder: AppListSortOrder = AppListSortOrder.NAME,
)
