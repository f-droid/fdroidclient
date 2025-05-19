package org.fdroid.basic.ui.main.repositories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat

@Composable
fun RepositoryItem(
    repoItem: Repository,
    isSelected: Boolean,
    localeList: LocaleListCompat,
    modifier: Modifier = Modifier,
) {
    ListItem(
        leadingContent = {
            Icon(
                Icons.Filled.Android,
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = null,
            )
        },
        headlineContent = {
            Text(repoItem.getName(localeList) ?: "Unknown repo")
        },
        supportingContent = {
            Text(repoItem.address)
        },
        trailingContent = {
            Switch(true, onCheckedChange = null)
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
