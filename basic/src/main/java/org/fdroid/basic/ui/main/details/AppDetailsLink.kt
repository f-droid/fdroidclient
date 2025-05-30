package org.fdroid.basic.ui.main.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun AppDetailsLink(icon: ImageVector, title: String, url: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Row(
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = { uriHandler.openUri(url) }),
    ) {
        Icon(icon, null)
        Text(title)
    }
}
