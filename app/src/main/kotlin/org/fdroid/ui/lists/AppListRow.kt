package org.fdroid.ui.lists

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.InstalledBadge
import org.fdroid.ui.utils.asRelativeTimeString

@Composable
fun AppListRow(
  item: AppListItem,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  showLastUpdated: Boolean = false,
) {
  val now = remember { System.currentTimeMillis() }
  ListItem(
    modifier = modifier,
    leadingContent = {
      BadgedBox(badge = { if (item.isInstalled) InstalledBadge() }) {
        AsyncShimmerImage(
          model = item.iconModel,
          error = painterResource(R.drawable.ic_repo_app_default),
          contentDescription = null,
          modifier =
            Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).semantics {
              hideFromAccessibility()
            },
        )
      }
    },
    supportingContent = {
      if (showLastUpdated) {
        Column(verticalArrangement = spacedBy(4.dp)) {
          Text(item.summary)
          Row(horizontalArrangement = spacedBy(4.dp), verticalAlignment = CenterVertically) {
            val isOld = now - item.lastUpdated > TimeUnit.DAYS.toMillis(365)
            if (isOld) {
              Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            }
            Text(
              text =
                stringResource(
                  R.string.last_updated_short,
                  item.lastUpdated.asRelativeTimeString(),
                ),
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
      } else {
        Text(item.summary)
      }
    },
    colors =
      ListItemDefaults.colors(
        containerColor =
          if (isSelected) {
            MaterialTheme.colorScheme.surfaceVariant
          } else {
            Color.Transparent
          }
      ),
  ) {
    Text(item.name)
  }
}

@PreviewLightDark
@Composable
fun AppListRowPreview() {
  FDroidContent {
    val item1 = AppListItem(1, "1", "This is app 1", "It has summary 2", 0, false, true)
    val item2 = AppListItem(2, "2", "This is app 2", "It has summary 2", 0, true, true)
    val item3 =
      AppListItem(
        repoId = 2,
        packageName = "3",
        name = "This is app 3",
        summary = "It has summary 3",
        lastUpdated = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5),
        isInstalled = true,
        isCompatible = true,
      )
    val item4 =
      AppListItem(
        repoId = 2,
        packageName = "4",
        name = "This is app 4",
        summary = "It has summary 4",
        lastUpdated = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365 * 2),
        isInstalled = true,
        isCompatible = true,
      )
    Column {
      AppListRow(item1, false)
      AppListRow(item2, true)
      AppListRow(item3, false, showLastUpdated = true)
      AppListRow(item4, false, showLastUpdated = true)
    }
  }
}
