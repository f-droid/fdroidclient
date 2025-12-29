package org.fdroid.ui.repositories

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.Navigator
import org.fdroid.ui.repositories.add.AddRepo
import org.fdroid.ui.repositories.add.AddRepoViewModel
import org.fdroid.ui.repositories.details.RepoDetails
import org.fdroid.ui.repositories.details.RepoDetailsInfo
import org.fdroid.ui.repositories.details.RepoDetailsViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.repoEntry(
    navigator: Navigator,
    isBigScreen: Boolean,
) {
    entry<NavigationKey.Repos>(
        metadata = ListDetailSceneStrategy.listPane("repos") {
            NoRepoSelected()
        },
    ) {
        val viewModel = hiltViewModel<RepositoriesViewModel>()
        val info = object : RepositoryInfo {
            override val model: RepositoryModel =
                viewModel.model.collectAsStateWithLifecycle().value

            override val currentRepositoryId: Long? = if (isBigScreen) {
                (navigator.last as? NavigationKey.RepoDetails)?.repoId
            } else null

            override fun onOnboardingSeen() = viewModel.onOnboardingSeen()

            override fun onRepositorySelected(repositoryItem: RepositoryItem) {
                val last = navigator.last
                val new = NavigationKey.RepoDetails(repositoryItem.repoId)
                if (last is NavigationKey.RepoDetails) {
                    navigator.replaceLast(new)
                } else {
                    navigator.navigate(new)
                }
            }

            override fun onRepositoryEnabled(repoId: Long, enabled: Boolean) =
                viewModel.onRepositoryEnabled(repoId, enabled)

            override fun onAddRepo() {
                navigator.navigate(NavigationKey.AddRepo())
            }

            override fun onRepositoryMoved(fromRepoId: Long, toRepoId: Long) =
                viewModel.onRepositoriesMoved(fromRepoId, toRepoId)

            override fun onRepositoriesFinishedMoving(
                fromRepoId: Long,
                toRepoId: Long,
            ) = viewModel.onRepositoriesFinishedMoving(fromRepoId, toRepoId)
        }
        Repositories(info, isBigScreen) {
            navigator.goBack()
        }
    }
    entry<NavigationKey.RepoDetails>(
        metadata = ListDetailSceneStrategy.detailPane("repos")
    ) { navKey ->
        val viewModel = hiltViewModel<RepoDetailsViewModel, RepoDetailsViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(navKey.repoId)
            }
        )
        RepoDetails(
            info = object : RepoDetailsInfo {
                override val model = viewModel.model.collectAsStateWithLifecycle().value
                override val actions = viewModel
            },
            onShowAppsClicked = { title, repoId ->
                val type = AppListType.Repository(title, repoId)
                navigator.navigate(NavigationKey.AppList(type))
            },
            onBackNav = if (isBigScreen) null else {
                { navigator.goBack() }
            },
        )
    }
    entry<NavigationKey.AddRepo> { navKey ->
        val viewModel = hiltViewModel<AddRepoViewModel>()
        // this is for intents we receive via IntentRouter, usually the user provides URI later
        LaunchedEffect(navKey) {
            if (navKey.uri != null) {
                viewModel.onFetchRepo(navKey.uri)
            }
        }
        AddRepo(
            state = viewModel.state.collectAsStateWithLifecycle().value,
            networkStateFlow = viewModel.networkState,
            proxyConfig = viewModel.proxyConfig,
            onFetchRepo = viewModel::onFetchRepo,
            onAddRepo = viewModel::addFetchedRepository,
            onExistingRepo = { repoId ->
                navigator.goBack()
                navigator.navigate(NavigationKey.RepoDetails(repoId))
            },
            onRepoAdded = { title, repoId ->
                navigator.goBack()
                navigator.navigate(NavigationKey.RepoDetails(repoId))
                val type = AppListType.Repository(title, repoId)
                navigator.navigate(NavigationKey.AppList(type))
            },
            onBackClicked = { navigator.goBack() },
        )
    }
}
