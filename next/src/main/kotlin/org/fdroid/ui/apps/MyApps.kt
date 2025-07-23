package org.fdroid.ui.apps

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation3.runtime.NavKey
import org.fdroid.database.AppListSortOrder
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.ui.BottomBar
import org.fdroid.ui.NavigationKey
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.Names
import org.fdroid.ui.utils.getPreviewVersion
import java.util.concurrent.TimeUnit.DAYS

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun MyApps(
    myAppsModel: MyAppsModel,
    currentPackageName: String?,
    onAppItemClick: (String) -> Unit,
    onNav: (NavKey) -> Unit,
    onSortChanged: (AppListSortOrder) -> Unit,
    onRefresh: () -> Unit,
    isBigScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    LifecycleStartEffect(myAppsModel) {
        onRefresh()
        onStopOrDispose { }
    }
    val updatableApps = myAppsModel.appUpdates
    val installedApps = myAppsModel.installedApps
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My apps")
                },
                actions = {
                    var sortByMenuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { sortByMenuExpanded = !sortByMenuExpanded }) {
                        Icon(Icons.Filled.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = sortByMenuExpanded,
                        onDismissRequest = { sortByMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by name") },
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
                                onSortChanged(AppListSortOrder.NAME)
                                sortByMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by latest") },
                            leadingIcon = {
                                Icon(Icons.Filled.AccessTime, null)
                            },
                            trailingIcon = {
                                RadioButton(
                                    selected = myAppsModel.sortOrder == AppListSortOrder.LAST_UPDATED,
                                    onClick = null,
                                )
                            },
                            onClick = {
                                onSortChanged(AppListSortOrder.LAST_UPDATED)
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
        if (updatableApps == null && installedApps == null) BigLoadingIndicator()
        else LazyColumn(
            modifier
                .padding(paddingValues)
                .then(
                    if (currentPackageName == null) Modifier
                    else Modifier.selectableGroup()
                ),
        ) {
            if (updatableApps == null || updatableApps.isNotEmpty()) {
                item(key = "A", contentType = "header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.updates),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(16.dp)
                                .weight(1f),
                        )
                        if (updatableApps?.isNotEmpty() == true) Button(
                            onClick = {},
                            modifier = Modifier.padding(end = 16.dp),
                        ) {
                            Text("Update all")
                        }
                    }
                }
            }
            if (updatableApps != null) items(
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
            if (!updatableApps.isNullOrEmpty() && !installedApps.isNullOrEmpty()) {
                item(key = "B", contentType = "header") {
                    Text(
                        text = stringResource(R.string.installed_apps__activity_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            if (installedApps != null) items(
                items = installedApps,
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
                val modifier = Modifier
                    .animateItem()
                    .then(interactionModifier)
                InstalledAppRow(app, isSelected, modifier)
            }
        }
    }
}

@Preview
@Composable
fun MyAppsLoadingPreview() {
    FDroidContent {
        MyApps(
            myAppsModel = MyAppsModel(
                appUpdates = null,
                installedApps = null,
                sortOrder = AppListSortOrder.NAME,
            ),
            currentPackageName = null,
            onAppItemClick = {},
            onNav = {},
            onSortChanged = { },
            isBigScreen = false,
            onRefresh = {},
        )
    }
}

@Preview
@Composable
@RestrictTo(RestrictTo.Scope.TESTS)
fun MyAppsPreview() {
    FDroidContent {
        val app1 = AppUpdateItem(
            packageName = "AX",
            name = "App Update 123",
            installedVersionName = "1.0.1",
            update = getPreviewVersion("1.1.0", 123456789),
            whatsNew = "This is new, all is new, nothing old.",
        )
        val app2 = AppUpdateItem(
            packageName = "BX",
            name = Names.randomName,
            installedVersionName = "3.0.1",
            update = getPreviewVersion("3.1.0", 9876543),
            whatsNew = null,
        )
        val installedApp1 = InstalledAppItem(
            packageName = "1",
            name = Names.randomName,
            installedVersionName = "1",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(1)
        )
        val installedApp2 = InstalledAppItem(
            packageName = "2",
            name = Names.randomName,
            installedVersionName = "2",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(2)
        )
        val installedApp3 = InstalledAppItem(
            packageName = "3",
            name = Names.randomName,
            installedVersionName = "3",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(3)
        )
        val model = MyAppsModel(
            appUpdates = listOf(app1, app2),
            installedApps = listOf(installedApp1, installedApp2, installedApp3),
            sortOrder = AppListSortOrder.NAME,
        )
        MyApps(
            myAppsModel = model,
            currentPackageName = null,
            onAppItemClick = {},
            onNav = {},
            onSortChanged = { },
            isBigScreen = false,
            onRefresh = {},
        )
    }
}
