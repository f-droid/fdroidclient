package org.fdroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import org.fdroid.database.AppListSortOrder
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.apps.MyApps
import org.fdroid.ui.apps.MyAppsViewModel
import org.fdroid.ui.details.AppDetails
import org.fdroid.ui.details.AppDetailsViewModel
import org.fdroid.ui.discover.Discover
import org.fdroid.ui.discover.DiscoverViewModel
import org.fdroid.ui.lists.AppList
import org.fdroid.ui.lists.AppListInfo
import org.fdroid.ui.lists.AppListViewModel
import org.fdroid.ui.repositories.Repositories
import org.fdroid.ui.repositories.RepositoriesViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Main() {
    val backStack = rememberNavBackStack<NavigationKey>(NavigationKey.Discover)
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
    FDroidContent {
        NavDisplay(
            backStack = backStack,
            onBack = { keysToRemove ->
                repeat(keysToRemove) { backStack.removeLastOrNull() }
            },
            sceneStrategy = listDetailStrategy,
            entryDecorators = listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<NavigationKey.Discover>(
                    metadata = ListDetailSceneStrategy.listPane("appdetails") {
                        Text("No app selected")
                    },
                ) {
                    val viewModel = hiltViewModel<DiscoverViewModel>()
                    val numUpdates = viewModel.numUpdates.collectAsStateWithLifecycle(0).value
                    Discover(
                        discoverModel = viewModel.discoverModel.collectAsStateWithLifecycle().value,
                        onListTap = {
                            backStack.add(NavigationKey.AppList(it))
                        },
                        onAppTap = {
                            backStack.add(NavigationKey.AppDetails(it.packageName))
                        },
                        onNav = { backStack.add(it) },
                        numUpdates = numUpdates,
                        isBigScreen = isBigScreen,
                        modifier = Modifier,
                    )
                }
                entry<NavigationKey.MyApps>(
                    metadata = ListDetailSceneStrategy.listPane("appdetails") {
                        Text("No app selected")
                    },
                ) {
                    val myAppsViewModel = hiltViewModel<MyAppsViewModel>()
                    val myAppsModel =
                        myAppsViewModel.myAppsModel.collectAsStateWithLifecycle().value
                    MyApps(
                        myAppsModel = myAppsModel,
                        currentPackageName = if (isBigScreen) {
                            (backStack.last() as? NavigationKey.AppDetails)?.packageName
                        } else null,
                        onAppItemClick = {
                            backStack.add(NavigationKey.AppDetails(it))
                        },
                        onNav = { backStack.add(it) },
                        onRefresh = myAppsViewModel::refresh,
                        isBigScreen = isBigScreen,
                        onSortChanged = myAppsViewModel::changeSortOrder,
                    )
                }
                entry<NavigationKey.AppDetails>(
                    metadata = ListDetailSceneStrategy.detailPane("appdetails")
                ) {
                    val appDetailsViewModel = hiltViewModel<AppDetailsViewModel>()
                    LaunchedEffect(it.packageName) {
                        appDetailsViewModel.setAppDetails(it.packageName)
                    }
                    AppDetails(
                        item = appDetailsViewModel.appDetails.collectAsStateWithLifecycle().value,
                        onNav = { navKey -> backStack.add(navKey) },
                        onBackNav = if (isBigScreen) null else {
                            { backStack.removeLastOrNull() }
                        },
                        modifier = Modifier,
                    )
                }
                entry<NavigationKey.AppList>(
                    metadata = ListDetailSceneStrategy.listPane("appdetails") {
                        Text("No app selected")
                    },
                ) {
                    val appListViewModel = hiltViewModel<AppListViewModel>()
                    LaunchedEffect(it.type) {
                        appListViewModel.load(it.type)
                    }
                    val appListInfo = object : AppListInfo {
                        override val model =
                            appListViewModel.appListModel.collectAsStateWithLifecycle().value

                        override fun toggleFilterVisibility() {
                            appListViewModel.toggleListFilterVisibility()
                        }

                        override fun sortBy(sort: AppListSortOrder) = appListViewModel.sortBy(sort)
                        override fun addCategory(category: String) =
                            appListViewModel.addCategory(category)

                        override fun removeCategory(category: String) =
                            appListViewModel.removeCategory(category)
                    }
                    AppList(
                        appListInfo = appListInfo,
                        currentPackageName = if (isBigScreen) {
                            (backStack.last() as? NavigationKey.AppDetails)?.packageName
                        } else null,
                        onBackClicked = { backStack.removeLastOrNull() },
                        modifier = Modifier,
                    ) { packageName ->
                        backStack.add(NavigationKey.AppDetails(packageName))
                    }
                }
                entry<NavigationKey.Repos>(
                    metadata = ListDetailSceneStrategy.listPane("repos") {
                        Text(text = "No repository selected")
                    },
                ) {
                    val viewModel = hiltViewModel<RepositoriesViewModel>()
                    val repos = viewModel.repos.collectAsStateWithLifecycle(null).value
                    val visibleRepository =
                        viewModel.visibleRepository.collectAsStateWithLifecycle().value
                    Repositories(
                        repositories = repos,
                        currentRepository = visibleRepository,
                        onRepositorySelected = {
                            viewModel.setVisibleRepository(it)
                            backStack.add(NavigationKey.RepoDetails(it.repoId))
                        },
                        onAddRepo = { },
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NavigationKey.RepoDetails>(
                    metadata = ListDetailSceneStrategy.detailPane("repos")
                ) {
                    Column(
                        verticalArrangement = spacedBy(16.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .safeDrawingPadding(),
                    ) {
                        Text("Repo ${it.repoId}")
                        Text("This will basically be the repo details screen from latest client")
                    }
                }
                entry(NavigationKey.Settings) {
                    Settings { backStack.removeLastOrNull() }
                }
                entry(NavigationKey.About) {
                    About { backStack.removeLastOrNull() }
                }
            },
        )
    }
}
