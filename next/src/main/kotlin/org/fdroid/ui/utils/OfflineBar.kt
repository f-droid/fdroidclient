package org.fdroid.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent

@Composable
fun OfflineBar(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(MaterialTheme.colorScheme.errorContainer)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.banner_no_internet),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        OfflineBar()
    }
}
