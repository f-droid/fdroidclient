package org.fdroid.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.Names

@Composable
fun InstalledAppRow(
    app: MyInstalledAppItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    hasIssue: Boolean = false,
) {
    Column(modifier = modifier) {
        ListItem(
            leadingContent = {
                BadgedBox(badge = {
                    if (hasIssue) Icon(
                        imageVector = Icons.Filled.Error,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription =
                            stringResource(R.string.my_apps_header_apps_with_issue),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(1.dp)
                            .size(24.dp)
                    )
                }) {
                    AsyncShimmerImage(
                        model = app.iconModel,
                        error = painterResource(R.drawable.ic_repo_app_default),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { hideFromAccessibility() },
                    )
                }
            },
            headlineContent = {
                Text(app.name)
            },
            supportingContent = {
                Text(app.installedVersionName)
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
}

@Preview
@Composable
fun InstalledAppRowPreview() {
    val app = InstalledAppItem(
        packageName = "",
        name = Names.randomName,
        installedVersionName = "1.0.1",
        lastUpdated = System.currentTimeMillis() - 5000,
    )
    FDroidContent {
        Column {
            InstalledAppRow(app, false)
            InstalledAppRow(app, true)
            InstalledAppRow(app, false, hasIssue = true)
        }
    }
}
