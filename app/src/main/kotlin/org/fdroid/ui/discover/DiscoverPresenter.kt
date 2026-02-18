package org.fdroid.ui.discover

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.Repository
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateState
import org.fdroid.ui.categories.CategoryGroup
import org.fdroid.ui.categories.CategoryItem

@Composable
fun DiscoverPresenter(
    newAppsFlow: Flow<List<AppDiscoverItem>>,
    recentlyUpdatedAppsFlow: Flow<List<AppDiscoverItem>>,
    mostDownloadedAppsFlow: MutableStateFlow<List<AppDiscoverItem>?>,
    categoriesFlow: Flow<List<CategoryItem>>,
    repositoriesFlow: Flow<List<Repository>>,
    searchTextFieldState: TextFieldState,
    isFirstStart: Boolean,
    networkState: NetworkState,
    repoUpdateStateFlow: StateFlow<RepoUpdateState?>,
    hasRepoIssuesFlow: Flow<Boolean>,
): DiscoverModel {
    val newApps = newAppsFlow.collectAsState(null).value
    val recentlyUpdatedApps = recentlyUpdatedAppsFlow.collectAsState(null).value
    val mostDownloadedApps = mostDownloadedAppsFlow.collectAsState().value
    val categories = categoriesFlow.collectAsState(null).value

    return if (!mostDownloadedApps.isNullOrEmpty() ||
        !newApps.isNullOrEmpty() ||
        !categories.isNullOrEmpty() ||
        !recentlyUpdatedApps.isNullOrEmpty()
    ) {
        // As soon as we loaded a list,
        // we start showing it on screen and update when other lists load.
        // This is to speed up the time to first content on initial screen.
        LoadedDiscoverModel(
            newApps = newApps ?: emptyList(),
            recentlyUpdatedApps = recentlyUpdatedApps ?: emptyList(),
            mostDownloadedApps = mostDownloadedApps,
            categories = categories?.groupBy { it.group },
            searchTextFieldState = searchTextFieldState,
            hasRepoIssues = hasRepoIssuesFlow.collectAsState(false).value,
        )
    } else {
        // everything is still null or empty, so figure out why
        val repositories = repositoriesFlow.collectAsState(null).value
        if (repositories?.all { !it.enabled } == true) {
            NoEnabledReposDiscoverModel
        } else if (isFirstStart || recentlyUpdatedApps?.size == 0) {
            // There should always be recently updated apps,
            // because those don't have a freshness constraint.
            // In case the DB got cleared (e.g. though panic action or failed migration),
            // the isFirstStart condition would be false,
            // but we still want to go down first start path to update repos again.
            FirstStartDiscoverModel(networkState, repoUpdateStateFlow.collectAsState().value)
        } else {
            LoadingDiscoverModel
        }
    }
}

sealed class DiscoverModel
data class FirstStartDiscoverModel(
    val networkState: NetworkState,
    val repoUpdateState: RepoUpdateState?,
) : DiscoverModel()

data object LoadingDiscoverModel : DiscoverModel()
data object NoEnabledReposDiscoverModel : DiscoverModel()
data class LoadedDiscoverModel(
    val newApps: List<AppDiscoverItem>,
    val recentlyUpdatedApps: List<AppDiscoverItem>,
    val mostDownloadedApps: List<AppDiscoverItem>?,
    val categories: Map<CategoryGroup, List<CategoryItem>>?,
    val searchTextFieldState: TextFieldState,
    val hasRepoIssues: Boolean,
) : DiscoverModel()
