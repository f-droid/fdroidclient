package org.fdroid.ui

import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import org.fdroid.ui.navigation.MainNavKey
import org.fdroid.ui.navigation.NavigationKey

@Composable
fun ScreenshotTest(
  currentNavKey: NavKey = NavigationKey.Discover,
  showBottomBar: Boolean = currentNavKey is MainNavKey,
  numUpdates: Int = 3,
  hasAppIssues: Boolean = true,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides TestNavOwner) {
    MainContent(
      model =
        MainModel(
          dynamicColors = false,
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
}

private object TestNavOwner : NavigationEventDispatcherOwner {
  override val navigationEventDispatcher: NavigationEventDispatcher
    get() = NavigationEventDispatcher() // Empty dispatcher for rendering safely
}
