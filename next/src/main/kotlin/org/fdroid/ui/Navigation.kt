package org.fdroid.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.fdroid.next.R
import org.fdroid.ui.icons.PackageVariant
import org.fdroid.ui.lists.AppListType

sealed interface NavigationKey : NavKey {

    @Serializable
    data object Discover : NavigationKey

    @Serializable
    data object MyApps : NavigationKey

    @Serializable
    data class AppDetails(val packageName: String) : NavigationKey

    @Serializable
    data class AppList(val type: AppListType) : NavigationKey

    @Serializable
    data object Repos : NavigationKey

    @Serializable
    data class RepoDetails(val repoId: Long) : NavigationKey

    @Serializable
    data class AddRepo(val uri: String? = null) : NavigationKey

    @Serializable
    data object Settings : NavigationKey

    @Serializable
    data object About : NavigationKey

}

enum class BottomNavDestinations(
    val key: NavigationKey,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    DISCOVER(NavigationKey.Discover, R.string.menu_discover, Icons.Filled.Explore),
    MY_APPS(NavigationKey.MyApps, R.string.menu_apps_my, Icons.Filled.Apps),
}

sealed class NavDestinations(
    val id: NavigationKey,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    object Repos :
        NavDestinations(NavigationKey.Repos, R.string.app_details_repositories, PackageVariant)

    object Settings :
        NavDestinations(NavigationKey.Settings, R.string.menu_settings, Icons.Filled.Settings)

    object About : NavDestinations(NavigationKey.About, R.string.menu_about, Icons.Filled.Info)
}

val topBarMenuItems = listOf(
    NavDestinations.Repos,
    NavDestinations.Settings,
)

val moreMenuItems = listOf(
    NavDestinations.About,
)
