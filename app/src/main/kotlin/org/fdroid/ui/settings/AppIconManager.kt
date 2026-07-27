package org.fdroid.ui.settings

import android.content.Context
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mu.KotlinLogging

class AppIconManager @Inject constructor(@ApplicationContext private val context: Context) {
  private val pm = context.packageManager
  private val log = KotlinLogging.logger {}

  private val _currentAppIcon = MutableStateFlow(getAppIconFromPackageManager())
  val currentAppIcon = _currentAppIcon.asStateFlow()

  fun setAppIcon(appIcon: AppIcon) {
    log.debug { "setAppIcon(${appIcon::class.simpleName})" }
    // enable new icon
    pm.setComponentEnabledSetting(
      appIcon.getComponentName(context),
      COMPONENT_ENABLED_STATE_ENABLED,
      DONT_KILL_APP,
    )
    log.debug { "  ${appIcon::class.simpleName} enabled" }
    // disable all other icons
    appIcons.forEach {
      if (it != appIcon) {
        pm.setComponentEnabledSetting(
          it.getComponentName(context),
          COMPONENT_ENABLED_STATE_DISABLED,
          DONT_KILL_APP,
        )
        log.debug { "  ${it::class.simpleName} disabled" }
      }
    }
    _currentAppIcon.update { appIcon }
  }

  private fun getAppIconFromPackageManager(): AppIcon {
    val activeIcon = appIcons.firstOrNull {
      val componentName = it.getComponentName(context)
      val componentEnabled = pm.getComponentEnabledSetting(componentName)

      log.debug { "Found $componentName with state of $componentEnabled" }
      if (it == AppIcon.Default && componentEnabled == COMPONENT_ENABLED_STATE_DEFAULT) {
        return it
      }
      componentEnabled == COMPONENT_ENABLED_STATE_ENABLED
    }

    return if (activeIcon == null) {
      setAppIcon(AppIcon.Default)
      AppIcon.Default
    } else {
      activeIcon
    }
  }
}
