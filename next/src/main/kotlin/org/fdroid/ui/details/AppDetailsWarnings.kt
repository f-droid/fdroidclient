package org.fdroid.ui.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.testApp

@Composable
fun AppDetailsWarnings(
    item: AppDetailsItem,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column {
            if (item.noCompatibleVersions) Row(
                horizontalArrangement = spacedBy(8.dp),
                verticalAlignment = CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.WarningAmber, "")
                Text(
                    text = stringResource(R.string.app_no_compatible_versions),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (item.noCompatibleVersions && item.oldTargetSdk) HorizontalDivider()
            if (item.oldTargetSdk) Row(
                horizontalArrangement = spacedBy(8.dp),
                verticalAlignment = CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.WarningAmber, "")
                Text(
                    stringResource(R.string.app_no_auto_update),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Preview
@Composable
fun AppDetailsWarningsPreview() {
    FDroidContent {
        Column {
            AppDetailsWarnings(testApp)
        }
    }
}
