package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MyAppsList(
    updatableApps: List<UpdatableApp>,
    installedApps: List<InstalledApp>,
    currentItem: MinimalApp?,
    onItemClick: (MinimalApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (updatableApps.isEmpty()) "My apps" else "Updates") },
                actions = {
                    if (updatableApps.isNotEmpty()) Button(
                        onClick = {},
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Text("Update all")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            modifier
                .padding(paddingValues)
                .then(
                    if (currentItem == null) Modifier
                    else Modifier.selectableGroup()
                ),
        ) {
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
            if (updatableApps.isNotEmpty()) item(contentType = { "D" }) {
                TopAppBar(title = { Text("My apps") }, windowInsets = WindowInsets())
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
