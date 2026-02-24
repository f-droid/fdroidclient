package org.fdroid.ui.lists

import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.repositories.RepositoryItem

interface AppListInfo {
    val model: AppListModel
    val actions: AppListActions
    val list: AppListType
    val showFilters: Boolean
    val showOnboarding: Boolean
}

data class AppListModel(
    val apps: List<AppListItem>?,
    val sortBy: AppListSortOrder,
    val filterIncompatible: Boolean,
    val categories: List<CategoryItem>?,
    val filteredCategoryIds: Set<String>,
    val antiFeatures: List<AntiFeatureItem>?,
    val notSelectedAntiFeatureIds: Set<String>,
    val repositories: List<RepositoryItem>,
    val filteredRepositoryIds: Set<Long>,
)

interface AppListActions {
    fun toggleFilterVisibility()
    fun sortBy(sort: AppListSortOrder)
    fun toggleFilterIncompatible()
    fun addCategory(categoryId: String)
    fun removeCategory(categoryId: String)
    fun addAntiFeature(antiFeatureId: String)
    fun removeAntiFeature(antiFeatureId: String)
    fun addRepository(repoId: Long)
    fun removeRepository(repoId: Long)
    fun saveFilters()
    fun clearFilters()
    fun onSearch(query: String)
    fun onOnboardingSeen()
}
