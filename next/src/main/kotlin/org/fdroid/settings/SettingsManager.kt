package org.fdroid.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.fdroid.database.AppListSortOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor() {

    // TODO read these values from persistent storage
    private val _filterIncompatible = MutableStateFlow(false)
    val filterIncompatible = _filterIncompatible.asStateFlow()
    private val _appListSortOrder = MutableStateFlow(AppListSortOrder.LAST_UPDATED)
    val appListSortOrder = _appListSortOrder.asStateFlow()
    private val _showFilterOnboarding = MutableStateFlow(true)
    val showFilterOnboarding = _showFilterOnboarding.asStateFlow()
    private val _showRepositoriesOnboarding = MutableStateFlow(true)
    val showRepositoriesOnboarding = _showRepositoriesOnboarding.asStateFlow()
    private val _showRepoDetailsOnboarding = MutableStateFlow(true)
    val showRepoDetailsOnboarding = _showRepoDetailsOnboarding.asStateFlow()

    fun saveAppListFilter(sortOrder: AppListSortOrder, filterIncompatible: Boolean) {
        _appListSortOrder.value = sortOrder
        _filterIncompatible.value = filterIncompatible
    }

    fun onFilterOnboardingSeen() {
        _showFilterOnboarding.value = false
        // TODO persist
    }

    fun onRepositoriesOnboardingSeen() {
        _showRepositoriesOnboarding.value = false
        // TODO persist
    }

    fun onRepoDetailsOnboardingSeen() {
        _showRepoDetailsOnboarding.value = false
        // TODO persist
    }

}
