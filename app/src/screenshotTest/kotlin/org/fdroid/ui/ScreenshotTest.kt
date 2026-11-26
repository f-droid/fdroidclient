package org.fdroid.ui

import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import org.fdroid.download.NetworkState
import org.fdroid.ui.navigation.MainNavKey
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.mainOnboardingState

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
          mainOnboardingState = mainOnboardingState,
          networkState = NetworkState(isOnline = true, isMetered = false),
          numUpdates = numUpdates,
          hasAppIssues = hasAppIssues,
          showOnboarding = false,
        ),
      navEntries = listOf(NavEntry(currentNavKey) { content() }),
      directive = PaneScaffoldDirective.Default,
      isBigScreen = false,
      showBottomBar = showBottomBar,
      currentNavKey = currentNavKey,
      onOnboardingSeen = {},
      onNav = {},
      onBack = {},
    )
  }
}

private object TestNavOwner : NavigationEventDispatcherOwner {
  override val navigationEventDispatcher: NavigationEventDispatcher
    get() = NavigationEventDispatcher() // Empty dispatcher for rendering safely
}
