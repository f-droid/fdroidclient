package org.fdroid.ui.lists

import android.content.res.Configuration.UI_MODE_NIGHT_YES
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
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.ui.utils.AsyncShimmerImage

@Composable
fun AppListRow(
    item: AppListItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = { Text(item.summary) },
        leadingContent = {
            AsyncShimmerImage(
                model = item.iconDownloadRequest,
                error = painterResource(R.drawable.ic_repo_app_default),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
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
fun AppListRowPreview() {
    FDroidContent {
        val item1 = AppListItem("1", "This is app 1", "It has summary 2", 0, null)
        val item2 = AppListItem("2", "This is app 2", "It has summary 2", 0, null)
        Column {
            AppListRow(item1, false)
            AppListRow(item2, true)
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun AppListRowPreviewNight() {
    FDroidContent {
        val item1 = AppListItem("1", "This is app 1", "It has summary 2", 0, null)
        val item2 = AppListItem("2", "This is app 2", "It has summary 2", 0, null)
        Column {
            AppListRow(item1, false)
            AppListRow(item2, true)
        }
    }
}
