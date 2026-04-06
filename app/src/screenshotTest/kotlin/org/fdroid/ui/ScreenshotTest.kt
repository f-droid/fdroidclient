package org.fdroid.ui

import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import org.fdroid.ui.navigation.MainNavKey
import org.fdroid.ui.navigation.NavigationKey

@Composable
fun ScreenshotTest(
  currentNavKey: NavKey = NavigationKey.Discover,
  showBottomBar: Boolean = currentNavKey is MainNavKey,
  smallBottomBar: Boolean = false,
  numUpdates: Int = 3,
  hasAppIssues: Boolean = true,
  content: @Composable () -> Unit,
) {
  MainContent(
    model =
      MainModel(
        dynamicColors = false,
        smallBottomBar = smallBottomBar,
        numUpdates = numUpdates,
        hasAppIssues = hasAppIssues,
      ),
    navEntries = listOf(NavEntry(currentNavKey) { content() }),
    directive = PaneScaffoldDirective.Default,
    isBigScreen = false,
    showBottomBar = showBottomBar,
    currentNavKey = currentNavKey,
    onNav = {},
    onBack = {},
  )
}
