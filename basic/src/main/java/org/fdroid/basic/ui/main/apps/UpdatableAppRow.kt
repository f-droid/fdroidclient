package org.fdroid.basic.ui.main.apps

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun UpdatableAppRow(
    app: UpdatableApp,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column {
        ListItem(
            leadingContent = {
                BadgedBox(badge = {
                    Icon(
                        imageVector = Icons.Filled.NewReleases,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null, modifier = Modifier.size(24.dp),
                    )
                }) {
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
                }
            },
            headlineContent = {
                Text(app.name)
            },
            supportingContent = {
                val size = app.size.let {
                    Formatter.formatFileSize(LocalContext.current, it)
                }
                Text("${app.currentVersionName} → ${app.updateVersionName} • $size")
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
            Text(
                text = app.whatsNew ?: "",
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}

@Preview
@Composable
fun UpdatableAppRowPreview() {
    val app = UpdatableApp(
        packageName = "",
        name = "App Update 123",
        currentVersionName = "1.0.1",
        updateVersionName = "1.1.0",
        size = 123456789,
        whatsNew = "This is new, all is new, nothing old.",
    )
    FDroidContent {
        Column {
            UpdatableAppRow(app, false)
            UpdatableAppRow(app, true)
        }
    }
}
