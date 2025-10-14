package org.fdroid.ui.apps

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
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
import org.fdroid.install.InstallState
import org.fdroid.ui.BottomBar
import org.fdroid.ui.NavigationKey
import org.fdroid.ui.lists.TopSearchBar
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.Names
import org.fdroid.ui.utils.getMyAppsInfo
import org.fdroid.ui.utils.getPreviewVersion
import java.util.concurrent.TimeUnit.DAYS

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
                app.installState is InstallState.UserConfirmationNeeded
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
            val state = app.installState as InstallState.UserConfirmationNeeded
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
            if (!isBigScreen) BottomBar(updatableApps?.size ?: 0, NavigationKey.MyApps, onNav)
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
            LazyColumn(
                state = lazyListState,
                modifier = modifier
                    .padding(paddingValues)
                    .then(
                        if (currentPackageName == null) Modifier
                        else Modifier.selectableGroup()
                    ),
            ) {
                // Updates header with Update all button (only show when there's a list below)
                if (!updatableApps.isNullOrEmpty()) {
                    item(key = "A", contentType = "header") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.updates),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .weight(1f),
                            )
                            Button(
                                onClick = myAppsInfo::updateAll,
                                modifier = Modifier.padding(end = 16.dp),
                            ) {
                                Text(stringResource(R.string.update_all))
                            }
                        }
                    }
                    // List of updatable apps
                    items(
                        items = updatableApps,
                        key = { it.packageName },
                        contentType = { "A" },
                    ) { app ->
                        val isSelected = app.packageName == currentPackageName
                        val interactionModifier = if (currentPackageName == null) {
                            Modifier.clickable(
                                onClick = { onAppItemClick(app.packageName) }
                            )
                        } else {
                            Modifier.selectable(
                                selected = isSelected,
                                onClick = { onAppItemClick(app.packageName) }
                            )
                        }
                        val modifier = Modifier.Companion
                            .animateItem()
                            .then(interactionModifier)
                        UpdatableAppRow(app, isSelected, modifier)
                    }
                }
                // Apps currently installing header
                if (installingApps.isNotEmpty()) {
                    item(key = "B", contentType = "header") {
                        Text(
                            text = stringResource(R.string.notification_title_summary_installing),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }
                    // List of currently installing apps
                    items(
                        items = installingApps,
                        key = { it.packageName },
                        contentType = { "B" },
                    ) { app ->
                        val isSelected = app.packageName == currentPackageName
                        val interactionModifier = if (currentPackageName == null) {
                            Modifier.clickable(
                                onClick = { onAppItemClick(app.packageName) }
                            )
                        } else {
                            Modifier.selectable(
                                selected = isSelected,
                                onClick = { onAppItemClick(app.packageName) }
                            )
                        }
                        val modifier = Modifier.Companion
                            .animateItem()
                            .then(interactionModifier)
                        InstallingAppRow(app, isSelected, modifier)
                    }
                }
                // Installed apps header (only show when we have non-empty lists above)
                if ((installingApps.isNotEmpty() || !updatableApps.isNullOrEmpty()) &&
                    !installedApps.isNullOrEmpty()
                ) {
                    item(key = "C", contentType = "header") {
                        Text(
                            text = stringResource(R.string.installed_apps__activity_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                // List of installed apps
                if (installedApps != null) items(
                    items = installedApps,
                    key = { it.packageName },
                    contentType = { "C" },
                ) { app ->
                    val isSelected = app.packageName == currentPackageName
                    val interactionModifier = if (currentPackageName == null) {
                        Modifier.clickable(
                            onClick = { onAppItemClick(app.packageName) }
                        )
                    } else {
                        Modifier.selectable(
                            selected = isSelected,
                            onClick = { onAppItemClick(app.packageName) }
                        )
                    }
                    val modifier = Modifier
                        .animateItem()
                        .then(interactionModifier)
                    InstalledAppRow(app, isSelected, modifier)
                }
            }
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
        val installingApp1 = InstallingAppItem(
            packageName = "A1",
            installState = InstallState.Downloading(
                name = "Installing App 1",
                versionName = "1.0.4",
                currentVersionName = null,
                lastUpdated = 23,
                iconDownloadRequest = null,
                downloadedBytes = 25,
                totalBytes = 100,
                startMillis = System.currentTimeMillis(),
            )
        )
        val app1 = AppUpdateItem(
            repoId = 1,
            packageName = "B1",
            name = "App Update 123",
            installedVersionName = "1.0.1",
            update = getPreviewVersion("1.1.0", 123456789),
            whatsNew = "This is new, all is new, nothing old.",
        )
        val app2 = AppUpdateItem(
            repoId = 2,
            packageName = "B2",
            name = Names.randomName,
            installedVersionName = "3.0.1",
            update = getPreviewVersion("3.1.0", 9876543),
            whatsNew = null,
        )
        val installedApp1 = InstalledAppItem(
            packageName = "C1",
            name = Names.randomName,
            installedVersionName = "1",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(1)
        )
        val installedApp2 = InstalledAppItem(
            packageName = "C2",
            name = Names.randomName,
            installedVersionName = "2",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(2)
        )
        val installedApp3 = InstalledAppItem(
            packageName = "C3",
            name = Names.randomName,
            installedVersionName = "3",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(3)
        )
        val model = MyAppsModel(
            installingApps = listOf(installingApp1),
            appUpdates = listOf(app1, app2),
            installedApps = listOf(installedApp1, installedApp2, installedApp3),
            sortOrder = AppListSortOrder.NAME,
        )
        MyApps(
            myAppsInfo = getMyAppsInfo(model),
            currentPackageName = null,
            onAppItemClick = {},
            onNav = {},
            isBigScreen = false,
        )
    }
}
