package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun AppDetails(
    appItem: AppNavigationItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalArrangement = spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.Android,
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Column {
            Text(appItem.name, style = MaterialTheme.typography.headlineMedium)
            Text(appItem.summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
fun AppDetailsPreview() {
    FDroidContent {
        val item = AppNavigationItem(
            packageName = "foo",
            name = "bar",
            summary = "This is a nice app!",
        )
        AppDetails(item)
    }
}
