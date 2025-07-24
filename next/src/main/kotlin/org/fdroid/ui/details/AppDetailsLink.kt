package org.fdroid.ui.details

import android.content.ClipData
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fdroid.next.R
import org.fdroid.ui.utils.openUriSafe

@Composable
fun AppDetailsLink(icon: ImageVector, title: String, url: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    Row(
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 48.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = { uriHandler.openUriSafe(url) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val entry = ClipEntry(ClipData.newPlainText("", url))
                    coroutineScope.launch {
                        clipboardManager.setClipEntry(entry)
                    }
                },
                onLongClickLabel = stringResource(R.string.copy_link),
            ),
    ) {
        Icon(icon, null)
        Text(title)
    }
}
