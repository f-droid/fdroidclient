package org.fdroid.ui.panic

import org.fdroid.download.PackageName

data class PanicSettingsState(
  val panicApps: List<PanicApp?> = listOf(null),
  val selectedPanicApp: PanicApp? = null,
) {
  val actionsEnabled: Boolean = selectedPanicApp != null
}

data class PanicApp(val packageName: String, val name: String) {
  val iconModel = PackageName(packageName, null, true)
}
