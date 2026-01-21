package org.fdroid.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.viktormykhailiv.compose.hints.HintHost
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.ui.apps.myAppsEntry
import org.fdroid.ui.details.appDetailsEntry
import org.fdroid.ui.discover.discoverEntry
import org.fdroid.ui.lists.appListEntry
import org.fdroid.ui.navigation.BottomBar
import org.fdroid.ui.navigation.IntentRouter
import org.fdroid.ui.navigation.MainNavKey
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.NavigationRail
import org.fdroid.ui.navigation.Navigator
import org.fdroid.ui.navigation.rememberNavigationState
import org.fdroid.ui.navigation.toEntries
import org.fdroid.ui.navigation.topLevelRoutes
import org.fdroid.ui.repositories.repoEntry
import org.fdroid.ui.settings.Settings
import org.fdroid.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Main(onListeningForIntent: () -> Unit = {}) {
    val navigationState = rememberNavigationState(
        startRoute = NavigationKey.Discover,
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember { Navigator(navigationState) }
    // set up intent routing by listening to new intents from activity
    val activity = (LocalActivity.current as ComponentActivity)
    DisposableEffect(navigator) {
        val intentListener = IntentRouter(navigator)
        activity.addOnNewIntentListener(intentListener)
        onListeningForIntent() // call this to get informed about initial intents we have missed
        onDispose { activity.removeOnNewIntentListener(intentListener) }
    }
    // Override the defaults so that there isn't a horizontal space between the panes.
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 2.dp)
    }
    val isBigScreen = directive.maxHorizontalPartitions > 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        discoverEntry(navigator)
        myAppsEntry(navigator, isBigScreen)
        appDetailsEntry(navigator, isBigScreen)
        appListEntry(navigator, isBigScreen)
        repoEntry(navigator, isBigScreen)
        entry(NavigationKey.Settings) {
            val viewModel = hiltViewModel<SettingsViewModel>()
            Settings(
                model = viewModel.model,
                onSaveLogcat = {
                    viewModel.onSaveLogcat(it)
                    navigator.goBack()
                },
                onBackClicked = { navigator.goBack() },
            )
        }
        entry(
            key = NavigationKey.About,
            metadata = ListDetailSceneStrategy.detailPane("appdetails"),
        ) {
            About(
                onBackClicked = if (isBigScreen) null else {
                    { navigator.goBack() }
                },
            )
        }
    }
    val showBottomBar = !isBigScreen && navigator.last is MainNavKey
    val viewModel = hiltViewModel<MainViewModel>()
    val dynamicColors =
        viewModel.dynamicColors.collectAsStateWithLifecycle(PREF_DEFAULT_DYNAMIC_COLORS).value
    val numUpdates = viewModel.numUpdates.collectAsStateWithLifecycle().value
    val hasAppIssues = viewModel.hasAppIssues.collectAsStateWithLifecycle(false).value
    FDroidContent(dynamicColors = dynamicColors) {
        HintHost {
            Scaffold(
                bottomBar = if (showBottomBar) {
                    {
                        BottomBar(
                            numUpdates = numUpdates,
                            hasIssues = hasAppIssues,
                            currentNavKey = navigationState.topLevelRoute,
                            onNav = { navKey -> navigator.navigate(navKey) },
                        )
                    }
                } else {
                    {}
                },
            ) { paddingValues ->
                Row {
                    // show nav rail only on big screen (at least two partitions)
                    if (isBigScreen) NavigationRail(
                        numUpdates = numUpdates,
                        hasIssues = hasAppIssues,
                        currentNavKey = navigationState.topLevelRoute,
                        onNav = { navKey -> navigator.navigate(navKey) },
                    )
                    val modifier = if (isBigScreen) {
                        // need to consume start insets or some phones leave a lot of space there
                        Modifier.consumeWindowInsets(PaddingValues(start = 64.dp))
                    } else if (showBottomBar) {
                        // we only apply the bottom padding here, so content stays above bottom bar
                        // but we need to consume the navigation bar height manually
                        val bottom = with(LocalDensity.current) {
                            WindowInsets.navigationBars.getBottom(this).toDp()
                        }
                        Modifier
                            .consumeWindowInsets(PaddingValues(bottom = bottom))
                            .padding(bottom = paddingValues.calculateBottomPadding())
                    } else {
                        Modifier
                    }
                    // this needs to a have a fixed place or state saving breaks,
                    // so all moving pieces with conditionals are above
                    NavDisplay(
                        entries = navigationState.toEntries(entryProvider),
                        sceneStrategy = listDetailStrategy,
                        onBack = { navigator.goBack() },
                        modifier = modifier,
                    )
                }
            }
        }
    }
}
