package org.fdroid.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import me.zhanghai.compose.preference.createPreferenceFlow
import mu.KotlinLogging
import org.fdroid.database.AppListSortOrder
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_APP_LIST_SORT_ORDER
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_LAST_UPDATE_CHECK
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_SHOW_INCOMPATIBLE
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_THEME
import org.fdroid.settings.SettingsConstants.PREF_KEY_APP_LIST_SORT_ORDER
import org.fdroid.settings.SettingsConstants.PREF_KEY_LAST_UPDATE_CHECK
import org.fdroid.settings.SettingsConstants.PREF_KEY_SHOW_INCOMPATIBLE
import org.fdroid.settings.SettingsConstants.PREF_KEY_THEME
import org.fdroid.settings.SettingsConstants.getAppListSortOrder
import org.fdroid.settings.SettingsConstants.toSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val log = KotlinLogging.logger {}

    private val prefs by lazy {
        context.getSharedPreferences("${context.packageName}_preferences", MODE_PRIVATE)
    }

    /**
     * This is mutable, so the settings UI can make changes to it.
     */
    val prefsFlow by lazy { createPreferenceFlow(prefs) }
    val theme get() = prefs.getString(PREF_KEY_THEME, PREF_DEFAULT_THEME)!!
    val themeFlow = prefsFlow.map { it.get<String>(PREF_KEY_THEME) }
    var lastRepoUpdate: Long
        get() = prefs.getLong(PREF_KEY_LAST_UPDATE_CHECK, PREF_DEFAULT_LAST_UPDATE_CHECK)
        set(value) {
            prefs.edit { putLong(PREF_KEY_LAST_UPDATE_CHECK, value) }
            _lastRepoUpdateFlow.update { value }
        }
    private val _lastRepoUpdateFlow = MutableStateFlow(lastRepoUpdate)
    val lastRepoUpdateFlow = _lastRepoUpdateFlow.asStateFlow()

    val filterIncompatible: Boolean
        get() = !prefs.getBoolean(PREF_KEY_SHOW_INCOMPATIBLE, PREF_DEFAULT_SHOW_INCOMPATIBLE)
    val appListSortOrder: AppListSortOrder
        get() {
            val s = prefs.getString(PREF_KEY_APP_LIST_SORT_ORDER, PREF_DEFAULT_APP_LIST_SORT_ORDER)
            return getAppListSortOrder(s)
        }

    fun saveAppListFilter(sortOrder: AppListSortOrder, filterIncompatible: Boolean) {
        prefs.edit {
            putBoolean(PREF_KEY_SHOW_INCOMPATIBLE, !filterIncompatible)
            putString(PREF_KEY_APP_LIST_SORT_ORDER, sortOrder.toSettings())
        }
    }
}
