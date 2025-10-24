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
    val (color, stringRes) = when {
        // app is outright incompatible
        item.isIncompatible -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            R.string.app_no_compatible_versions,
        )
        // app is installed, but can't receive updates, because current repo has different signer
        item.noUpdatesBecauseDifferentSigner -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            R.string.app_no_compatible_signer,
        )
        // app targets old targetSdk, not a deal breaker, but worth flagging, no auto-update
        item.oldTargetSdk -> Pair(
            MaterialTheme.colorScheme.inverseSurface,
            R.string.app_no_auto_update,
        )
        else -> return
    }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        WarningRow(
            text = stringResource(stringRes),
        )
    }
}

@Composable
private fun WarningRow(text: String) {
    Row(
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Default.WarningAmber, null)
        Text(text, style = MaterialTheme.typography.bodyLarge)
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

@Preview
@Composable
private fun IncompatiblePreview() {
    FDroidContent {
        Column {
            AppDetailsWarnings(
                testApp.copy(
                    versions = listOf(
                        testApp.versions!!.first().copy(isCompatible = false),
                    ),
                )
            )
        }
    }
}
