package org.fdroid.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.viktormykhailiv.compose.hints.HintHost
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.install.InstallConfirmationState
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
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
import org.fdroid.ui.navigation.BottomBar
import org.fdroid.ui.navigation.IntentRouter
import org.fdroid.ui.navigation.MainNavKey
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.NavigationRail
import org.fdroid.ui.navigation.Navigator
import org.fdroid.ui.navigation.rememberNavigationState
import org.fdroid.ui.navigation.toEntries
import org.fdroid.ui.navigation.topLevelRoutes
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
fun Main(onListeningForIntent: () -> Unit = {}) {
    val navigationState = rememberNavigationState(
        startRoute = NavigationKey.Discover,
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember { Navigator(navigationState) }
    // set up intent routing by listening to new intents from activity
    val activity = (LocalActivity.current as ComponentActivity)
    DisposableEffect(navigator) {
        val intentListener = IntentRouter(navigator)
        activity.addOnNewIntentListener(intentListener)
        onListeningForIntent() // call this to get informed about initial intents we have missed
        onDispose { activity.removeOnNewIntentListener(intentListener) }
    }
    // Override the defaults so that there isn't a horizontal space between the panes.
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 2.dp)
    }
    val isBigScreen = directive.maxHorizontalPartitions > 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<NavigationKey.Discover>(
            metadata = ListDetailSceneStrategy.listPane("appdetails") {
                Text(stringResource(R.string.no_app_selected))
            },
        ) {
            val viewModel = hiltViewModel<DiscoverViewModel>()
            Discover(
                discoverModel = viewModel.discoverModel.collectAsStateWithLifecycle().value,
                onListTap = {
                    navigator.navigate(NavigationKey.AppList(it))
                },
                onAppTap = {
                    val new = NavigationKey.AppDetails(it.packageName)
                    if (navigator.last is NavigationKey.AppDetails) {
                        navigator.replaceLast(new)
                    } else {
                        navigator.navigate(new)
                    }
                },
                onNav = { navKey -> navigator.navigate(navKey) },
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
                    (navigator.last as? NavigationKey.AppDetails)?.packageName
                } else null,
                onAppItemClick = {
                    val new = NavigationKey.AppDetails(it)
                    if (navigator.last is NavigationKey.AppDetails) {
                        navigator.replaceLast(new)
                    } else {
                        navigator.navigate(new)
                    }
                },
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
                onNav = { navKey -> navigator.navigate(navKey) },
                onBackNav = if (isBigScreen) null else {
                    { navigator.goBack() }
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
                    (navigator.last as? NavigationKey.AppDetails)?.packageName
                } else null,
                onBackClicked = { navigator.goBack() },
            ) { packageName ->
                val new = NavigationKey.AppDetails(packageName)
                if (navigator.last is NavigationKey.AppDetails) {
                    navigator.replaceLast(new)
                } else {
                    navigator.navigate(new)
                }
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
            Repositories(info) {
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
        entry(NavigationKey.Settings) {
            val viewModel = hiltViewModel<SettingsViewModel>()
            Settings(
                model = viewModel.model,
                onSaveLogcat = {
                    viewModel.onSaveLogcat(it)
                    navigator.goBack()
                },
                onBackClicked = { navigator.goBack() },
            )
        }
        entry(NavigationKey.About) {
            About { navigator.goBack() }
        }
    }
    val viewModel = hiltViewModel<MainViewModel>()
    val dynamicColors =
        viewModel.dynamicColors.collectAsStateWithLifecycle(PREF_DEFAULT_DYNAMIC_COLORS).value
    val numUpdates = viewModel.numUpdates.collectAsStateWithLifecycle().value
    val hasAppIssues = viewModel.hasAppIssues.collectAsStateWithLifecycle(false).value
    val navDisplay = @Composable { modifier: Modifier ->
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            contentAlignment = Center,
            sceneStrategy = listDetailStrategy,
            onBack = { navigator.goBack() },
            modifier = modifier,
        )
    }
    FDroidContent(dynamicColors = dynamicColors) {
        HintHost {
            if (isBigScreen) Row {
                NavigationRail(
                    numUpdates = numUpdates,
                    hasIssues = hasAppIssues,
                    currentNavKey = navigationState.topLevelRoute,
                    onNav = { navKey -> navigator.navigate(navKey) },
                    modifier = Modifier.padding(top = 16.dp)
                )
                // need to consume start insets or some phones leave a lot of space there
                navDisplay(Modifier.consumeWindowInsets(PaddingValues(start = 64.dp)))
            } else if (navigator.last is MainNavKey) {
                Scaffold(bottomBar = {
                    BottomBar(
                        numUpdates = numUpdates,
                        hasIssues = hasAppIssues,
                        currentNavKey = navigationState.topLevelRoute,
                        onNav = { navKey -> navigator.navigate(navKey) },
                    )
                }) { paddingValues ->
                    // we only apply the bottom padding here, so content stays above bottom bar
                    // but we need to consume the navigation bar height manually
                    val bottom = with(LocalDensity.current) {
                        WindowInsets.navigationBars.getBottom(this).toDp()
                    }
                    val modifier = Modifier
                        .consumeWindowInsets(PaddingValues(bottom = bottom))
                        .padding(bottom = paddingValues.calculateBottomPadding())
                    navDisplay(modifier)
                }
            } else {
                navDisplay(Modifier)
            }
        }
    }
}
