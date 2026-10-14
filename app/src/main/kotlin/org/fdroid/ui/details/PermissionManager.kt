package org.fdroid.ui.details

import android.content.Context
import android.content.pm.PermissionInfo.PROTECTION_MASK_BASE
import android.content.pm.PermissionInfo.PROTECTION_NORMAL
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.fdroid.R
import org.fdroid.database.AppVersion
import org.fdroid.index.v2.PermissionV2

@Singleton
class PermissionManager(
  @param:ApplicationContext private val context: Context,
  private val sdk: Int = SDK_INT,
) {

  @Inject constructor(@ApplicationContext context: Context) : this(context, SDK_INT)

  private val pm = context.packageManager

  fun getPermissions(version: AppVersion): List<Permission> {
    return getPermissions(version.usesPermission) + getPermissions(version.usesPermissionSdk23)
  }

  private fun getPermissions(permissionsV2: List<PermissionV2>): List<Permission> {
    return permissionsV2.mapNotNull { perm ->
      if ((perm.maxSdkVersion ?: Int.MAX_VALUE) < sdk) return@mapNotNull null
      try {
        val permInfo = pm.getPermissionInfo(perm.name, 0)
        Permission(
          name = perm.name,
          label = permInfo.loadLabel(pm).toString(),
          description = permInfo.loadDescription(pm)?.toString(),
          type =
            @Suppress("DEPRECATION") // protectionLevel will get replaced when minSdk allows it
            when (permInfo.protectionLevel and PROTECTION_MASK_BASE) {
              PROTECTION_NORMAL if perm.name in sensitivePermissions -> PermissionType.SENSITIVE
              PROTECTION_NORMAL -> PermissionType.NORMAL
              else -> PermissionType.RUNTIME
            },
        )
      } catch (_: Exception) {
        Permission(
          name = perm.name,
          label = null,
          description = null,
          type = PermissionType.CUSTOM,
        )
      }
    }
  }
}

data class Permission(
  val name: String,
  val label: String?,
  val description: String?,
  val type: PermissionType,
)

enum class PermissionType(@StringRes val titleRes: Int) {
  SENSITIVE(R.string.app_details_permissions_title_sensitive),
  NORMAL(R.string.app_details_permissions_title_normal),
  RUNTIME(R.string.app_details_permissions_title_runtime),
  CUSTOM(R.string.app_details_permissions_title_custom),
}

/** A set of permissions that are considered sensitive and don't require user approval. */
private val sensitivePermissions =
  setOf(
    "android.permission.ACCESS_HIDDEN_PROFILES",
    "android.permission.ACTIVITY_RECOGNITION",
    "android.permission.CHANGE_WIFI_STATE",
    "android.permission.DISABLE_KEYGUARD",
    "android.permission.EXPAND_STATUS_BAR",
    "android.permission.HIGH_SAMPLING_RATE_SENSORS",
    "android.permission.INTERNET",
    "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS",
    "android.permission.MODIFY_AUDIO_SETTINGS",
    "android.permission.MODIFY_PHONE_STATE",
    "android.permission.NFC",
    "android.permission.QUERY_ADVANCED_PROTECTION_MODE",
    "android.permission.QUERY_ALL_PACKAGES",
    "android.permission.READ_INSTALL_SESSIONS",
    "android.permission.READ_SYNC_SETTINGS",
    "android.permission.READ_USER_DICTIONARY",
    "android.permission.SET_WALLPAPER",
    "android.permission.TRANSMIT_IR",
    "android.permission.WRITE_USER_DICTIONARY",
  )
