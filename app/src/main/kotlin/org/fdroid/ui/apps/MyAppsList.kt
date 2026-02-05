package org.fdroid.ui.apps

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.database.NotAvailable
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.MeteredConnectionDialog
import org.fdroid.ui.utils.OfflineBar
import org.fdroid.ui.utils.getMyAppsInfo
import org.fdroid.ui.utils.myAppsModel

@Composable
fun MyAppsList(
    myAppsInfo: MyAppsInfo,
    currentPackageName: String?,
    lazyListState: LazyListState,
    onAppItemClick: (String) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val updatableApps = myAppsInfo.model.appUpdates
    val installingApps = myAppsInfo.model.installingApps
    val appsWithIssue = myAppsInfo.model.appsWithIssue
    val installedApps = myAppsInfo.model.installedApps
    // allow us to hide "update all" button to avoid user pressing it twice
    var showUpdateAllButton by remember(updatableApps) {
        mutableStateOf(true)
    }
    var showMeteredDialog by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showIssueIgnoreDialog by remember { mutableStateOf<AppWithIssueItem?>(null) }
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
        modifier = modifier
            .then(
                if (currentPackageName == null) Modifier
                else Modifier.selectableGroup()
            ),
    ) {
        // Updates header with Update all button
        if (!updatableApps.isNullOrEmpty()) {
            if (!myAppsInfo.model.networkState.isOnline) {
                item(key = "OfflineBar", contentType = "offlineBar") {
                    OfflineBar()
                }
            }
            item(key = "A", contentType = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.updates),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(16.dp)
                            .weight(1f),
                    )
                    if (showUpdateAllButton) Button(
                        onClick = {
                            val installLambda = {
                                myAppsInfo.updateAll()
                                showUpdateAllButton = false
                            }
                            if (myAppsInfo.model.networkState.isMetered) {
                                showMeteredDialog = installLambda
                            } else {
                                installLambda()
                            }
                        },
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
        // Apps with issues
        if (!appsWithIssue.isNullOrEmpty()) {
            // header
            item(key = "C", contentType = "header") {
                Text(
                    text = stringResource(R.string.my_apps_header_apps_with_issue),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            // list of apps with issues
            items(
                items = appsWithIssue,
                key = { it.packageName },
                contentType = { "C" },
            ) { app ->
                val isSelected = app.packageName == currentPackageName
                var showNotAvailableDialog by remember { mutableStateOf(false) }
                val onClick = {
                    if (app.issue is NotAvailable) {
                        showNotAvailableDialog = true
                    } else {
                        onAppItemClick(app.packageName)
                    }
                }
                val interactionModifier = if (currentPackageName == null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = { showIssueIgnoreDialog = app },
                        onLongClickLabel = stringResource(R.string.my_apps_ignore_dialog_title),
                    )
                } else {
                    Modifier.selectable(
                        selected = isSelected,
                        onClick = onClick,
                    )
                }
                val modifier = Modifier
                    .animateItem()
                    .then(interactionModifier)
                InstalledAppRow(app, isSelected, modifier, app.issue)
                // Dialogs
                val appToIgnore = showIssueIgnoreDialog
                if (appToIgnore != null) IgnoreIssueDialog(
                    appName = appToIgnore.name,
                    onIgnore = {
                        myAppsInfo.ignoreAppIssue(appToIgnore)
                        showIssueIgnoreDialog = null
                    },
                    onDismiss = { showIssueIgnoreDialog = null },
                ) else if (showNotAvailableDialog) NotAvailableDialog(app.packageName) {
                    showNotAvailableDialog = false
                }
            }
        }
        // Installed apps header (only show when we have non-empty lists above)
        val aboveNonEmpty = installingApps.isNotEmpty() ||
            !updatableApps.isNullOrEmpty() ||
            !appsWithIssue.isNullOrEmpty()
        if (aboveNonEmpty && !installedApps.isNullOrEmpty()) {
            item(key = "D", contentType = "header") {
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
            contentType = { "D" },
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
    val meteredLambda = showMeteredDialog
    if (meteredLambda != null) MeteredConnectionDialog(
        numBytes = myAppsInfo.model.appUpdatesBytes,
        onConfirm = { meteredLambda() },
        onDismiss = { showMeteredDialog = null },
    )
}

@Preview
@Composable
@RestrictTo(RestrictTo.Scope.TESTS)
private fun MyAppsListPreview() {
    FDroidContent {
        MyApps(
            myAppsInfo = getMyAppsInfo(myAppsModel),
            currentPackageName = null,
            onAppItemClick = {},
        )
    }
}
