package org.fdroid.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.appsearch.SearchResults
import org.fdroid.database.Repository
import org.fdroid.ui.categories.CategoryItem
import kotlin.time.Duration

@Composable
fun DiscoverPresenter(
    appsFlow: Flow<List<AppDiscoverItem>>,
    categoriesFlow: Flow<List<CategoryItem>>,
    repositoriesFlow: Flow<List<Repository>>,
    searchResultsFlow: StateFlow<SearchResults?>,
    searchOptionFlow: StateFlow<SearchOption>,
    searchTimeFlow: StateFlow<Duration?>,
): DiscoverModel {
    val apps = appsFlow.collectAsState(null).value
    val categories = categoriesFlow.collectAsState(null).value
    val searchResults = searchResultsFlow.collectAsState().value
    val searchOption = searchOptionFlow.collectAsState().value
    val searchTime = searchTimeFlow.collectAsState().value

    return if (apps.isNullOrEmpty()) {
        val repositories = repositoriesFlow.collectAsState(null).value
        if (apps == null) {
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
            newApps = apps.filter { it.isNew },
            recentlyUpdatedApps = apps.filter { !it.isNew },
            categories = categories,
            searchResults = searchResults,
            searchOption = searchOption,
            searchTime = searchTime,
        )
    }
}

sealed class DiscoverModel
data class LoadingDiscoverModel(val isFirstStart: Boolean) : DiscoverModel()
object NoEnabledReposDiscoverModel : DiscoverModel()
data class LoadedDiscoverModel(
    val newApps: List<AppDiscoverItem>,
    val recentlyUpdatedApps: List<AppDiscoverItem>,
    val categories: List<CategoryItem>?,
    val searchResults: SearchResults? = null,
    val searchOption: SearchOption = SearchOption.APPSEARCH,
    val searchTime: Duration? = null,
) : DiscoverModel()
