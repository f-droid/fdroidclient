package org.fdroid.basic.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Update
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
import org.fdroid.basic.ui.main.apps.AppNavigationItem
import org.fdroid.basic.ui.main.apps.FilterInfo
import org.fdroid.basic.ui.main.apps.FilterModel
import org.fdroid.basic.ui.main.updates.UpdatableApp
import org.fdroid.fdroid.ui.theme.FDroidContent

enum class AppDestinations(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    APPS(R.string.apps, Icons.Filled.Apps),
    UPDATES(R.string.updates, Icons.Filled.Update),
}

@Composable
fun Main(numUpdates: Int, updates: List<UpdatableApp>, filterInfo: FilterInfo) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.APPS) }
    FDroidContent {
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
                AppDestinations.entries.forEach { dest ->
                    item(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (dest == AppDestinations.UPDATES && numUpdates > 0) {
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
            if (currentDestination == AppDestinations.APPS) Apps(
                apps = filterInfo.model.apps,
                filterInfo = filterInfo,
                modifier = Modifier,
            )
            else Updates(
                apps = updates,
            )
        }
    }
}

@Preview
@PreviewScreenSizes
@Composable
fun MainPreview() {
    val apps = listOf(
        AppNavigationItem("", "foo", "bar", false),
        AppNavigationItem("", "foo", "bar", false),
        AppNavigationItem("", "foo", "bar", false),
    )
    val filterInfo = object : FilterInfo {
        override val model = FilterModel(
            isLoading = false,
            apps = apps,
            onlyInstalledApps = false,
            sortBy = Sort.NAME,
            allCategories = listOf("foo", "bar"),
            addedCategories = emptyList(),
        )

        override fun sortBy(sort: Sort) {}
        override fun addCategory(category: String) {}
        override fun removeCategory(category: String) {}
        override fun showOnlyInstalledApps(onlyInstalled: Boolean) {}
    }
    Main(2, emptyList(), filterInfo)
}
