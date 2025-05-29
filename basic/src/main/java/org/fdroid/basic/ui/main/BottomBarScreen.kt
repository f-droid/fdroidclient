package org.fdroid.basic.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.window.core.layout.WindowWidthSizeClass
import org.fdroid.basic.R
import org.fdroid.basic.ui.main.apps.InstalledApp
import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.basic.ui.main.apps.MyAppsScaffold
import org.fdroid.basic.ui.main.apps.UpdatableApp
import org.fdroid.basic.ui.main.discover.AppNavigationItem
import org.fdroid.basic.ui.main.discover.DiscoverScaffold
import org.fdroid.basic.ui.main.discover.FilterModel
import org.fdroid.basic.ui.main.discover.Sort
import org.fdroid.basic.ui.main.lists.AppList
import org.fdroid.basic.ui.main.lists.FilterInfo

enum class BottomNavDestinations(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    APPS(R.string.discover, Icons.Filled.Explore),
    UPDATES(R.string.apps_my, Icons.Filled.Apps),
}

@Composable
fun BottomBarScreen(
    onMainNav: (String) -> Unit,
    numUpdates: Int,
    updates: List<UpdatableApp>,
    installed: List<InstalledApp>,
    appList: AppList,
    filterInfo: FilterInfo,
    currentItem: MinimalApp?,
    onSelectAppItem: (MinimalApp) -> Unit,
    sortBy: Sort,
    onSortChanged: (Sort) -> Unit,
    onAppListChanged: (AppList) -> Unit,
) {
    var currentDestination by rememberSaveable { mutableStateOf(BottomNavDestinations.APPS) }
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val customNavSuiteType = with(adaptiveInfo) {
        when (windowSizeClass.windowWidthSizeClass) {
            WindowWidthSizeClass.COMPACT -> NavigationSuiteType.NavigationBar
            else -> NavigationSuiteType.NavigationRail
        }
    }
    NavigationSuiteScaffold(
        modifier = Modifier.fillMaxSize(),
        layoutType = customNavSuiteType,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = Color.Transparent,
        ),
        navigationSuiteItems = {
            BottomNavDestinations.entries.forEach { dest ->
                item(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (dest == BottomNavDestinations.UPDATES && numUpdates > 0) {
                                    Badge {
                                        Text(text = numUpdates.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                dest.icon,
                                contentDescription = stringResource(dest.label)
                            )
                        }
                    },
                    label = { Text(stringResource(dest.label)) },
                    selected = dest == currentDestination,
                    onClick = { currentDestination = dest }
                )
            }
        }
    ) {
        when (currentDestination) {
            BottomNavDestinations.APPS -> DiscoverScaffold(
                appList = appList,
                apps = filterInfo.model.apps,
                filterInfo = filterInfo,
                onMainNav = onMainNav,
                currentItem = currentItem,
                onSelectAppItem = onSelectAppItem,
                onAppListChanged = onAppListChanged,
                modifier = Modifier,
            )
            BottomNavDestinations.UPDATES -> MyAppsScaffold(
                updatableApps = updates,
                installedApps = installed,
                currentItem = currentItem,
                onSelectAppItem = onSelectAppItem,
                sortBy = sortBy,
                onSortChanged = onSortChanged,
            )
        }
    }
}

@Preview
@PreviewScreenSizes
@Composable
fun BottomBarPreview() {
    val apps = listOf(
        AppNavigationItem("", "foo", null, "bar", false),
        AppNavigationItem("", "foo", null, "bar", false),
        AppNavigationItem("", "foo", null, "bar", false),
    )
    var filterExpanded by rememberSaveable { mutableStateOf(true) }
    val filterInfo = object : FilterInfo {
        override val model = FilterModel(
            isLoading = false,
            areFiltersShown = filterExpanded,
            apps = apps,
            sortBy = Sort.NAME,
            allCategories = listOf("foo", "bar"),
            addedCategories = emptyList(),
        )

        override fun toggleFilterVisibility() {}
        override fun sortBy(sort: Sort) {}
        override fun addCategory(category: String) {}
        override fun removeCategory(category: String) {
            filterExpanded = !filterExpanded
        }
    }
    BottomBarScreen(
        onMainNav = { },
        numUpdates = 2,
        updates = emptyList(),
        installed = emptyList(),
        appList = AppList.New,
        filterInfo = filterInfo,
        currentItem = null,
        onSelectAppItem = {},
        sortBy = Sort.NAME,
        onSortChanged = {},
        onAppListChanged = {},
    )
}
