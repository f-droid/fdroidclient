package org.fdroid.ui.apps

import org.fdroid.database.AppListSortOrder
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallConfirmationState

interface MyAppsInfo {
  val model: MyAppsModel
  val actions: MyAppsActions
}

data class MyAppsModel(
  val appToConfirm: InstallingAppItem? = null,
  val appUpdates: List<AppUpdateItem>? = null,
  val installingApps: List<InstallingAppItem>,
  val appsWithIssue: List<AppWithIssueItem>? = null,
  val installedApps: List<InstalledAppItem>? = null,
  val showUpdatesHint: Boolean,
  val showAppIssueHint: Boolean,
  val sortOrder: AppListSortOrder = AppListSortOrder.NAME,
  val networkState: NetworkState,
  val isSearching: Boolean = false,
  val appUpdatesBytes: Long? = null,
) {
  val showClearInstallingAppsButton: Boolean = installingApps.all { !it.installState.showProgress }
}

interface MyAppsActions {
  fun updateAll()

  fun changeSortOrder(sort: AppListSortOrder)

  fun search(query: String)

  fun confirmAppInstall(packageName: String, state: InstallConfirmationState)

  fun clearInstallingApps()

  fun ignoreAppIssue(item: AppWithIssueItem)

  fun onUpdatesHintSeen()

  fun onAppIssueHintSeen()

  fun onNotWarnWhenMetered()

  fun exportInstalledApps()
}
