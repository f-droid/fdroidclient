package org.fdroid.settings

import androidx.annotation.StringRes
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.settings.SettingsConstants.AutoUpdateValues
import org.fdroid.settings.SettingsConstants.MirrorChooserValues

object SettingsConstants {

  const val PREF_KEY_LAST_UPDATE_CHECK = "lastUpdateCheck"
  const val PREF_DEFAULT_LAST_UPDATE_CHECK = -1L

  const val PREF_KEY_THEME = "theme"
  const val PREF_DEFAULT_THEME = "followSystem"

  const val PREF_KEY_DYNAMIC_COLORS = "dynamicColors"
  const val PREF_DEFAULT_DYNAMIC_COLORS = false

  enum class AutoUpdateValues {
    OnlyWifi,
    Always,
    Never,
  }

  const val PREF_KEY_REPO_UPDATES = "repoAutoUpdates"
  val PREF_DEFAULT_REPO_UPDATES = AutoUpdateValues.OnlyWifi.name

  const val PREF_KEY_AUTO_UPDATES = "appAutoUpdates"
  val PREF_DEFAULT_AUTO_UPDATES = AutoUpdateValues.OnlyWifi.name

  enum class MirrorChooserValues {
    Random {
      override val res = R.string.pref_mirror_chooser_summary_random
    },
    PreferForeign {
      override val res = R.string.pref_mirror_chooser_summary_prefer_foreign
    };

    @get:StringRes abstract val res: Int
  }

  const val PREF_KEY_MIRROR_CHOOSER = "mirrorChooser"
  val PREF_DEFAULT_MIRROR_CHOOSER = MirrorChooserValues.Random.name

  const val PREF_KEY_PROXY = "proxy"
  const val PREF_DEFAULT_PROXY = ""

  const val PREF_USE_DNS_CACHE = "useDnsCache"
  const val PREF_USE_DNS_CACHE_DEFAULT = false

  const val PREF_DNS_CACHE = "dnsCache"
  const val PREF_DNS_CACHE_DEFAULT = ""

  const val PREF_KEY_PREVENT_SCREENSHOTS = "preventScreenshots"
  const val PREF_DEFAULT_PREVENT_SCREENSHOTS = false

  const val PREF_KEY_SHOW_INCOMPATIBLE = "incompatibleVersions"
  const val PREF_DEFAULT_SHOW_INCOMPATIBLE = true

  const val PREF_KEY_APP_LIST_SORT_ORDER = "appListSortOrder"
  const val PREF_DEFAULT_APP_LIST_SORT_ORDER = "lastUpdated"

  fun getAppListSortOrder(s: String?) =
    when (s) {
      "name" -> AppListSortOrder.NAME
      else -> AppListSortOrder.LAST_UPDATED
    }

  fun AppListSortOrder.toSettings() =
    when (this) {
      AppListSortOrder.LAST_UPDATED -> "lastUpdated"
      AppListSortOrder.NAME -> "name"
    }

  const val PREF_KEY_MY_APPS_SORT_ORDER = "myAppsSortOrder"
  const val PREF_DEFAULT_MY_APPS_SORT_ORDER = "name"

  const val PREF_KEY_IGNORED_APP_ISSUES = "ignoredAppIssues"

  const val PREF_KEY_INSTALL_HISTORY = "keepInstallHistory"
  const val PREF_DEFAULT_INSTALL_HISTORY = false
}

fun String?.toAutoUpdateValue() =
  try {
    if (this == null) AutoUpdateValues.OnlyWifi else AutoUpdateValues.valueOf(this)
  } catch (_: IllegalArgumentException) {
    AutoUpdateValues.OnlyWifi
  }

fun String?.toMirrorChooserValue() =
  try {
    if (this == null) MirrorChooserValues.Random else MirrorChooserValues.valueOf(this)
  } catch (_: IllegalArgumentException) {
    MirrorChooserValues.Random
  }
