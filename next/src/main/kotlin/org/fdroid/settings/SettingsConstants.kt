package org.fdroid.settings

import org.fdroid.database.AppListSortOrder

object SettingsConstants {

    const val PREF_KEY_LAST_UPDATE_CHECK = "lastUpdateCheck"
    const val PREF_DEFAULT_LAST_UPDATE_CHECK = -1

    const val PREF_KEY_THEME = "theme"
    const val PREF_DEFAULT_THEME = "followSystem"

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

}
