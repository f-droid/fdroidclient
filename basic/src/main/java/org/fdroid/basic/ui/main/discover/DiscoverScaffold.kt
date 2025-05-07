package org.fdroid.basic.ui.main.discover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.basic.ui.main.details.AppDetails
import org.fdroid.basic.ui.main.lists.AppList
import org.fdroid.fdroid.ui.theme.FDroidContent

enum class Sort {
    NAME,
    LATEST,
}

const val NUM_ITEMS = 42

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
fun DiscoverScaffold(
    apps: List<AppNavigationItem>,
    filterInfo: FilterInfo,
    onMainNav: (String) -> Unit,
    onSelectAppItem: (MinimalApp) -> Unit,
    currentItem: MinimalApp?,
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
    val listState = rememberLazyListState()
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                val discoverNavController = rememberNavController()
                NavHost(navController = discoverNavController, startDestination = "discover") {
                    composable("discover") {
                        Discover(
                            apps = apps,
                            onTitleTap = { discoverNavController.navigate("list") },
                            onAppTap = {
                                onSelectAppItem(it)
                                scope.launch { navigator.navigateTo(Detail, it) }
                            },
                            onMainNav = { onMainNav(it) },
                            modifier = Modifier,
                        )
                    }
                    composable("list") {
                        AppList(
                            listState = listState,
                            apps = apps,
                            filterInfo = filterInfo,
                            currentItem = if (isDetailVisible) currentItem else null,
                            onBackClicked = { discoverNavController.popBackStack() },
                            modifier = Modifier,
                        ) {
                            onSelectAppItem(it)
                            scope.launch { navigator.navigateTo(Detail, it) }
                        }
                    }
                }
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
fun DiscoverScaffoldPreview() {
    FDroidContent {
        val apps = listOf(
            AppNavigationItem("1", "foo", "bar", false),
            AppNavigationItem("2", "foo", "bar", false),
            AppNavigationItem("3", "foo", "bar", false),
        )
        val filterInfo = object : FilterInfo {
            override val model = FilterModel(
                isLoading = false,
                apps = apps,
                onlyInstalledApps = false,
                sortBy = Sort.NAME,
                allCategories = listOf("foo", "bar"),
                addedCategories = emptyList(),
            )

            override fun sortBy(sort: Sort) {}
            override fun addCategory(category: String) {}
            override fun removeCategory(category: String) {}
            override fun showOnlyInstalledApps(onlyInstalled: Boolean) {}
        }
        DiscoverScaffold(apps, filterInfo, {}, {}, null)
    }
}
