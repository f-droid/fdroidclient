package org.fdroid.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import org.fdroid.ui.navigation.NavigationKey

@Composable
fun ScreenshotTest(
  showBottomBar: Boolean = true,
  smallBottomBar: Boolean = false,
  currentNavKey: NavKey = NavigationKey.Discover,
  numUpdates: Int = 3,
  hasAppIssues: Boolean = true,
  content: @Composable (Modifier) -> Unit,
) {
  MainContent(
    model =
      MainModel(
        dynamicColors = false,
        smallBottomBar = smallBottomBar,
        numUpdates = numUpdates,
        hasAppIssues = hasAppIssues,
      ),
    isBigScreen = false,
    showBottomBar = showBottomBar,
    currentNavKey = currentNavKey,
    onNav = {},
    content = content,
  )
}
