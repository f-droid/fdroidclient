package org.fdroid.basic.ui.main.details

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UpdateDisabled
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.fdroid.basic.R
import org.fdroid.basic.details.AppDetailsItem
import org.fdroid.basic.details.testApp
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun AppDetailsMenu(
    item: AppDetailsItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        if (item.appPrefs != null) DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.Preview, null)
            },
            text = { Text(stringResource(R.string.menu_release_channel_beta)) },
            trailingIcon = {
                Checkbox(
                    checked = item.allowsBetaVersions,
                    onCheckedChange = null,
                    enabled = !item.ignoresAllUpdates,
                )
            },
            enabled = !item.ignoresAllUpdates,
            onClick = {
                item.actions.allowBetaVersions()
                onDismiss()
            },
        )
        if (item.actions.ignoreAllUpdates != null) DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.UpdateDisabled, null)
            },
            text = { Text(stringResource(R.string.menu_ignore_all)) },
            trailingIcon = {
                Checkbox(item.ignoresAllUpdates, null)
            },
            onClick = {
                item.actions.ignoreAllUpdates()
                onDismiss()
            },
        )
        if (item.actions.ignoreThisUpdate != null) DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.UpdateDisabled, null)
            },
            text = { Text(stringResource(R.string.menu_ignore_this)) },
            trailingIcon = {
                Checkbox(
                    checked = item.ignoresCurrentUpdate,
                    onCheckedChange = null,
                    enabled = !item.ignoresAllUpdates,
                )
            },
            enabled = !item.ignoresAllUpdates,
            onClick = {
                item.actions.ignoreThisUpdate()
                onDismiss()
            },
        )
        if (item.actions.shareApk != null) DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.Share, null)
            },
            text = { Text(stringResource(R.string.menu_share_apk)) },
            onClick = {
                item.actions.shareApk()
                onDismiss()
            },
        )
        if (item.actions.uninstallApp != null) DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.Delete, null)
            },
            text = { Text(stringResource(R.string.menu_uninstall)) },
            onClick = {
                item.actions.uninstallApp()
                onDismiss()
            },
        )
    }
}

@Preview
@Composable
fun AppDetailsMenuPreview() {
    AppDetailsMenu(testApp, true) {}
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun AppDetailsMenuAllIgnoredPreview() {
    val appPrefs = testApp.appPrefs!!.toggleIgnoreAllUpdates()
    FDroidContent {
        AppDetailsMenu(testApp.copy(appPrefs = appPrefs), true) {}
    }
}
