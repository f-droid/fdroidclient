package org.fdroid.ui.utils

import android.text.format.Formatter
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.ui.apps.AppUpdateItem

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
