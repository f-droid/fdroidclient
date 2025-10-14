package org.fdroid.ui.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.download.PackageName
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.Names

@Composable
fun InstalledAppRow(
    app: InstalledAppItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ListItem(
            leadingContent = {
                AsyncShimmerImage(
                    model = PackageName(app.packageName, app.iconDownloadRequest),
                    error = painterResource(R.drawable.ic_repo_app_default),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
            },
            headlineContent = {
                Text(app.name)
            },
            supportingContent = {
                Text(app.installedVersionName)
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
fun InstalledAppRowPreview() {
    val app = InstalledAppItem(
        packageName = "",
        name = Names.randomName,
        installedVersionName = "1.0.1",
        lastUpdated = System.currentTimeMillis() - 5000,
    )
    FDroidContent {
        Column {
            InstalledAppRow(app, false)
            InstalledAppRow(app, true)
        }
    }
}
