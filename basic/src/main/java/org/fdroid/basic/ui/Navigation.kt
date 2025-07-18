package org.fdroid.basic.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Explore
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.fdroid.basic.R
import org.fdroid.basic.ui.main.lists.AppListType

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
    data object Settings : NavigationKey

    @Serializable
    data object About : NavigationKey

}

enum class BottomNavDestinations(
    val key: NavigationKey,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    DISCOVER(NavigationKey.Discover, R.string.discover, Icons.Filled.Explore),
    MY_APPS(NavigationKey.MyApps, R.string.apps_my, Icons.Filled.Apps),
}
