package org.fdroid.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.viktormykhailiv.compose.hints.HintHost
import org.fdroid.ui.navigation.BottomBar
import org.fdroid.ui.navigation.MainNavKey
import org.fdroid.ui.navigation.NavigationRail

@Composable
fun MainContent(
  model: MainModel,
  isBigScreen: Boolean,
  showBottomBar: Boolean,
  currentNavKey: NavKey,
  onNav: (MainNavKey) -> Unit,
  content: @Composable (Modifier) -> Unit,
) =
  FDroidContent(dynamicColors = model.dynamicColors) {
    HintHost {
      Scaffold(
        bottomBar =
          if (showBottomBar) {
            { BottomBar(model = model, currentNavKey = currentNavKey, onNav = onNav) }
          } else {
            {}
          }
      ) { paddingValues ->
        Row {
          // show nav rail only on big screen (at least two partitions)
          if (isBigScreen)
            NavigationRail(
              numUpdates = model.numUpdates,
              hasIssues = model.hasAppIssues,
              currentNavKey = currentNavKey,
              onNav = onNav,
            )
          val modifier =
            if (isBigScreen) {
              // need to consume start insets or some phones leave a lot of space there
              Modifier.consumeWindowInsets(PaddingValues(start = 64.dp))
            } else if (showBottomBar) {
              // we only apply the bottom padding here, so content stays above bottom bar,
              // but we need to consume the navigation bar height manually
              val bottom =
                with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
              Modifier.consumeWindowInsets(PaddingValues(bottom = bottom))
                .padding(bottom = paddingValues.calculateBottomPadding())
            } else {
              Modifier
            }
          // this needs to a have a fixed place or state saving breaks,
          // so all moving pieces with conditionals are above
          content(modifier)
        }
      }
    }
  }
