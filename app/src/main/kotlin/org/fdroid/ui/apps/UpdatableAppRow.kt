package org.fdroid.ui.apps

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BadgeIcon
import org.fdroid.ui.utils.ExpandIconArrow
import org.fdroid.ui.utils.getPreviewVersion

@Composable
fun UpdatableAppRow(app: AppUpdateItem, isSelected: Boolean, modifier: Modifier = Modifier) {
  var isExpanded by remember { mutableStateOf(false) }
  Column(modifier = modifier) {
    ListItem(
      leadingContent = {
        BadgedBox(
          badge = {
            BadgeIcon(
              icon = Icons.Filled.NewReleases,
              color = MaterialTheme.colorScheme.secondary,
              contentDescription =
                stringResource(R.string.notification_title_single_update_available),
            )
          }
        ) {
          AsyncShimmerImage(
            model = app.iconModel,
            error = painterResource(R.drawable.ic_repo_app_default),
            contentDescription = null,
            modifier = Modifier.size(48.dp).semantics { hideFromAccessibility() },
          )
        }
      },
      headlineContent = { Text(app.name) },
      supportingContent = {
        val size = app.update.size?.let { Formatter.formatFileSize(LocalContext.current, it) }
        val text =
          if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
            "${app.installedVersionName} → ${app.update.versionName} • $size"
          } else {
            "$size • ${app.update.versionName} ← ${app.installedVersionName}"
          }
        Text(text)
      },
      trailingContent = {
        if (app.whatsNew != null)
          IconButton(onClick = { isExpanded = !isExpanded }) { ExpandIconArrow(isExpanded) }
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
    )
    AnimatedVisibility(
      visible = isExpanded,
      modifier = Modifier.padding(8.dp).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
      Card(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = app.whatsNew ?: "",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(8.dp),
        )
      }
    }
  }
}

@Preview
@Composable
fun UpdatableAppRowPreview() {
  val app1 =
    AppUpdateItem(
      repoId = 1,
      packageName = "A",
      name = "App Update 123",
      installedVersionName = "1.0.1",
      update = getPreviewVersion("1.1.0", 123456789),
      whatsNew = "This is new, all is new, nothing old.",
    )
  val app2 =
    AppUpdateItem(
      repoId = 2,
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

@Preview(locale = "fa")
@Composable
private fun UpdatableAppRowRtl() {
  val app1 =
    AppUpdateItem(
      repoId = 1,
      packageName = "A",
      name = "App Update 123",
      installedVersionName = "1.0.1",
      update = getPreviewVersion("1.1.0", 123456789),
      whatsNew = "This is new, all is new, nothing old.",
    )
  FDroidContent { Column { UpdatableAppRow(app1, false) } }
}
