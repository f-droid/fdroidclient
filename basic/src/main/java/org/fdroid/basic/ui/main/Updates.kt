package org.fdroid.basic.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import org.fdroid.basic.ui.main.updates.UpdatableApp
import org.fdroid.basic.ui.main.updates.UpdatableAppRow
import org.fdroid.fdroid.ui.theme.FDroidContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Updates(apps: List<UpdatableApp>, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Updates") },
                actions = {
                    Button(onClick = {}, modifier = Modifier.padding(end = 16.dp)) {
                        Text("Update all")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier.padding(paddingValues)) {
            items(apps, key = { it.name }) { app ->
                UpdatableAppRow(app, Modifier.animateItem())
            }
        }
    }
}

@Preview
@PreviewScreenSizes
@Composable
fun UpdatesPreview() {
    FDroidContent {
        val app1 = UpdatableApp(
            name = "App Update 123",
            currentVersionName = "1.0.1",
            updateVersionName = "1.1.0",
            size = 123456789,
        )
        val app2 = UpdatableApp(
            name = "App Update 456",
            currentVersionName = "3.0.1",
            updateVersionName = "3.1.0",
            size = 9876543,
        )
        Updates(listOf(app1, app2))
    }
}
