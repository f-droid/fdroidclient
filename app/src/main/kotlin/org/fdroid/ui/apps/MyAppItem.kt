package org.fdroid.ui.apps

import org.fdroid.database.AppIssue
import org.fdroid.download.DownloadRequest
import org.fdroid.download.PackageName
import org.fdroid.index.v2.PackageVersion
import org.fdroid.install.InstallStateWithInfo

sealed class MyAppItem {
  abstract val packageName: String
  abstract val name: String
  abstract val lastUpdated: Long
  abstract val iconModel: Any?
}

data class InstallingAppItem(
  override val packageName: String,
  val installState: InstallStateWithInfo,
) : MyAppItem() {
  override val name: String = installState.name
  override val lastUpdated: Long = installState.lastUpdated
  override val iconModel: Any = PackageName(packageName, installState.iconModel as? DownloadRequest)
}

data class AppUpdateItem(
  val repoId: Long,
  override val packageName: String,
  override val name: String,
  val installedVersionName: String,
  val update: PackageVersion,
  val whatsNew: String?,
  override val iconModel: Any? = null,
) : MyAppItem() {
  override val lastUpdated: Long = update.added
}

data class AppWithIssueItem(
  override val packageName: String,
  override val name: String,
  override val installedVersionName: String,
  val installedVersionCode: Long,
  val issue: AppIssue,
  override val lastUpdated: Long,
  override val iconModel: Any? = null,
) : MyInstalledAppItem()

data class InstalledAppItem(
  override val packageName: String,
  override val name: String,
  override val installedVersionName: String,
  val installedVersionCode: Long,
  override val lastUpdated: Long,
  override val iconModel: Any? = null,
) : MyInstalledAppItem()

abstract class MyInstalledAppItem : MyAppItem() {
  abstract val installedVersionName: String
}
