package org.fdroid.basic.ui.main.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import org.fdroid.basic.ui.categories.Category
import org.fdroid.basic.ui.main.repositories.Repository

@Composable
fun DiscoverPresenter(
    appsFlow: Flow<List<AppDiscoverItem>>,
    categoriesFlow: Flow<List<Category>>,
    repositoriesFlow: Flow<List<Repository>>,
): DiscoverModel {
    val apps = appsFlow.collectAsState(null).value
    val categories = categoriesFlow.collectAsState(null).value

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
        )
    }
}

sealed class DiscoverModel
data class LoadingDiscoverModel(val isFirstStart: Boolean) : DiscoverModel()
object NoEnabledReposDiscoverModel : DiscoverModel()
data class LoadedDiscoverModel(
    val newApps: List<AppDiscoverItem>,
    val recentlyUpdatedApps: List<AppDiscoverItem>,
    val categories: List<Category>?,
) : DiscoverModel()
