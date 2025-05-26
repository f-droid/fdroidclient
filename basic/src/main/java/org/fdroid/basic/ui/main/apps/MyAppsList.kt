package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fdroid.basic.R
import org.fdroid.basic.ui.main.discover.Sort

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MyAppsList(
    updatableApps: List<UpdatableApp>,
    installedApps: List<InstalledApp>,
    currentItem: MinimalApp?,
    onItemClick: (MinimalApp) -> Unit,
    sortBy: Sort,
    onSortChanged: (Sort) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    var sortByMenuExpanded by remember { mutableStateOf(false) }
                    Column {
                        Text("My apps")
                        FilterChip(
                            selected = false,
                            leadingIcon = {
                                val vector = when (sortBy) {
                                    Sort.NAME -> Icons.Filled.SortByAlpha
                                    Sort.LATEST -> Icons.Filled.AccessTime
                                }
                                Icon(
                                    vector,
                                    null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, null)
                            },
                            label = {
                                val s = when (sortBy) {
                                    Sort.NAME -> "Sort by name"
                                    Sort.LATEST -> "Sort by latest"
                                }
                                Text(s)
                                DropdownMenu(
                                    expanded = sortByMenuExpanded,
                                    onDismissRequest = { sortByMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Sort by name") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.SortByAlpha, null)
                                        },
                                        onClick = {
                                            onSortChanged(Sort.NAME)
                                            sortByMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sort by latest") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.AccessTime, null)
                                        },
                                        onClick = {
                                            onSortChanged(Sort.LATEST)
                                            sortByMenuExpanded = false
                                        },
                                    )
                                }
                            },
                            onClick = { sortByMenuExpanded = !sortByMenuExpanded },
                        )
                    }
                },
                actions = {
                    if (updatableApps.isNotEmpty()) Button(
                        onClick = {},
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Text("Update all")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        LazyColumn(
            modifier
                .padding(paddingValues)
                .then(
                    if (currentItem == null) Modifier
                    else Modifier.selectableGroup()
                ),
        ) {
            if (updatableApps.isNotEmpty()) item(key = "A", contentType = "header") {
                Text(
                    text = stringResource(R.string.updates),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(updatableApps, key = { it.packageName }, contentType = { "A" }) { app ->
                val isSelected = app.packageName == currentItem?.packageName
                val interactionModifier = if (currentItem == null) {
                    Modifier.clickable(
                        onClick = { onItemClick(app) }
                    )
                } else {
                    Modifier.selectable(
                        selected = isSelected,
                        onClick = { onItemClick(app) }
                    )
                }
                val modifier = Modifier
                    .animateItem()
                    .then(interactionModifier)
                UpdatableAppRow(app, isSelected, modifier)
            }
            if (updatableApps.isNotEmpty()) item(key = "B", contentType = "header") {
                Text(
                    text = stringResource(R.string.installed_apps__activity_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(installedApps, key = { it.packageName }, contentType = { "B" }) { app ->
                val isSelected = app.packageName == currentItem?.packageName
                val interactionModifier = if (currentItem == null) {
                    Modifier.clickable(
                        onClick = { onItemClick(app) }
                    )
                } else {
                    Modifier.selectable(
                        selected = isSelected,
                        onClick = { onItemClick(app) }
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
