package org.fdroid.ui.repositories.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.download.Mirror
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.FDroidOutlineButton

@Composable
fun UserMirrors(
    mirrors: List<UserMirrorItem>,
    setMirrorEnabled: (Mirror, Boolean) -> Unit,
    onShareMirror: (UserMirrorItem) -> Unit,
    onDeleteMirror: (Mirror) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf<Mirror?>(null) }
    if (showDeleteDialog != null) AlertDialog(
        title = {
            Text(text = stringResource(R.string.repo_confirm_delete_mirror_title))
        },
        text = {
            Text(text = stringResource(R.string.repo_confirm_delete_mirror_body))
        },
        onDismissRequest = { showDeleteDialog = null },
        confirmButton = {
            TextButton(onClick = {
                onDeleteMirror(showDeleteDialog!!)
                showDeleteDialog = null
            }) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteDialog = null }) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
    ExpandableSection(
        icon = rememberVectorPainter(Icons.Default.Dns),
        title = stringResource(R.string.repo_user_mirrors),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Column {
            mirrors.forEach { m ->
                UserMirrorRow(
                    item = m,
                    setMirrorEnabled = setMirrorEnabled,
                    onShareMirror = onShareMirror,
                    onDeleteMirror = { showDeleteDialog = m.mirror },
                )
            }
        }
    }
}

@Composable
private fun UserMirrorRow(
    item: UserMirrorItem,
    setMirrorEnabled: (Mirror, Boolean) -> Unit,
    onShareMirror: (UserMirrorItem) -> Unit,
    onDeleteMirror: (Mirror) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(item.url)
        },
        supportingContent = {
            Row(
                horizontalArrangement = spacedBy(16.dp),
            ) {
                FDroidOutlineButton(
                    text = stringResource(R.string.menu_share),
                    imageVector = Icons.Default.Share,
                    onClick = { onShareMirror(item) },
                )
                FDroidOutlineButton(
                    text = stringResource(R.string.delete),
                    imageVector = Icons.Default.Delete,
                    onClick = { onDeleteMirror(item.mirror) },
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        trailingContent = {
            Switch(
                checked = item.isEnabled,
                onCheckedChange = null,
            )
        },
        colors = ListItemDefaults.colors(MaterialTheme.colorScheme.background),
        modifier = modifier.toggleable(
            value = item.isEnabled,
            role = Role.Switch,
            onValueChange = { checked -> setMirrorEnabled(item.mirror, checked) },
        ),
    )
}

@Preview
@Composable
fun UserMirrorsPreview() {
    FDroidContent {
        val mirrors = listOf(
            UserMirrorItem(Mirror("https://mirror.example.com/fdroid/repo"), true),
            UserMirrorItem(
                Mirror(
                    "https://mirror.example.org/with/a/very/long/url/that/wraps/repo",
                    "fr"
                ), true
            ),
            UserMirrorItem(Mirror("https://mirror.example.com/foo/bar/fdroid/repo"), false),
        )
        UserMirrors(
            mirrors = mirrors,
            setMirrorEnabled = { _, _ -> },
            onShareMirror = { },
            onDeleteMirror = { },
        )
    }
}
