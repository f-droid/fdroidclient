package org.fdroid.ui

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.viktormykhailiv.compose.hints.hintAnchor
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import org.fdroid.ui.navigation.BottomBar
import org.fdroid.ui.navigation.NavigationRail
import org.fdroid.ui.navigation.rememberResponsiveNavigationSceneDecoratorStrategy
import org.fdroid.ui.utils.HintOverlayContainer

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun MainContent(
  model: MainModel,
  navEntries: List<NavEntry<NavKey>>,
  directive: PaneScaffoldDirective,
  isBigScreen: Boolean,
  showBottomBar: Boolean,
  currentNavKey: NavKey,
  onOnboardingSeen: () -> Unit,
  onNav: (NavKey) -> Unit,
  onBack: () -> Unit,
) =
  FDroidContent(dynamicColors = model.dynamicColors) {
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    SharedTransitionLayout {
      HintOverlayContainer {
        val hintController = rememberHintController()
        val onOnboardingSeen = {
          hintController.dismiss()
          onOnboardingSeen()
        }
        val hintAnchor = rememberHintAnchorState(getMainOnboardingHint(onOnboardingSeen, onNav))
        LaunchedEffect(model.showOnboarding) {
          if (model.showOnboarding) {
            hintController.show(hintAnchor)
          }
        }
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
          transitionSpec = {
            if (showBottomBar) fadeIn(tween()) togetherWith fadeOut(tween())
            else {
              slideInHorizontally { it } togetherWith
                slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.7f) + fadeOut()
            }
          },
          popTransitionSpec = {
            if (showBottomBar) fadeIn(tween()) togetherWith fadeOut(tween())
            else {
              slideInHorizontally { -it / 2 } +
                scaleIn(initialScale = 0.7f) +
                fadeIn(tween()) togetherWith slideOutHorizontally { it }
            }
          },
          predictivePopTransitionSpec = {
            if (showBottomBar) fadeIn(tween()) togetherWith fadeOut(tween())
            else {
              slideInHorizontally { -it / 2 } +
                scaleIn(initialScale = 0.7f) +
                fadeIn(tween()) togetherWith slideOutHorizontally { it }
            }
          },
          onBack = onBack,
          modifier = Modifier.hintAnchor(hintAnchor, fullScreen = true),
        )
      }
    }
  }
