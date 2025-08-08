@file:Suppress("ktlint:standard:filename")

package org.fdroid.ui.lists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.repositories.RepositoryItem
import java.util.Locale

@Composable
fun AppListPresenter(
    listFlow: StateFlow<AppListType>,
    appsFlow: StateFlow<List<AppListItem>?>,
    areFiltersShownFlow: StateFlow<Boolean>,
    sortByFlow: StateFlow<AppListSortOrder>,
    categoriesFlow: Flow<List<CategoryItem>>,
    filteredCategoryIdsFlow: StateFlow<Set<String>>,
    repositoriesFlow: Flow<List<RepositoryItem>>,
    filteredRepositoryIdsFlow: StateFlow<Set<Long>>,
): AppListModel {
    val apps = appsFlow.collectAsState(null).value
    val sortBy = sortByFlow.collectAsState().value
    val categories = categoriesFlow.collectAsState(null).value
    val filteredCategoryIds = filteredCategoryIdsFlow.collectAsState().value
    val repositories = repositoriesFlow.collectAsState(emptyList()).value
    val filteredRepositoryIds = filteredRepositoryIdsFlow.collectAsState().value

    val filteredApps = apps?.filter {
        (filteredRepositoryIds.isEmpty() || it.repoId in filteredRepositoryIds) &&
            (filteredCategoryIds.isEmpty() ||
                (it.categoryIds ?: emptySet()).intersect(filteredCategoryIds).isNotEmpty())
    }
    val locale = Locale.getDefault()
    return AppListModel(
        list = listFlow.collectAsState().value,
        apps = if (sortBy == AppListSortOrder.NAME) {
            filteredApps?.sortedBy { it.name.lowercase(locale) }
        } else {
            filteredApps?.sortedByDescending { it.lastUpdated }
        },
        areFiltersShown = areFiltersShownFlow.collectAsState().value,
        sortBy = sortBy,
        categories = categories,
        filteredCategoryIds = filteredCategoryIds,
        repositories = repositories,
        filteredRepositoryIds = filteredRepositoryIds,
    )
}

data class AppListModel(
    val list: AppListType,
    val apps: List<AppListItem>?,
    val areFiltersShown: Boolean,
    val sortBy: AppListSortOrder,
    val categories: List<CategoryItem>?,
    val filteredCategoryIds: Set<String>,
    val repositories: List<RepositoryItem>,
    val filteredRepositoryIds: Set<Long>,
)
