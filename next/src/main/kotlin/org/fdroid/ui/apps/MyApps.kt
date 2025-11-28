package org.fdroid.ui.apps

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.AppListSortOrder.LAST_UPDATED
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.install.InstallConfirmationState
import org.fdroid.ui.BottomBar
import org.fdroid.ui.NavigationKey
import org.fdroid.ui.lists.TopSearchBar
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.getMyAppsInfo
import org.fdroid.ui.utils.myAppsModel

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun MyApps(
    myAppsInfo: MyAppsInfo,
    currentPackageName: String?,
    onAppItemClick: (String) -> Unit,
    onNav: (NavKey) -> Unit,
    isBigScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    val myAppsModel = myAppsInfo.model
    val appToConfirm by remember(myAppsInfo.model.installingApps) {
        derivedStateOf {
            myAppsInfo.model.installingApps.find { app ->
                app.installState is InstallConfirmationState
            }
        }
    }
    LifecycleStartEffect(Unit) {
        myAppsInfo.refresh()
        onStopOrDispose { }
    }
    // Ask user to confirm appToConfirm whenever it changes and we are in STARTED state.
    // In tests, waiting for RESUME didn't work, because the LaunchedEffect ran before.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(appToConfirm) {
        val app = appToConfirm
        if (app != null && lifecycleOwner.lifecycle.currentState.isAtLeast(STARTED)) {
            val state = app.installState as InstallConfirmationState
            myAppsInfo.confirmAppInstall(app.packageName, state)
        }
    }
    val installingApps = myAppsModel.installingApps
    val updatableApps = myAppsModel.appUpdates
    val installedApps = myAppsModel.installedApps
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val onSearchCleared = { myAppsInfo.search("") }
    // when search bar is shown, back button closes it again
    BackHandler(enabled = searchActive) {
        searchActive = false
        onSearchCleared()
    }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        topBar = {
            if (searchActive) {
                TopSearchBar(onSearch = myAppsInfo::search, onSearchCleared) {
                    onBackPressedDispatcher?.onBackPressed()
                }
            } else TopAppBar(
                title = {
                    Text(stringResource(R.string.menu_apps_my))
                },
                actions = {
                    IconButton(onClick = { searchActive = true }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.menu_search),
                        )
                    }
                    var sortByMenuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { sortByMenuExpanded = !sortByMenuExpanded }) {
                        Icon(Icons.Filled.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = sortByMenuExpanded,
                        onDismissRequest = { sortByMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_name)) },
                            leadingIcon = {
                                Icon(Icons.Filled.SortByAlpha, null)
                            },
                            trailingIcon = {
                                RadioButton(
                                    selected = myAppsModel.sortOrder == AppListSortOrder.NAME,
                                    onClick = null,
                                )
                            },
                            onClick = {
                                myAppsInfo.changeSortOrder(AppListSortOrder.NAME)
                                sortByMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_latest)) },
                            leadingIcon = {
                                Icon(Icons.Filled.AccessTime, null)
                            },
                            trailingIcon = {
                                RadioButton(
                                    selected = myAppsModel.sortOrder == LAST_UPDATED,
                                    onClick = null,
                                )
                            },
                            onClick = {
                                myAppsInfo.changeSortOrder(LAST_UPDATED)
                                sortByMenuExpanded = false
                            },
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (!isBigScreen) BottomBar(
                numUpdates = updatableApps?.size ?: 0,
                hasIssues = !myAppsModel.appsWithIssue.isNullOrEmpty(),
                currentNavKey = NavigationKey.MyApps,
                onNav = onNav,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()
        if (updatableApps == null && installedApps == null) BigLoadingIndicator()
        else if (installingApps.isEmpty() &&
            updatableApps.isNullOrEmpty() &&
            installedApps.isNullOrEmpty()
        ) {
            Text(
                text = if (searchActive) {
                    stringResource(R.string.search_my_apps_no_results)
                } else {
                    stringResource(R.string.my_apps_empty)
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
            )
        } else {
            MyAppsList(
                myAppsInfo = myAppsInfo,
                currentPackageName = currentPackageName,
                lazyListState = lazyListState,
                onAppItemClick = onAppItemClick,
                modifier = modifier.padding(paddingValues),
            )
        }
    }
}

@Preview
@Composable
fun MyAppsLoadingPreview() {
    val model = MyAppsModel(
        installingApps = emptyList(),
        appUpdates = null,
        installedApps = null,
        sortOrder = AppListSortOrder.NAME,
    )
    FDroidContent {
        MyApps(
            myAppsInfo = getMyAppsInfo(model),
            currentPackageName = null,
            onAppItemClick = {},
            onNav = {},
            isBigScreen = false,
        )
    }
}

@Preview
@Composable
@RestrictTo(RestrictTo.Scope.TESTS)
fun MyAppsPreview() {
    FDroidContent {
        MyApps(
            myAppsInfo = getMyAppsInfo(myAppsModel),
            currentPackageName = null,
            onAppItemClick = {},
            onNav = {},
            isBigScreen = false,
        )
    }
}
