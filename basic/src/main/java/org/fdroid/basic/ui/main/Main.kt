package org.fdroid.basic.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
import org.fdroid.fdroid.ui.theme.FDroidContent

enum class AppDestinations(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    APPS(R.string.apps, Icons.Filled.Apps),
    UPDATES(R.string.updates, Icons.Filled.Update),
}

@Composable
fun Main() {
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
                                    if (dest == AppDestinations.UPDATES) {
                                        Badge {
                                            Text(text = "13")
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
            if (currentDestination == AppDestinations.APPS) Apps(Modifier)
            else Text(
                text = "TODO",
                modifier = Modifier.safeDrawingPadding(),
            )
        }
    }
}

@Preview
@PreviewScreenSizes
@Composable
fun MainPreview() {
    Main()
}
