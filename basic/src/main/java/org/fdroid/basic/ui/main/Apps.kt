package org.fdroid.basic.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole.Detail
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fdroid.basic.ui.main.apps.AppDetails
import org.fdroid.basic.ui.main.apps.AppList
import org.fdroid.basic.ui.main.apps.AppNavigationItem
import org.fdroid.basic.ui.main.apps.AppsFilter
import org.fdroid.basic.ui.main.apps.AppsSearch
import org.fdroid.basic.ui.main.apps.FilterInfo
import org.fdroid.basic.ui.main.apps.FilterModel
import org.fdroid.fdroid.ui.theme.FDroidContent

enum class Sort {
    NAME,
    LATEST,
}

const val NUM_ITEMS = 42

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun Apps(
    apps: List<AppNavigationItem>,
    filterInfo: FilterInfo,
    modifier: Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<AppNavigationItem>()
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
        listPane = {
            AnimatedPane {
                Column(
                    modifier.fillMaxSize()
                ) {
                    var filterExpanded by rememberSaveable { mutableStateOf(true) }
                    val addedRepos = remember { mutableStateListOf<String>() }
                    val showFilterBadge = addedRepos.isNotEmpty() ||
                        filterInfo.model.addedCategories.isNotEmpty() ||
                        filterInfo.model.onlyInstalledApps
                    AppsSearch(
                        showFilterBadge = showFilterBadge,
                        toggleFilter = { filterExpanded = !filterExpanded },
                    ) {
                        scope.launch { navigator.navigateTo(Detail, it) }
                    }
                    AppsFilter(
                        filterExpanded = filterExpanded,
                        filter = filterInfo,
                        addedRepos = addedRepos,
                    )
                    AppList(
                        apps = apps,
                        currentItem = if (isDetailVisible) {
                            navigator.currentDestination?.contentKey
                        } else {
                            null
                        },
                    ) {
                        scope.launch { navigator.navigateTo(Detail, it) }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.contentKey?.let {
                    AppDetails(appItem = it)
                } ?: Text("No app selected", modifier = Modifier.padding(16.dp))
            }
        },
    )
}

@Preview
@PreviewScreenSizes
@Composable
fun AppsPreview() {
    FDroidContent {
        val apps = listOf(
            AppNavigationItem("", "foo", "bar", false),
            AppNavigationItem("", "foo", "bar", false),
            AppNavigationItem("", "foo", "bar", false),
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
        Apps(apps, filterInfo, Modifier)
    }
}
