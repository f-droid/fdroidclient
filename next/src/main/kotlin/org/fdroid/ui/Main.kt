package org.fdroid.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.viktormykhailiv.compose.hints.HintHost
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.install.InstallConfirmationState
import org.fdroid.ui.apps.AppWithIssueItem
import org.fdroid.ui.apps.MyApps
import org.fdroid.ui.apps.MyAppsInfo
import org.fdroid.ui.apps.MyAppsViewModel
import org.fdroid.ui.details.AppDetails
import org.fdroid.ui.details.AppDetailsViewModel
import org.fdroid.ui.discover.Discover
import org.fdroid.ui.discover.DiscoverViewModel
import org.fdroid.ui.lists.AppList
import org.fdroid.ui.lists.AppListActions
import org.fdroid.ui.lists.AppListInfo
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.lists.AppListViewModel
import org.fdroid.ui.repositories.Repositories
import org.fdroid.ui.repositories.RepositoriesViewModel
import org.fdroid.ui.repositories.RepositoryInfo
import org.fdroid.ui.repositories.RepositoryItem
import org.fdroid.ui.repositories.RepositoryModel
import org.fdroid.ui.repositories.add.AddRepo
import org.fdroid.ui.repositories.add.AddRepoViewModel
import org.fdroid.ui.repositories.details.RepoDetails
import org.fdroid.ui.repositories.details.RepoDetailsInfo
import org.fdroid.ui.repositories.details.RepoDetailsViewModel
import org.fdroid.ui.settings.Settings
import org.fdroid.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Main(dynamicColors: Boolean, onListeningForIntent: () -> Unit = {}) {
    val backStack = rememberNavBackStack(NavigationKey.Discover)
    // set up intent routing by listening to new intents from activity
    val activity = (LocalActivity.current as ComponentActivity)
    DisposableEffect(backStack) {
        val intentListener = IntentRouter(backStack)
        activity.addOnNewIntentListener(intentListener)
        onListeningForIntent() // call this to get informed about initial intents we have missed
        onDispose { activity.removeOnNewIntentListener(intentListener) }
    }
    // Override the defaults so that there isn't a horizontal space between the panes.
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val isBigScreen = remember(windowAdaptiveInfo) {
        windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<NavigationKey.Discover>(
            metadata = ListDetailSceneStrategy.listPane("appdetails") {
                Text(stringResource(R.string.no_app_selected))
            },
        ) {
            val viewModel = hiltViewModel<DiscoverViewModel>()
            val numUpdates = viewModel.numUpdates.collectAsStateWithLifecycle(0).value
            val hasIssues = viewModel.hasAppIssues.collectAsState(false).value
            Discover(
                discoverModel = viewModel.discoverModel.collectAsStateWithLifecycle().value,
                onListTap = {
                    backStack.add(NavigationKey.AppList(it))
                },
                onAppTap = {
                    backStack.add(NavigationKey.AppDetails(it.packageName))
                },
                onNav = { navKey ->
                    if (navKey == NavigationKey.MyApps) {
                        // reset back stack when going to My Apps
                        while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                    }
                    backStack.add(navKey)
                },
                numUpdates = numUpdates,
                hasIssues = hasIssues,
                isBigScreen = isBigScreen,
                onSearch = viewModel::search,
                onSearchCleared = viewModel::onSearchCleared,
                modifier = Modifier,
            )
        }
        entry<NavigationKey.MyApps>(
            metadata = ListDetailSceneStrategy.listPane("appdetails") {
                Text(stringResource(R.string.no_app_selected))
            },
        ) {
            val myAppsViewModel = hiltViewModel<MyAppsViewModel>()
            val myAppsInfo = object : MyAppsInfo {
                override val model = myAppsViewModel.myAppsModel.collectAsStateWithLifecycle().value

                override fun updateAll() = myAppsViewModel.updateAll()
                override fun changeSortOrder(sort: AppListSortOrder) =
                    myAppsViewModel.changeSortOrder(sort)

                override fun search(query: String) = myAppsViewModel.search(query)
                override fun confirmAppInstall(
                    packageName: String,
                    state: InstallConfirmationState,
                ) = myAppsViewModel.confirmAppInstall(packageName, state)

                override fun ignoreAppIssue(item: AppWithIssueItem) =
                    myAppsViewModel.ignoreAppIssue(item)
            }
            MyApps(
                myAppsInfo = myAppsInfo,
                currentPackageName = if (isBigScreen) {
                    (backStack.last() as? NavigationKey.AppDetails)?.packageName
                } else null,
                onAppItemClick = {
                    backStack.add(NavigationKey.AppDetails(it))
                },
                onNav = { navKey ->
                    if (navKey == NavigationKey.Discover) {
                        // reset back stack when going to Discover
                        while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                    }
                    backStack.add(navKey)
                },
                isBigScreen = isBigScreen,
            )
        }
        entry<NavigationKey.AppDetails>(
            metadata = ListDetailSceneStrategy.detailPane("appdetails")
        ) {
            val viewModel = hiltViewModel<AppDetailsViewModel, AppDetailsViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(it.packageName)
                }
            )
            AppDetails(
                item = viewModel.appDetails.collectAsStateWithLifecycle().value,
                onNav = { navKey -> backStack.add(navKey) },
                onBackNav = if (isBigScreen) null else {
                    { backStack.removeLastOrNull() }
                },
                modifier = Modifier,
            )
        }
        entry<NavigationKey.AppList>(
            metadata = ListDetailSceneStrategy.listPane("appdetails") {
                Text(stringResource(R.string.no_app_selected))
            },
        ) {
            val viewModel = hiltViewModel<AppListViewModel, AppListViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(it.type)
                }
            )
            val appListInfo = object : AppListInfo {
                override val model = viewModel.appListModel.collectAsStateWithLifecycle().value
                override val list: AppListType = it.type
                override val actions: AppListActions = viewModel
                override val showFilters: Boolean =
                    viewModel.showFilters.collectAsStateWithLifecycle().value
                override val showOnboarding: Boolean =
                    viewModel.showOnboarding.collectAsStateWithLifecycle().value
            }
            AppList(
                appListInfo = appListInfo,
                currentPackageName = if (isBigScreen) {
                    (backStack.last() as? NavigationKey.AppDetails)?.packageName
                } else null,
                onBackClicked = { backStack.removeLastOrNull() },
            ) { packageName ->
                backStack.add(NavigationKey.AppDetails(packageName))
            }
        }
        entry<NavigationKey.Repos>(
            metadata = ListDetailSceneStrategy.listPane("repos") {
                Text(text = stringResource(R.string.no_repository_selected))
            },
        ) {
            val viewModel = hiltViewModel<RepositoriesViewModel>()
            val info = object : RepositoryInfo {
                override val model: RepositoryModel =
                    viewModel.model.collectAsStateWithLifecycle().value

                override val currentRepositoryId: Long? = if (isBigScreen) {
                    (backStack.last() as? NavigationKey.RepoDetails)?.repoId
                } else null

                override fun onOnboardingSeen() = viewModel.onOnboardingSeen()

                override fun onRepositorySelected(repositoryItem: RepositoryItem) {
                    backStack.add(NavigationKey.RepoDetails(repositoryItem.repoId))
                }

                override fun onRepositoryEnabled(repoId: Long, enabled: Boolean) =
                    viewModel.onRepositoryEnabled(repoId, enabled)

                override fun onAddRepo() {
                    backStack.add(NavigationKey.AddRepo())
                }

                override fun onRepositoryMoved(fromRepoId: Long, toRepoId: Long) =
                    viewModel.onRepositoriesMoved(fromRepoId, toRepoId)

                override fun onRepositoriesFinishedMoving(
                    fromRepoId: Long,
                    toRepoId: Long,
                ) = viewModel.onRepositoriesFinishedMoving(fromRepoId, toRepoId)
            }
            Repositories(info) {
                backStack.removeLastOrNull()
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
                    backStack.add(NavigationKey.AppList(type))
                },
            ) {
                backStack.removeLastOrNull()
            }
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
                proxyConfig = viewModel.proxyConfig,
                onFetchRepo = viewModel::onFetchRepo,
                onAddRepo = viewModel::addFetchedRepository,
                onExistingRepo = { repoId ->
                    backStack.removeLastOrNull()
                    backStack.add(NavigationKey.RepoDetails(repoId))
                },
                onRepoAdded = { title, repoId ->
                    backStack.removeLastOrNull()
                    backStack.add(NavigationKey.RepoDetails(repoId))
                    val type = AppListType.Repository(title, repoId)
                    backStack.add(NavigationKey.AppList(type))
                },
                onBackClicked = { backStack.removeLastOrNull() },
            )
        }
        entry(NavigationKey.Settings) {
            val viewModel = hiltViewModel<SettingsViewModel>()
            Settings(
                model = viewModel.model,
                onSaveLogcat = {
                    viewModel.onSaveLogcat(it)
                    backStack.removeLastOrNull()
                },
                onBackClicked = { backStack.removeLastOrNull() },
            )
        }
        entry(NavigationKey.About) {
            About { backStack.removeLastOrNull() }
        }
    }
    FDroidContent(dynamicColors = dynamicColors) {
        HintHost {
            NavDisplay(
                backStack = backStack,
                sceneStrategy = listDetailStrategy,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )
        }
    }
}
