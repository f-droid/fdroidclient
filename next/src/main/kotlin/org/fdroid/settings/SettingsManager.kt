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

    fun saveAppListFilter(sortOrder: AppListSortOrder, filterIncompatible: Boolean) {
        _appListSortOrder.value = sortOrder
        _filterIncompatible.value = filterIncompatible
    }
}
