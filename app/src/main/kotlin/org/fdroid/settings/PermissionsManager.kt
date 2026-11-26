package org.fdroid.settings

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionsManager
@Inject
constructor(@param:ApplicationContext private val context: Context) {

  val permissionState: PermissionState
    get() =
      PermissionState(
        canRequestPackageInstalls = canRequestPackageInstalls,
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
        hasNotificationPermission = hasNotificationPermission,
        requestPackageInstallsIntent = requestPackageInstallsIntent,
        requestIgnoreBatteryOptimizationsIntent = requestIgnoreBatteryOptimizationsIntent,
      )

  val requestPackageInstallsIntent: Intent =
    if (SDK_INT >= 26) {
      Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        "package:${context.packageName}".toUri(),
      )
    } else {
      Intent(Settings.ACTION_SECURITY_SETTINGS)
    }

  @SuppressLint("BatteryLife")
  val requestIgnoreBatteryOptimizationsIntent: Intent =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
      data = "package:${context.packageName}".toUri()
    }

  private val canRequestPackageInstalls: Boolean
    get() {
      return if (SDK_INT >= 26) {
        context.packageManager.canRequestPackageInstalls()
      } else {
        // old API for < SDK 26
        @Suppress("DEPRECATION")
        Settings.Secure.getInt(
          context.contentResolver,
          Settings.Secure.INSTALL_NON_MARKET_APPS,
          0,
        ) == 1
      }
    }

  private val isIgnoringBatteryOptimizations: Boolean
    get() {
      val powerManager = context.getSystemService(PowerManager::class.java)
      return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

  private val hasNotificationPermission: Boolean
    get() =
      SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED
}

data class PermissionState(
  val canRequestPackageInstalls: Boolean,
  val isIgnoringBatteryOptimizations: Boolean,
  val hasNotificationPermission: Boolean,
  val requestPackageInstallsIntent: Intent,
  val requestIgnoreBatteryOptimizationsIntent: Intent,
) {
  val acceptable: Boolean = canRequestPackageInstalls && isIgnoringBatteryOptimizations
}
