package org.fdroid.basic.ui.main.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole.Detail
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fdroid.basic.ui.main.details.AppDetails
import org.fdroid.basic.ui.main.discover.Names
import org.fdroid.fdroid.ui.theme.FDroidContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MyAppsScaffold(
    updatableApps: List<UpdatableApp>,
    installedApps: List<InstalledApp>,
    currentItem: MinimalApp?,
    onSelectAppItem: (MinimalApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<MinimalApp>()
    val scope = rememberCoroutineScope()
    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }
    val isDetailVisible = navigator.scaffoldValue[Detail] == PaneAdaptedValue.Expanded
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                MyAppsList(
                    updatableApps = updatableApps,
                    installedApps = installedApps,
                    currentItem = if (isDetailVisible) currentItem else null,
                    onItemClick = {
                        onSelectAppItem(it)
                        scope.launch { navigator.navigateTo(Detail, it) }
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.contentKey?.let {
                    AppDetails(it)
                } ?: Text("No app selected", modifier = Modifier.padding(16.dp))
            }
        },
    )
}

@Preview
@PreviewScreenSizes
@Composable
fun MyAppsScaffoldPreview() {
    FDroidContent {
        val app1 = UpdatableApp(
            packageName = "",
            name = Names.randomName,
            currentVersionName = "1.0.1",
            updateVersionName = "1.1.0",
            size = 123456789,
        )
        val app2 = UpdatableApp(
            packageName = "",
            name = Names.randomName,
            currentVersionName = "3.0.1",
            updateVersionName = "3.1.0",
            size = 9876543,
        )
        val installedApp1 = InstalledApp("", Names.randomName, "3.0.1")
        val installedApp2 = InstalledApp("", Names.randomName, "1.0")
        val installedApp3 = InstalledApp("", Names.randomName, "0.1")
        MyAppsScaffold(
            updatableApps = listOf(app1, app2),
            installedApps = listOf(installedApp1, installedApp2, installedApp3),
            currentItem = null,
            onSelectAppItem = {},
        )
    }
}
