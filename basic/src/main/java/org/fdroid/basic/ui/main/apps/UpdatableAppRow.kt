package org.fdroid.basic.ui.main.apps

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.fdroid.basic.R
import org.fdroid.basic.manager.AppUpdateItem
import org.fdroid.basic.ui.getPreviewVersion
import org.fdroid.basic.ui.icons.PackageName
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun UpdatableAppRow(
    app: AppUpdateItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        ListItem(
            leadingContent = {
                BadgedBox(badge = {
                    Icon(
                        imageVector = Icons.Filled.NewReleases,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null, modifier = Modifier.size(24.dp),
                    )
                }) {
                    AsyncImage(
                        model = PackageName(app.packageName, app.iconDownloadRequest),
                        error = painterResource(R.drawable.ic_repo_app_default),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                }
            },
            headlineContent = {
                Text(app.name)
            },
            supportingContent = {
                val size = app.update.size?.let {
                    Formatter.formatFileSize(LocalContext.current, it)
                }
                Text("${app.installedVersionName} → ${app.update.versionName} • $size")
            },
            trailingContent = {
                if (app.whatsNew != null) IconButton(onClick = { isExpanded = !isExpanded }) {
                    if (isExpanded) {
                        Icon(Icons.Default.ArrowDropUp, "TODO")
                    } else {
                        Icon(Icons.Default.ArrowDropDown, "TODO")
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
        AnimatedVisibility(visible = isExpanded, modifier = Modifier.padding(8.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = app.whatsNew ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun UpdatableAppRowPreview() {
    val app1 = AppUpdateItem(
        packageName = "A",
        name = "App Update 123",
        installedVersionName = "1.0.1",
        update = getPreviewVersion("1.1.0", 123456789),
        whatsNew = "This is new, all is new, nothing old.",
    )
    val app2 = AppUpdateItem(
        packageName = "B",
        name = "App Update 456",
        installedVersionName = "1.0.1",
        update = getPreviewVersion("1.1.0", 123456789),
        whatsNew = "This is new, all is new, nothing old.",
    )
    FDroidContent {
        Column {
            UpdatableAppRow(app1, false)
            UpdatableAppRow(app2, true)
        }
    }
}
