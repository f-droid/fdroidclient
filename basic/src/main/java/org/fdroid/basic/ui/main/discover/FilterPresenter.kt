package org.fdroid.basic.ui.main.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@Composable
fun FilterPresenter(
    areFiltersShownFlow: StateFlow<Boolean>,
    appsFlow: Flow<List<AppNavigationItem>>,
    sortByFlow: StateFlow<Sort>,
    allCategories: List<String>,
    addedCategoriesFlow: StateFlow<List<String>>,
): FilterModel {
    val apps = appsFlow.collectAsState(null).value
    val sortBy = sortByFlow.collectAsState().value
    val addedCategories = addedCategoriesFlow.collectAsState().value

    val newApps = apps?.filter { app ->
        addedCategories.isEmpty() || addedCategories.any { app.summary.contains(it) }
    } ?: emptyList()

    return FilterModel(
        isLoading = apps == null,
        areFiltersShown = areFiltersShownFlow.collectAsState().value,
        apps = if (sortBy == Sort.NAME) {
            newApps.sortedBy { it.name.lowercase(Locale.getDefault()) }
        } else {
            newApps.sortedByDescending { it.lastUpdated }
        },
        sortBy = sortBy,
        allCategories = allCategories,
        addedCategories = addedCategories,
    )
}

data class FilterModel(
    val isLoading: Boolean,
    val areFiltersShown: Boolean,
    val apps: List<AppNavigationItem>,
    val sortBy: Sort,
    val allCategories: List<String>,
    val addedCategories: List<String>,
)
