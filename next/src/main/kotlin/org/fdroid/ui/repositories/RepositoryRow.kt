package org.fdroid.ui.repositories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BadgeIcon
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.repoItems

@Composable
fun RepositoryRow(
    repoItem: RepositoryItem,
    isSelected: Boolean,
    onRepoEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        leadingContent = {
            BadgedBox(
                badge = {
                    if (repoItem.hasIssue) BadgeIcon(
                        icon = Icons.Filled.Error,
                        color = MaterialTheme.colorScheme.error,
                        contentDescription = stringResource(R.string.repo_has_update_error),
                    )
                },
            ) {
                AsyncShimmerImage(
                    model = repoItem.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        headlineContent = {
            Text(repoItem.name)
        },
        supportingContent = {
            val lastUpdated = if (repoItem.timestamp <= 0) {
                stringResource(R.string.repositories_last_update_never)
            } else {
                repoItem.timestamp.asRelativeTimeString()
            }
            Text(stringResource(R.string.repo_last_update_upstream, lastUpdated))
        },
        trailingContent = {
            Switch(repoItem.enabled, onCheckedChange = onRepoEnabled)
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.background
            }
        ),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        Column {
            RepositoryRow(repoItems[0], false, {})
            RepositoryRow(repoItems[1], true, {})
            RepositoryRow(repoItems[2].copy(timestamp = 0), false, {})
        }
    }
}
