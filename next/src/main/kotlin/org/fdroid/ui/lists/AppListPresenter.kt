package org.fdroid.ui.lists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.categories.CategoryItem
import java.util.Locale

@Composable
fun AppListPresenter(
    listFlow: StateFlow<AppListType>,
    appsFlow: StateFlow<List<AppListItem>?>,
    areFiltersShownFlow: StateFlow<Boolean>,
    sortByFlow: StateFlow<AppListSortOrder>,
    allCategoriesFlow: Flow<List<CategoryItem>>,
    addedCategoriesFlow: StateFlow<List<String>>,
): AppListModel {
    val apps = appsFlow.collectAsState(null).value
    val sortBy = sortByFlow.collectAsState().value
    val allCategories = allCategoriesFlow.collectAsState(null).value
    val addedCategories = addedCategoriesFlow.collectAsState().value

    val filteredApps = apps?.filter { app ->
        addedCategories.isEmpty() || addedCategories.any { app.summary.contains(it) }
    }

    return AppListModel(
        list = listFlow.collectAsState().value,
        apps = if (sortBy == AppListSortOrder.NAME) {
            filteredApps?.sortedBy { it.name.lowercase(Locale.getDefault()) }
        } else {
            filteredApps?.sortedByDescending { it.lastUpdated }
        },
        areFiltersShown = areFiltersShownFlow.collectAsState().value,
        sortBy = sortBy,
        allCategories = allCategories,
        addedCategories = addedCategories,
    )
}

data class AppListModel(
    val list: AppListType,
    val apps: List<AppListItem>?,
    val areFiltersShown: Boolean,
    val sortBy: AppListSortOrder,
    val allCategories: List<CategoryItem>?,
    val addedCategories: List<String>,
)
