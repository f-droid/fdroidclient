package org.fdroid.appsearch

import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem

data class SearchResults(
    val apps: List<AppListItem>,
    val categories: List<CategoryItem>,
)
