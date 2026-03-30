package org.fdroid.ui.nearby

import org.fdroid.install.InstallState

data class SwapSuccessModel(
  val apps: List<SwapSuccessItem> = emptyList(),
  val loading: Boolean = true,
  val appToConfirm: SwapSuccessItem? = null,
)

data class SwapSuccessItem(
  val packageName: String,
  val name: String,
  val versionName: String,
  val versionCode: Long,
  val installedVersionName: String?,
  val installedVersionCode: Long?,
  val iconModel: Any?,
  val installState: InstallState = InstallState.Unknown,
) {
  val isInstalled: Boolean
    get() = installedVersionCode != null && installedVersionCode >= versionCode

  val hasUpdate: Boolean
    get() = installedVersionCode != null && installedVersionCode < versionCode
}
