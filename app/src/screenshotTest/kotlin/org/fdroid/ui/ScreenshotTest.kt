package org.fdroid.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import org.fdroid.ui.navigation.NavigationKey

@Composable
fun ScreenshotTest(
    showBottomBar: Boolean = true,
    currentNavKey: NavKey = NavigationKey.Discover,
    numUpdates: Int = 3,
    hasAppIssues: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    MainContent(
        isBigScreen = false,
        dynamicColors = false,
        showBottomBar = showBottomBar,
        currentNavKey = currentNavKey,
        numUpdates = numUpdates,
        hasAppIssues = hasAppIssues,
        onNav = {},
        content = content,
    )
}
