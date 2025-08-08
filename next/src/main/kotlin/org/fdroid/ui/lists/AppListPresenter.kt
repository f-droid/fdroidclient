@file:Suppress("ktlint:standard:filename")

package org.fdroid.ui.lists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
    searchQueryFlow: StateFlow<String>,
): AppListModel {
    val apps = appsFlow.collectAsState(null).value
    val sortBy = sortByFlow.collectAsState().value
    val categories = categoriesFlow.collectAsState(null).value
    val filteredCategoryIds = filteredCategoryIdsFlow.collectAsState().value
    val repositories = repositoriesFlow.collectAsState(emptyList()).value
    val filteredRepositoryIds = filteredRepositoryIdsFlow.collectAsState().value
    val searchQuery = searchQueryFlow.collectAsState().value

    val availableCategoryIds = remember(apps) {
        apps?.flatMap { it.categoryIds ?: emptySet() }?.toSet() ?: emptySet()
    }
    val filteredCategories = remember(categories, apps) {
        categories?.filter {
            it.id in availableCategoryIds
        }
    }
    val filteredApps = apps?.filter {
        val matchesCategories = filteredCategoryIds.isEmpty() ||
            (it.categoryIds ?: emptySet()).intersect(filteredCategoryIds).isNotEmpty()
        val matchesRepos = filteredRepositoryIds.isEmpty() || it.repoId in filteredRepositoryIds
        val matchesQuery = searchQuery.isEmpty() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.summary.contains(searchQuery, ignoreCase = true)
        matchesCategories && matchesRepos && matchesQuery
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
        categories = filteredCategories,
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
