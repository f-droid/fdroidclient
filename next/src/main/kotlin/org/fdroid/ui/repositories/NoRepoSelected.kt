package org.fdroid.ui.repositories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.icons.PackageVariant

@Composable
fun NoRepoSelected() {
    Box(
        contentAlignment = Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(32.dp)
        ) {
            Icon(
                imageVector = PackageVariant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = stringResource(R.string.repo_list_info_text),
                modifier = Modifier.fillMaxWidth(fraction = 0.7f)
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 400)
@Composable
private fun Preview() {
    FDroidContent {
        NoRepoSelected()
    }
}
