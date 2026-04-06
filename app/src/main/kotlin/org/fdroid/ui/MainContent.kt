package org.fdroid.ui

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.viktormykhailiv.compose.hints.HintHost
import org.fdroid.ui.navigation.BottomBar
import org.fdroid.ui.navigation.MainNavKey
import org.fdroid.ui.navigation.NavigationRail
import org.fdroid.ui.navigation.rememberResponsiveNavigationSceneDecoratorStrategy

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun MainContent(
  model: MainModel,
  navEntries: List<NavEntry<NavKey>>,
  directive: PaneScaffoldDirective,
  isBigScreen: Boolean,
  showBottomBar: Boolean,
  currentNavKey: NavKey,
  onNav: (MainNavKey) -> Unit,
  onBack: () -> Unit,
) =
  FDroidContent(dynamicColors = model.dynamicColors) {
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    SharedTransitionLayout {
      HintHost {
        val responsiveNavigationSceneDecoratorStrategy =
          rememberResponsiveNavigationSceneDecoratorStrategy<NavKey>(
            isBigScreen = isBigScreen,
            navBar = {
              if (showBottomBar) {
                BottomBar(model = model, currentNavKey = currentNavKey, onNav = onNav)
              }
            },
            navRail = {
              NavigationRail(
                numUpdates = model.numUpdates,
                hasIssues = model.hasAppIssues,
                currentNavKey = currentNavKey,
                onNav = onNav,
              )
            },
            sharedTransitionScope = this,
          )
        NavDisplay(
          entries = navEntries,
          sceneDecoratorStrategies = listOf(responsiveNavigationSceneDecoratorStrategy),
          sceneStrategies = listOf(listDetailStrategy),
          onBack = onBack,
          modifier = Modifier,
        )
      }
    }
  }
