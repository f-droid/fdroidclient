package org.fdroid.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.Repository
import org.fdroid.ui.categories.CategoryGroup
import org.fdroid.ui.categories.CategoryItem

@Composable
fun DiscoverPresenter(
    newAppsFlow: Flow<List<AppDiscoverItem>>,
    recentlyUpdatedAppsFlow: Flow<List<AppDiscoverItem>>,
    categoriesFlow: Flow<List<CategoryItem>>,
    repositoriesFlow: Flow<List<Repository>>,
    searchResultsFlow: StateFlow<SearchResults?>,
): DiscoverModel {
    val newApps = newAppsFlow.collectAsState(null).value
    val recentlyUpdatedApps = recentlyUpdatedAppsFlow.collectAsState(null).value
    val categories = categoriesFlow.collectAsState(null).value
    val searchResults = searchResultsFlow.collectAsState().value

    // We may not have any new apps, but there should always be recently updated apps,
    // because those don't have a freshness constraint.
    // So if we don't have those, we are still loading, have no enabled repo, or this is first start
    return if (recentlyUpdatedApps.isNullOrEmpty()) {
        val repositories = repositoriesFlow.collectAsState(null).value
        if (newApps == null && recentlyUpdatedApps == null) {
            LoadingDiscoverModel(false)
        } else if (repositories?.all { !it.enabled } == true) {
            NoEnabledReposDiscoverModel
        } else {
            // apps are empty, if we have enabled repos assume this is first start (still loading)
            // TODO use more reliable check for first start
            val isFirstStart = repositories?.find { it.enabled } != null
            LoadingDiscoverModel(isFirstStart)
        }
    } else {
        LoadedDiscoverModel(
            newApps = newApps ?: emptyList(),
            recentlyUpdatedApps = recentlyUpdatedApps,
            categories = categories?.groupBy { it.group },
            searchResults = searchResults,
        )
    }
}

sealed class DiscoverModel
data class LoadingDiscoverModel(val isFirstStart: Boolean) : DiscoverModel()
object NoEnabledReposDiscoverModel : DiscoverModel()
data class LoadedDiscoverModel(
    val newApps: List<AppDiscoverItem>,
    val recentlyUpdatedApps: List<AppDiscoverItem>,
    val categories: Map<CategoryGroup, List<CategoryItem>>?,
    val searchResults: SearchResults? = null,
) : DiscoverModel()
