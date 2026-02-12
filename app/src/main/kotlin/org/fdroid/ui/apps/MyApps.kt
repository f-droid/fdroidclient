package org.fdroid.ui.apps

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.AppListSortOrder.LAST_UPDATED
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallConfirmationState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.search.TopSearchBar
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.getMyAppsInfo
import org.fdroid.ui.utils.myAppsModel

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun MyApps(
    myAppsInfo: MyAppsInfo,
    currentPackageName: String?,
    onAppItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val myAppsModel = myAppsInfo.model
    // Ask user to confirm appToConfirm whenever it changes and we are in STARTED state.
    // In tests, waiting for RESUME didn't work, because the LaunchedEffect ran before.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(myAppsModel.appToConfirm) {
        val app = myAppsModel.appToConfirm
        if (app != null && lifecycleOwner.lifecycle.currentState.isAtLeast(STARTED)) {
            val state = app.installState as InstallConfirmationState
            myAppsInfo.actions.confirmAppInstall(app.packageName, state)
        }
    }
    val installingApps = myAppsModel.installingApps
    val updatableApps = myAppsModel.appUpdates
    val appsWithIssue = myAppsModel.appsWithIssue
    val installedApps = myAppsModel.installedApps
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val onSearchCleared = { myAppsInfo.actions.search("") }
    // when search bar is shown, back button closes it again
    BackHandler(enabled = searchActive) {
        searchActive = false
        onSearchCleared()
    }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        topBar = {
            if (searchActive) {
                TopSearchBar(
                    onSearch = myAppsInfo.actions::search,
                    onSearchCleared = onSearchCleared,
                ) {
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Sort,
                            contentDescription = stringResource(R.string.more),
                        )
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
                                myAppsInfo.actions.changeSortOrder(AppListSortOrder.NAME)
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
                                myAppsInfo.actions.changeSortOrder(LAST_UPDATED)
                                sortByMenuExpanded = false
                            },
                        )
                    }
                    if (myAppsModel.installedApps != null) {
                        IconButton(onClick = myAppsInfo.actions::exportInstalledApps) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.menu_share),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()
        if (updatableApps == null && installedApps == null) BigLoadingIndicator()
        else if (installingApps.isEmpty() &&
            updatableApps.isNullOrEmpty() &&
            appsWithIssue.isNullOrEmpty() &&
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
                paddingValues = paddingValues,
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
        showAppIssueHint = false,
        sortOrder = AppListSortOrder.NAME,
        networkState = NetworkState(isOnline = false, isMetered = false),
    )
    FDroidContent {
        MyApps(
            myAppsInfo = getMyAppsInfo(model),
            currentPackageName = null,
            onAppItemClick = {},
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
        )
    }
}

@Preview
@Composable
@RestrictTo(RestrictTo.Scope.TESTS)
fun MyAppsEmptyPreview() {
    FDroidContent {
        val model = MyAppsModel(
            installingApps = emptyList(),
            appUpdates = emptyList(),
            installedApps = emptyList(),
            showAppIssueHint = false,
            sortOrder = AppListSortOrder.NAME,
            networkState = NetworkState(isOnline = false, isMetered = false),
        )
        MyApps(
            myAppsInfo = getMyAppsInfo(model),
            currentPackageName = null,
            onAppItemClick = {},
        )
    }
}
