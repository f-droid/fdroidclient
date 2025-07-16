package org.fdroid.basic.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import org.fdroid.basic.MainViewModel
import org.fdroid.basic.R
import org.fdroid.basic.ui.NavigationKey
import org.fdroid.basic.ui.icons.PackageVariant
import org.fdroid.basic.ui.main.apps.MyApps
import org.fdroid.basic.ui.main.details.AppDetails
import org.fdroid.basic.ui.main.discover.Discover
import org.fdroid.basic.ui.main.lists.AppList
import org.fdroid.basic.ui.main.lists.FilterInfo
import org.fdroid.basic.ui.main.lists.Sort
import org.fdroid.basic.ui.main.repositories.RepositoryList
import org.fdroid.fdroid.ui.theme.FDroidContent

sealed class NavDestinations(
    val id: NavigationKey,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    object Repos :
        NavDestinations(NavigationKey.Repos, R.string.app_details_repositories, PackageVariant)

    object Settings :
        NavDestinations(NavigationKey.Settings, R.string.menu_settings, Icons.Filled.Settings)

    object About : NavDestinations(NavigationKey.About, R.string.about, Icons.Filled.Info)
}

val topBarMenuItems = listOf(
    NavDestinations.Repos,
    NavDestinations.Settings,
)

val moreMenuItems = listOf(
    NavDestinations.About,
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Main(viewModel: MainViewModel = hiltViewModel()) {
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
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()//(directive = directive)
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
//                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<NavigationKey.Discover>(
                    metadata = ListDetailSceneStrategy.listPane("appdetails") {
                        Text("No app selected")
                    },
                ) {
                    val numUpdates = viewModel.numUpdates.collectAsStateWithLifecycle(0).value
                    Discover(
                        discoverModel = viewModel.discoverModel.collectAsStateWithLifecycle().value,
                        onTitleTap = {
                            viewModel.setAppList(it)
                            backStack.add(NavigationKey.AppList)
                        },
                        onAppTap = {
                            viewModel.setAppDetails(it)
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
                    val updates = viewModel.updates.collectAsStateWithLifecycle().value
                    val installed = viewModel.installed.collectAsStateWithLifecycle().value
                    val currentItem = viewModel.appDetails.collectAsStateWithLifecycle().value
                    MyApps(
                        updatableApps = updates,
                        installedApps = installed,
                        currentItem = currentItem,
                        onItemClick = {
                            viewModel.setAppDetails(it)
                            backStack.add(NavigationKey.AppDetails(it.packageName))
                        },
                        onNav = { backStack.add(it) },
                        sortBy = viewModel.myAppsManager.sortBy.collectAsStateWithLifecycle().value,
                        isBigScreen = isBigScreen,
                        onSortChanged = viewModel.myAppsManager::sortBy,
                    )
                }
                entry<NavigationKey.AppDetails>(
                    metadata = ListDetailSceneStrategy.detailPane("appdetails")
                ) {
                    val currentItem = viewModel.appDetails.collectAsStateWithLifecycle().value
                    it.packageName // TODO use?
                    AppDetails(
                        appItem = currentItem,
                        onBackNav = { backStack.removeLastOrNull() },
                        modifier = Modifier,
                    )
                }
                entry<NavigationKey.AppList>(
                    metadata = ListDetailSceneStrategy.listPane("appdetails") {
                        Text("No app selected")
                    },
                ) {
                    val filterInfo = object : FilterInfo {
                        override val model =
                            viewModel.filterModel.collectAsStateWithLifecycle().value

                        override fun toggleFilterVisibility() {
                            viewModel.toggleListFilterVisibility()
                        }

                        override fun sortBy(sort: Sort) = viewModel.sortBy(sort)
                        override fun addCategory(category: String) = viewModel.addCategory(category)
                        override fun removeCategory(category: String) =
                            viewModel.removeCategory(category)
                    }
                    val currentItem = viewModel.appDetails.collectAsStateWithLifecycle().value
                    AppList(
                        appList = viewModel.currentList.collectAsStateWithLifecycle().value,
                        filterInfo = filterInfo,
                        currentItem = currentItem,
                        onBackClicked = { backStack.removeLastOrNull() },
                        modifier = Modifier,
                    ) {
                        viewModel.setAppDetails(it)
                        backStack.add(NavigationKey.AppDetails(it.packageName))
                    }
                }
                entry<NavigationKey.Repos>(
                    metadata = ListDetailSceneStrategy.listPane("repos") {
                        Text(text = "No repository selected")
                    },
                ) {
                    val repositoryManager = viewModel.repositoryManager
                    val repos = repositoryManager.repos.collectAsStateWithLifecycle(null).value
                    val visibleRepository =
                        repositoryManager.visibleRepository.collectAsStateWithLifecycle().value
                    RepositoryList(
                        repositories = repos,
                        currentRepository = visibleRepository,
                        onRepositorySelected = {
                            repositoryManager.setVisibleRepository(it)
                            backStack.add(NavigationKey.RepoDetails(it.repoId))
                        } ,
                        onAddRepo = repositoryManager::addRepo,
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
