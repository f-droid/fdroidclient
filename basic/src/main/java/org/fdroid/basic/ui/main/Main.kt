package org.fdroid.basic.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.fdroid.basic.MainViewModel
import org.fdroid.basic.R
import org.fdroid.basic.ui.icons.PackageVariant
import org.fdroid.basic.ui.main.discover.FilterInfo
import org.fdroid.basic.ui.main.discover.Sort
import org.fdroid.basic.ui.main.repositories.RepositoriesScaffold
import org.fdroid.fdroid.ui.theme.FDroidContent

sealed class NavDestinations(
    val id: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    object Main : NavDestinations("main", R.string.app_name, Icons.Default.Info)
    object Repos : NavDestinations("repos", R.string.app_details_repositories, PackageVariant)
    object Settings : NavDestinations("settings", R.string.menu_settings, Icons.Filled.Settings)
    object About : NavDestinations("about", R.string.about, Icons.Filled.Info)
}

val topBarMenuItems = listOf(
    NavDestinations.Repos,
    NavDestinations.Settings,
)

val moreMenuItems = listOf(
    NavDestinations.About,
)

@Composable
fun Main(viewModel: MainViewModel = hiltViewModel()) {
    FDroidContent {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = NavDestinations.Main.id) {
            composable(route = NavDestinations.Main.id) {
                val numUpdates = viewModel.numUpdates.collectAsStateWithLifecycle(0).value
                val updates = viewModel.updates.collectAsStateWithLifecycle().value
                val installed = viewModel.installed.collectAsStateWithLifecycle().value
                val filterInfo = object : FilterInfo {
                    override val model = viewModel.filterModel.collectAsStateWithLifecycle().value
                    override fun sortBy(sort: Sort) = viewModel.sortBy(sort)
                    override fun addCategory(category: String) = viewModel.addCategory(category)
                    override fun removeCategory(category: String) =
                        viewModel.removeCategory(category)

                    override fun showOnlyInstalledApps(onlyInstalled: Boolean) =
                        viewModel.showOnlyInstalledApps(onlyInstalled)
                }
                val currentItem = viewModel.appDetails.collectAsStateWithLifecycle().value
                BottomBarScreen(
                    onMainNav = { navController.navigate(it) },
                    numUpdates = numUpdates,
                    updates = updates,
                    installed = installed,
                    filterInfo = filterInfo,
                    currentItem = currentItem,
                    onSelectAppItem = viewModel::setAppDetails,
                )
            }
            composable(route = NavDestinations.Repos.id) {
                val repositoryManager = viewModel.repositoryManager
                val repos = repositoryManager.repos.collectAsStateWithLifecycle().value
                val visibleRepository =
                    repositoryManager.visibleRepository.collectAsStateWithLifecycle().value
                RepositoriesScaffold(
                    repositories = repos,
                    currentRepository = visibleRepository,
                    onRepositorySelected = repositoryManager::setVisibleRepository,
                ) {
                    navController.popBackStack()
                }
            }
            composable(route = NavDestinations.Settings.id) {
                Settings { navController.popBackStack() }
            }
            composable(route = NavDestinations.About.id) {
                About { navController.popBackStack() }
            }
            // Add more destinations similarly.
        }

    }
}
