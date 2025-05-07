package org.fdroid.basic.ui.main.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun FilterPresenter(
    appsFlow: Flow<List<AppNavigationItem>>,
    onlyInstalledAppsFlow: StateFlow<Boolean>,
    sortByFlow: StateFlow<Sort>,
    allCategories: List<String>,
    addedCategoriesFlow: StateFlow<List<String>>,
): FilterModel {
    val apps = appsFlow.collectAsState(null).value
    val onlyInstalledApps = onlyInstalledAppsFlow.collectAsState().value
    val sortBy = sortByFlow.collectAsState().value
    val addedCategories = addedCategoriesFlow.collectAsState().value

    val newApps = apps?.filter { app ->
        if (onlyInstalledApps) app.packageName.toInt() % 2 > 0 else true
    }?.filter { app ->
        addedCategories.isEmpty() || addedCategories.any { app.summary.contains(it) }
    } ?: emptyList()

    return FilterModel(
        isLoading = apps == null,
        apps = if (sortBy == Sort.NAME) {
            newApps.sortedBy { it.packageName.toInt() }
        } else {
            newApps.sortedByDescending { it.packageName.toInt() }
        },
        onlyInstalledApps = onlyInstalledApps,
        sortBy = sortBy,
        allCategories = allCategories,
        addedCategories = addedCategories,
    )
}

data class FilterModel(
    val isLoading: Boolean,
    val apps: List<AppNavigationItem>,
    val onlyInstalledApps: Boolean,
    val sortBy: Sort,
    val allCategories: List<String>,
    val addedCategories: List<String>,
)
