package org.fdroid.basic.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import kotlinx.coroutines.launch
import org.fdroid.basic.R
import org.fdroid.basic.ui.main.apps.AppDetails
import org.fdroid.basic.ui.main.apps.AppList
import org.fdroid.basic.ui.main.apps.AppNavigationItem
import org.fdroid.basic.ui.main.apps.AppsFilter
import org.fdroid.basic.ui.main.apps.AppsSearch
import org.fdroid.fdroid.ui.theme.FDroidContent

enum class Sort {
    NAME,
    LATEST,
}

const val NUM_ITEMS = 42

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun Apps(modifier: Modifier) {
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
                    var sortBy by rememberSaveable { mutableStateOf(Sort.NAME) }
                    var onlyInstalledApps by rememberSaveable { mutableStateOf(false) }
                    val addedCategories = remember { mutableStateListOf<String>() }
                    val addedRepos = remember { mutableStateListOf<String>() }
                    val categories = listOf(
                        stringResource(R.string.category_Time),
                        stringResource(R.string.category_Games),
                        stringResource(R.string.category_Money),
                        stringResource(R.string.category_Reading),
                        stringResource(R.string.category_Theming),
                        stringResource(R.string.category_Connectivity),
                        stringResource(R.string.category_Internet),
                        stringResource(R.string.category_Navigation),
                        stringResource(R.string.category_Multimedia),
                        stringResource(R.string.category_Phone_SMS),
                        stringResource(R.string.category_Science_Education),
                        stringResource(R.string.category_Security),
                        stringResource(R.string.category_Sports_Health),
                        stringResource(R.string.category_System),
                        stringResource(R.string.category_Writing),
                    )
                    AppsSearch(
                        onlyInstalledApps = onlyInstalledApps,
                        addedCategories = addedCategories,
                        addedRepos = addedRepos,
                        toggleFilter = { filterExpanded = !filterExpanded },
                    )
                    AppsFilter(
                        filterExpanded = filterExpanded,
                        sortBy = sortBy,
                        onlyInstalledApps = onlyInstalledApps,
                        addedCategories = addedCategories,
                        addedRepos = addedRepos,
                        categories = categories,
                        onSortByChanged = { sortBy = it },
                        toggleOnlyInstalledApps = {
                            onlyInstalledApps = !onlyInstalledApps
                        },
                    )
                    AppList(
                        onlyInstalledApps = onlyInstalledApps,
                        sortBy = sortBy,
                        addedCategories = addedCategories,
                        categories = categories,
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
                    AppDetails(
                        appItem = it,
                    )
                }
            }
        },
    )
}

@Preview
@PreviewScreenSizes
@Composable
fun AppsPreview() {
    FDroidContent {
        Apps(Modifier)
    }
}
