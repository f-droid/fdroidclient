package org.fdroid.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import dagger.hilt.android.qualifiers.ApplicationContext
import info.guardianproject.panic.Panic.PACKAGE_NAME_NONE
import info.guardianproject.panic.PanicResponder.PREF_TRIGGER_PACKAGE_NAME
import javax.inject.Inject
import javax.inject.Singleton
import org.fdroid.settings.SettingsManager

/**
 * This class is used to check if the user has used the legacy hiding feature and if so, it will
 * switch to the new calculator icon and disable the legacy hiding feature.
 *
 * After sufficient time has passed, this class can be removed.
 */
@Singleton
class LegacyOnboardingManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val settingsManager: SettingsManager,
  private val appIconManager: AppIconManager,
) {
  val showAppHidingOnboarding: Boolean
    get() {
      if (isHiddenLegacy()) {
        // switch to new calculator icon
        appIconManager.setAppIcon(AppIcon.Calculator)
        // disable legacy hiding
        context.packageManager.setComponentEnabledSetting(
          component,
          COMPONENT_ENABLED_STATE_DISABLED,
          DONT_KILL_APP,
        )
        return true
      }
      return hadHidingSettingsEnabled()
    }

  // the old component used to hide F-Droid as a calculator
  private val component = ComponentName(context, "org.fdroid.fdroid.panic.CalculatorActivity")

  private fun hadHidingSettingsEnabled(): Boolean {
    // check if long press on search to hide app was active
    if (settingsManager.prefs.getBoolean("hideOnLongPressSearch", false)) {
      return true
    }
    // check if we had a connected panic app and hiding active
    if (settingsManager.prefs.getBoolean("pref_panic_hide", false)) {
      settingsManager.prefs.getString(PREF_TRIGGER_PACKAGE_NAME, null)?.let { triggerPackageName ->
        if (triggerPackageName != PACKAGE_NAME_NONE) return true
      }
    }
    return false
  }

  private fun isHiddenLegacy(): Boolean {
    val componentEnabled = context.packageManager.getComponentEnabledSetting(component)
    return componentEnabled == COMPONENT_ENABLED_STATE_ENABLED
  }
}
