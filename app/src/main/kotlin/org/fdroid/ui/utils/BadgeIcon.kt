package org.fdroid.ui.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SecurityUpdate
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent

@Composable
fun BadgeIcon(
    icon: ImageVector,
    contentDescription: String,
    color: Color = MaterialTheme.colorScheme.error
) = Icon(
    imageVector = icon,
    tint = color,
    contentDescription = contentDescription,
    modifier = Modifier
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .padding(1.dp)
        .size(24.dp)
)

@Composable
fun InstalledBadge() = BadgeIcon(
    icon = Icons.Filled.CheckCircle,
    contentDescription = stringResource(R.string.app_installed),
    color = MaterialTheme.colorScheme.secondary,
)

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        Column {
            BadgedBox(badge = {
                BadgeIcon(
                    icon = Icons.Filled.SecurityUpdate,
                    contentDescription = stringResource(R.string.app_installed),
                    color = MaterialTheme.colorScheme.error
                )
            }, modifier = Modifier.padding(16.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp),
                )
            }
            BadgedBox(
                badge = { InstalledBadge() },
                modifier = Modifier.padding(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp),
                )
            }
        }
    }
}
