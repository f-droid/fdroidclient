package org.fdroid.ui.navigation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.fdroid.R
import org.fdroid.ui.icons.PackageVariant
import org.fdroid.ui.lists.AppListType

sealed interface NavigationKey : NavKey {

    @Serializable
    data object Discover : NavigationKey, MainNavKey {
        override val label: Int = R.string.menu_discover
        override val icon: ImageVector = Icons.Filled.Explore
    }

    @Serializable
    data object MyApps : NavigationKey, MainNavKey {
        override val label: Int = R.string.menu_apps_my
        override val icon: ImageVector = Icons.Filled.Apps
    }

    @Serializable
    data object Search : NavigationKey

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

    @Serializable
    data object InstallationHistory : NavigationKey

}

sealed interface MainNavKey : NavKey {
    @get:StringRes
    val label: Int
    val icon: ImageVector
}

val topLevelRoutes = listOf<MainNavKey>(
    NavigationKey.Discover,
    NavigationKey.MyApps,
)

sealed class NavDestinations(
    val id: NavigationKey,
    @param:StringRes val label: Int,
    val icon: ImageVector,
) {
    object Repos :
        NavDestinations(NavigationKey.Repos, R.string.app_details_repositories, PackageVariant)

    object Settings :
        NavDestinations(NavigationKey.Settings, R.string.menu_settings, Icons.Filled.Settings)

    class AllApps(title: String) : NavDestinations(
        id = NavigationKey.AppList(AppListType.All(title)),
        label = R.string.app_list_all,
        icon = Icons.Filled.Apps,
    )

    object About : NavDestinations(NavigationKey.About, R.string.menu_about, Icons.Filled.Info)
}

val topBarMenuItems = listOf(
    NavDestinations.Repos,
    NavDestinations.Settings,
)

fun getMoreMenuItems(context: Context) = listOf(
    NavDestinations.AllApps(context.getString(R.string.app_list_all)),
    NavDestinations.About,
)
