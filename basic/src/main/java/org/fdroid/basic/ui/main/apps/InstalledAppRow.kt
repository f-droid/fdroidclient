package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.basic.ui.main.discover.Names
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun InstalledAppRow(
    app: InstalledApp,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ListItem(
            leadingContent = {
                Icon(
                    Icons.Filled.Android,
                    tint = MaterialTheme.colorScheme.secondary,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .size(48.dp)
                        .background(Color.White)
                        .padding(8.dp),
                )
            },
            headlineContent = {
                Text(app.name)
            },
            supportingContent = {
                Text(app.versionName)
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
    val app = InstalledApp(
        packageName = "",
        name = Names.randomName,
        versionName = "1.0.1",
    )
    FDroidContent {
        Column {
            InstalledAppRow(app, false)
            InstalledAppRow(app, true)
        }
    }
}
