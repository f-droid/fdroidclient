package org.fdroid.ui.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.install.InstallState
import org.fdroid.ui.utils.AsyncShimmerImage

@Composable
fun InstallingAppRow(
    app: InstallingAppItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ListItem(
            leadingContent = {
                AsyncShimmerImage(
                    model = app.iconModel,
                    error = painterResource(R.drawable.ic_repo_app_default),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
            },
            headlineContent = {
                Text(app.name)
            },
            supportingContent = {
                val currentVersionName = app.installState.currentVersionName
                if (currentVersionName == null) {
                    Text(app.installState.versionName)
                } else {
                    Text("$currentVersionName → ${app.installState.versionName}")
                }
            },
            trailingContent = {
                if (app.installState is InstallState.Installed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.app_installed),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(8.dp)
                    )
                } else if (app.installState is InstallState.Error) {
                    val desc = stringResource(R.string.notification_title_summary_install_error)
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = desc,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    if (app.installState is InstallState.Downloading) {
                        CircularProgressIndicator(progress = { app.installState.progress })
                    } else {
                        CircularProgressIndicator()
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                }
            ),
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun Preview() {
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
    val installingApp2 = InstallingAppItem(
        packageName = "A2",
        installState = InstallState.Installed(
            name = "Installing App 2",
            versionName = "2.0.1",
            currentVersionName = null,
            lastUpdated = 13,
            iconDownloadRequest = null,
        )
    )
    val installingApp3 = InstallingAppItem(
        packageName = "A3",
        installState = InstallState.Error(
            msg = "error msg",
            name = "Installing App 2",
            versionName = "0.0.4",
            currentVersionName = null,
            lastUpdated = 13,
            iconDownloadRequest = null,
        )
    )
    FDroidContent {
        Column {
            InstallingAppRow(installingApp1, false)
            InstallingAppRow(installingApp2, true)
            InstallingAppRow(installingApp3, false)
        }
    }
}
