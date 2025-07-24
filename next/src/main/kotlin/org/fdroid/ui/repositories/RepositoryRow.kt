package org.fdroid.ui.repositories

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.fdroid.ui.utils.AsyncShimmerImage

@Composable
fun RepositoryRow(
    repoItem: RepositoryItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        leadingContent = {
            AsyncShimmerImage(
                model = repoItem.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        headlineContent = {
            Text(repoItem.name)
        },
        supportingContent = {
            Text(repoItem.address)
        },
        trailingContent = {
            Switch(repoItem.enabled, onCheckedChange = null)
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
