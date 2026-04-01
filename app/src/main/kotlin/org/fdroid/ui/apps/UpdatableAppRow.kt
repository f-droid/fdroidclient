package org.fdroid.ui.apps

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
      supportingContent = { VersionLine(app) },
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

/**
 * Shows which is the installed version of the app and to which version it will upgrade. Takes into
 * account the layout direction to show the correct order of versions and the arrow.
 */
@Composable
fun VersionLine(app: AppUpdateItem) {
  VersionLineWithSize(app.installedVersionName, app.update.versionName, app.update.size)
}

@Composable
fun VersionLineWithSize(fromVersion: String?, toVersion: String, numBytes: Long? = null) {
  val size = numBytes?.let { Formatter.formatFileSize(LocalContext.current, it) }
  VersionLine(fromVersion, toVersion, size)
}

@Composable
fun VersionLine(fromVersion: String?, toVersion: String, extraText: String? = null) {
  val test = buildAnnotatedString {
    if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
      if (fromVersion != null) {
        append(fromVersion)
      }
    } else {
      append("\u202A${toVersion}\u202C")
    }
    if (fromVersion != null) appendInlineContent("arrowId", " → ")
    if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
      append("\u202A${toVersion}\u202C")
    } else if (fromVersion != null) {
      append("\u202A${fromVersion}\u202C")
    }
    if (extraText != null) append(" • $extraText")
  }
  val inlineContent =
    mapOf(
      Pair(
        "arrowId",
        InlineTextContent(
          Placeholder(
            width = 24.sp,
            height = 20.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
          )
        ) {
          Icon(
            Icons.AutoMirrored.Default.ArrowRightAlt,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 2.dp),
          )
        },
      )
    )
  Text(test, inlineContent = if (fromVersion == null) mapOf() else inlineContent)
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
      installedVersionName = "1.0.1-alpha",
      update = getPreviewVersion("1.1.0-beta", 123456789),
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
      installedVersionName = "1.0.1-alpha",
      update = getPreviewVersion("1.1.0-beta", 123456789),
      whatsNew = "This is new, all is new, nothing old.",
    )
  FDroidContent { Column { UpdatableAppRow(app1, false) } }
}
