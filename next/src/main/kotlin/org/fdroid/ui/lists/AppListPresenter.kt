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
import org.fdroid.ui.utils.normalize
import java.util.Locale

@Composable
fun AppListPresenter(
    appsFlow: StateFlow<List<AppListItem>?>,
    sortByFlow: StateFlow<AppListSortOrder>,
    filterIncompatibleFlow: StateFlow<Boolean>,
    categoriesFlow: Flow<List<CategoryItem>>,
    filteredCategoryIdsFlow: StateFlow<Set<String>>,
    repositoriesFlow: Flow<List<RepositoryItem>>,
    filteredRepositoryIdsFlow: StateFlow<Set<Long>>,
    searchQueryFlow: StateFlow<String>,
): AppListModel {
    val apps = appsFlow.collectAsState(null).value
    val sortBy = sortByFlow.collectAsState().value
    val filterIncompatible = filterIncompatibleFlow.collectAsState().value
    val categories = categoriesFlow.collectAsState(null).value
    val filteredCategoryIds = filteredCategoryIdsFlow.collectAsState().value
    val repositories = repositoriesFlow.collectAsState(emptyList()).value
    val filteredRepositoryIds = filteredRepositoryIdsFlow.collectAsState().value
    val searchQuery = searchQueryFlow.collectAsState().value.normalize()

    val availableCategoryIds = remember(apps) {
        // if there's only one category, we'll not show the filters for it
        apps?.flatMap { it.categoryIds ?: emptySet() }?.toSet()?.takeIf { it.size > 1 }
            ?: emptySet()
    }
    val filteredCategories = remember(categories, apps) {
        categories?.filter {
            it.id in availableCategoryIds
        }
    }
    val availableRepositories = remember(apps) {
        val repoIds = mutableSetOf<Long>()
        apps?.forEach { repoIds.add(it.repoId) }
        val repos = repositories.filter { it.repoId in repoIds }
        // if there's only one repository, we'll not show the filters for it
        if (repos.size > 1) repos else emptyList()
    }
    val filteredApps = apps?.filter {
        val matchesCategories = filteredCategoryIds.isEmpty() ||
            (it.categoryIds ?: emptySet()).intersect(filteredCategoryIds).isNotEmpty()
        val matchesRepos = filteredRepositoryIds.isEmpty() || it.repoId in filteredRepositoryIds
        val matchesQuery = searchQuery.isEmpty() ||
            it.name.normalize().contains(searchQuery, ignoreCase = true) ||
            it.summary.normalize().contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        val matchesCompatibility = !filterIncompatible || it.isCompatible
        matchesCategories && matchesRepos && matchesQuery && matchesCompatibility
    }
    val locale = Locale.getDefault()
    return AppListModel(
        apps = if (sortBy == AppListSortOrder.NAME) {
            filteredApps?.sortedBy { it.name.lowercase(locale) }
        } else {
            filteredApps?.sortedByDescending { it.lastUpdated }
        },
        sortBy = sortBy,
        filterIncompatible = filterIncompatible,
        categories = filteredCategories,
        filteredCategoryIds = filteredCategoryIds,
        repositories = availableRepositories,
        filteredRepositoryIds = filteredRepositoryIds,
    )
}
