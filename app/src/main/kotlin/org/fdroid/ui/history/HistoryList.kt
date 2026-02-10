package org.fdroid.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.R
import org.fdroid.history.InstallEvent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BadgeIcon
import org.fdroid.ui.utils.asRelativeTimeString

@Composable
fun HistoryList(
    items: List<HistoryItem>,
    enabled: Boolean?,
    onEnabled: (Boolean) -> Unit,
    paddingValues: PaddingValues
) {
    LazyColumn(
        contentPadding = paddingValues,
    ) {
        if (enabled != null) item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clickable { onEnabled(!enabled) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Use install history",
                        fontSize = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(enabled, onCheckedChange = onEnabled)
                }
            }
        }
        if (items.isEmpty()) item {
            val s = if (enabled == true) stringResource(R.string.install_history_empty_state)
            else stringResource(R.string.install_history_disabled_state)
            Text(
                text = s,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        } else items(
            items = items,
            key = { "${it.event.time}-${it.event.packageName}" },
            contentType = { "item" },
        ) { item ->
            ListItem(
                leadingContent = {
                    BadgedBox(badge = {
                        BadgeIcon(
                            icon = if (item.event is InstallEvent) {
                                Icons.Filled.DownloadForOffline
                            } else {
                                Icons.Filled.RemoveCircleOutline
                            },
                            color = if (item.event is InstallEvent) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            contentDescription = if (item.event is InstallEvent) {
                                stringResource(R.string.menu_install)
                            } else {
                                stringResource(R.string.menu_uninstall)
                            },
                        )
                    }) {
                        AsyncShimmerImage(
                            model = item.iconModel,
                            error = painterResource(R.drawable.ic_repo_app_default),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { hideFromAccessibility() },
                        )
                    }
                },
                headlineContent = {
                    Text(item.event.name ?: item.event.packageName)
                },
                supportingContent = {
                    val dateStr = item.event.time.asRelativeTimeString()
                    val text = if (item.event is InstallEvent) {
                        if (item.event.oldVersionName == null) {
                            "${item.event.versionName} • $dateStr"
                        } else if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
                            "${item.event.oldVersionName} → ${item.event.versionName} • $dateStr"
                        } else {
                            "$dateStr • ${item.event.versionName} ← ${item.event.oldVersionName}"
                        }
                    } else dateStr
                    Text(text)
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
                modifier = if (enabled == false) Modifier.alpha(0.5f) else Modifier
            )
        }
    }
}
