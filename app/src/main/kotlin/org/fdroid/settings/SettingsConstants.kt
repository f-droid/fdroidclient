package org.fdroid.settings

import org.fdroid.database.AppListSortOrder
import org.fdroid.settings.SettingsConstants.AutoUpdateValues

object SettingsConstants {

    const val PREF_KEY_LAST_UPDATE_CHECK = "lastRepoUpdateCheck"
    const val PREF_DEFAULT_LAST_UPDATE_CHECK = -1

    const val PREF_KEY_THEME = "theme"
    const val PREF_DEFAULT_THEME = "followSystem"

    const val PREF_KEY_DYNAMIC_COLORS = "dynamicColors"
    const val PREF_DEFAULT_DYNAMIC_COLORS = false

    enum class AutoUpdateValues { OnlyWifi, Always, Never }

    const val PREF_KEY_REPO_UPDATES = "repoAutoUpdates"
    val PREF_DEFAULT_REPO_UPDATES = AutoUpdateValues.OnlyWifi.name

    const val PREF_KEY_AUTO_UPDATES = "appAutoUpdates"
    val PREF_DEFAULT_AUTO_UPDATES = AutoUpdateValues.OnlyWifi.name

    const val PREF_KEY_PROXY = "proxy"
    const val PREF_DEFAULT_PROXY = ""

    const val PREF_KEY_SHOW_INCOMPATIBLE = "incompatibleVersions"
    const val PREF_DEFAULT_SHOW_INCOMPATIBLE = true

    const val PREF_KEY_APP_LIST_SORT_ORDER = "appListSortOrder"
    const val PREF_DEFAULT_APP_LIST_SORT_ORDER = "lastUpdated"
    fun getAppListSortOrder(s: String?) = when (s) {
        "name" -> AppListSortOrder.NAME
        else -> AppListSortOrder.LAST_UPDATED
    }

    fun AppListSortOrder.toSettings() = when (this) {
        AppListSortOrder.LAST_UPDATED -> "lastUpdated"
        AppListSortOrder.NAME -> "name"
    }

    const val PREF_KEY_IGNORED_APP_ISSUES = "ignoredAppIssues"
}

fun String?.toAutoUpdateValue() = try {
    if (this == null) AutoUpdateValues.OnlyWifi
    else AutoUpdateValues.valueOf(this)
} catch (_: IllegalArgumentException) {
    AutoUpdateValues.OnlyWifi
}
