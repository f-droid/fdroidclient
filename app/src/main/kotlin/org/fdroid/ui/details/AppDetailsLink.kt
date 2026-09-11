package org.fdroid.ui.details

import android.content.ClipData
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fdroid.R
import org.fdroid.ui.utils.openUriSafe

@Composable
fun AppDetailsLink(
  icon: ImageVector,
  title: String,
  url: String,
  modifier: Modifier = Modifier,
  disableTinting: Boolean = false,
  subTitle: String? = null,
) {
  val uriHandler = LocalUriHandler.current
  val haptics = LocalHapticFeedback.current
  val clipboardManager = LocalClipboard.current
  val coroutineScope = rememberCoroutineScope()
  Row(
    horizontalArrangement = spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .fillMaxWidth()
        .combinedClickable(
          onClick = { uriHandler.openUriSafe(url) },
          onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            val entry = ClipEntry(ClipData.newPlainText("", url))
            coroutineScope.launch { clipboardManager.setClipEntry(entry) }
          },
          onLongClickLabel = stringResource(R.string.copy_link),
        ),
  ) {
    Icon(
      icon,
      null,
      // don't tint colorful icons (e.g. for donation options)
      tint = if (disableTinting) Color.Unspecified else LocalContentColor.current,
      modifier = Modifier.size(24.dp),
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = spacedBy(8.dp),
    ) {
      Text(title, modifier = Modifier.alignBy(FirstBaseline))
      subTitle?.let { it ->
        Text(
          text = it,
          style = MaterialTheme.typography.labelSmall,
          maxLines = 1,
          overflow = TextOverflow.MiddleEllipsis,
          modifier = Modifier.alignBy(FirstBaseline),
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun AppDetailsLinkPreview() {
  AppDetailsLink(
    Icons.Default.Share,
    "example.com",
    "",
  )
}

@Preview(showBackground = true)
@Composable
fun AppDetailsLinkWithSubtitlePreview() {
  AppDetailsLink(
    Icons.Default.Code,
    "Source Code",
    "",
    subTitle = "https://gitlab.com/fdroid/fdroidclient",
  )
}

@Preview(showBackground = true)
@Composable
fun AppDetailsLinkWithVeryLongSubtitlePreview() {
  AppDetailsLink(
    icon = Icons.Default.Checklist,
    title = "SHA-512",
    url = "",
    subTitle =
      "a69a964b3d34c3cd792b2c68c4730e0c4a7e54bbf90276052913971f00df6b997f2c7ffa6aea208a6bd86a4f171d06b0531585036e2bfe87dd11702abf87acab",
  )
}
