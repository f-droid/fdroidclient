package org.fdroid.basic.ui.main.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.BadgedBox
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
import coil3.compose.AsyncImage
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun AppItem(
    name: String,
    summary: String,
    icon: String?,
    isNew: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(summary) },
        leadingContent = {
            BadgedBox(badge = {
                if (isNew) Icon(
                    imageVector = Icons.Filled.NewReleases,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null, modifier = Modifier.size(24.dp),
                )
            }) {
                icon?.let {
                    AsyncImage(
                        model = icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                } ?: Icon(
                    Icons.Filled.Android,
                    tint = MaterialTheme.colorScheme.secondary,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .size(48.dp)
                        .background(Color.White)
                        .padding(8.dp),
                )
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

@Preview
@Composable
fun AppItemPreview() {
    FDroidContent {
        AppItem("This is app 1", "It has summary 2", null, false, false)
    }
}

@Preview
@Composable
fun AppItemPreviewNew() {
    FDroidContent {
        AppItem("This is app 1", "It has summary 2", null, true, false)
    }
}

@Preview
@Composable
fun AppItemPreviewSelected() {
    FDroidContent {
        AppItem("This is app 1", "It has summary 2", null, false, true)
    }
}
