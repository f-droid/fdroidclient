package org.fdroid.ui.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun InstalledBadge() = Icon(
    imageVector = Icons.Filled.CheckCircle,
    tint = MaterialTheme.colorScheme.secondary,
    contentDescription = stringResource(R.string.app_installed),
    modifier = Modifier
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .padding(1.dp)
        .size(24.dp)
)

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        BadgedBox(badge = { InstalledBadge() }, modifier = Modifier.padding(16.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp),
            )
        }
    }
}
