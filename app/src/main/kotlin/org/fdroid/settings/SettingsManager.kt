package org.fdroid.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.annotation.UiThread
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.engine.ProxyConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import me.zhanghai.compose.preference.createPreferenceFlow
import me.zhanghai.compose.preference.isDefaultPreferenceFlowLongSupportEnabled
import mu.KotlinLogging
import org.fdroid.database.AppListSortOrder
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_APP_LIST_SORT_ORDER
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_INSTALL_HISTORY
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_LAST_UPDATE_CHECK
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PROXY
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_REPO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_SHOW_INCOMPATIBLE
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_THEME
import org.fdroid.settings.SettingsConstants.PREF_KEY_APP_LIST_SORT_ORDER
import org.fdroid.settings.SettingsConstants.PREF_KEY_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_DYNAMIC_COLORS
import org.fdroid.settings.SettingsConstants.PREF_KEY_IGNORED_APP_ISSUES
import org.fdroid.settings.SettingsConstants.PREF_KEY_INSTALL_HISTORY
import org.fdroid.settings.SettingsConstants.PREF_KEY_LAST_UPDATE_CHECK
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.fdroid.settings.SettingsConstants.PREF_KEY_REPO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_SHOW_INCOMPATIBLE
import org.fdroid.settings.SettingsConstants.PREF_KEY_THEME
import org.fdroid.settings.SettingsConstants.getAppListSortOrder
import org.fdroid.settings.SettingsConstants.toSettings
import java.net.InetSocketAddress
import java.net.Proxy
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
    val prefsFlow by lazy {
        isDefaultPreferenceFlowLongSupportEnabled = true
        createPreferenceFlow(prefs)
    }
    val theme get() = prefs.getString(PREF_KEY_THEME, PREF_DEFAULT_THEME)!!
    val themeFlow = prefsFlow.map { it.get<String>(PREF_KEY_THEME) }.distinctUntilChanged()
    val dynamicColorFlow: Flow<Boolean> = prefsFlow.map {
        it.get<Boolean>(PREF_KEY_DYNAMIC_COLORS) ?: PREF_DEFAULT_DYNAMIC_COLORS
    }.distinctUntilChanged()
    val repoUpdates
        get() = prefs.getString(PREF_KEY_REPO_UPDATES, PREF_DEFAULT_REPO_UPDATES)
            .toAutoUpdateValue()
    val repoUpdatesFlow
        get() = prefsFlow.map {
            it.get<String>(PREF_KEY_REPO_UPDATES).toAutoUpdateValue()
        }.distinctUntilChanged()
    val autoUpdateApps
        get() = prefs.getString(PREF_KEY_AUTO_UPDATES, PREF_DEFAULT_AUTO_UPDATES)
            .toAutoUpdateValue()
    val autoUpdateAppsFlow
        get() = prefsFlow.map {
            it.get<String>(PREF_KEY_AUTO_UPDATES).toAutoUpdateValue()
        }.distinctUntilChanged()
    var lastRepoUpdate: Long
        get() = try {
            val t = prefs.getLong(PREF_KEY_LAST_UPDATE_CHECK, PREF_DEFAULT_LAST_UPDATE_CHECK)
            // TODO for the stable release of 2.0 we can remove the migration code below
            if (t == PREF_DEFAULT_LAST_UPDATE_CHECK) {
                prefs.getLong("lastRepoUpdateCheck", PREF_DEFAULT_LAST_UPDATE_CHECK)
            } else t
        } catch (_: Exception) {
            PREF_DEFAULT_LAST_UPDATE_CHECK
        }
        set(value) {
            prefs.edit { putLong(PREF_KEY_LAST_UPDATE_CHECK, value) }
            _lastRepoUpdateFlow.update { value }
        }

    var useInstallHistory: Boolean
        get() = prefs.getBoolean(PREF_KEY_INSTALL_HISTORY, PREF_DEFAULT_INSTALL_HISTORY)
        set(value) {
            prefs.edit { putBoolean(PREF_KEY_INSTALL_HISTORY, value) }
            _useInstallHistoryFlow.update { value }
        }
    private val _useInstallHistoryFlow = MutableStateFlow(useInstallHistory)
    val useInstallHistoryFlow = _useInstallHistoryFlow.asStateFlow()

    private val _lastRepoUpdateFlow = MutableStateFlow(lastRepoUpdate)
    val lastRepoUpdateFlow = _lastRepoUpdateFlow.asStateFlow()
    val isFirstStart get() = lastRepoUpdate <= PREF_DEFAULT_LAST_UPDATE_CHECK

    /**
     * A set of package name for which we should not show app issues.
     */
    var ignoredAppIssues: Map<String, Long>
        get() = try {
            prefs.getStringSet(PREF_KEY_IGNORED_APP_ISSUES, emptySet<String>())?.associate {
                val (packageName, versionCode) = it.split('|')
                Pair(packageName, versionCode.toLong())
            } ?: emptyMap()
        } catch (e: Exception) {
            log.error(e) { "Error parsing ignored app issues: " }
            emptyMap()
        }
        private set(value) {
            val newValue = value.map { (packageName, versionCode) -> "$packageName|$versionCode" }
            prefs.edit { putStringSet(PREF_KEY_IGNORED_APP_ISSUES, newValue.toSet()) }
        }

    val proxyConfig: ProxyConfig?
        @UiThread
        get() {
            val proxyStr = prefs.getString(PREF_KEY_PROXY, PREF_DEFAULT_PROXY)
            return if (proxyStr.isNullOrBlank()) null
            else {
                val (host, port) = proxyStr.split(':')
                // don't resolve hostname here, or we get NetworkOnMainThreadException
                val address = InetSocketAddress.createUnresolved(host, port.toInt())
                Proxy(Proxy.Type.SOCKS, address)
            }
        }

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

    fun ignoreAppIssue(packageName: String, versionCode: Long) {
        val newMap = ignoredAppIssues.toMutableMap().apply {
            put(packageName, versionCode)
        }
        ignoredAppIssues = newMap
    }
}
