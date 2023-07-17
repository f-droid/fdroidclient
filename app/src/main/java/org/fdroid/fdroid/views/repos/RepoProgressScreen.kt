package org.fdroid.fdroid.views.repos

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.compose.ComposeUtils.FDroidContent

@Composable
fun RepoProgressScreen(paddingValues: PaddingValues, text: String) {
    Column(
        verticalArrangement = spacedBy(16.dp, CenterVertically),
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .padding(paddingValues)
            .fillMaxSize(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h5,
        )
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
    }
}

@Preview
@Composable
fun FetchingRepoScreenPreview() {
    FDroidContent {
        RepoProgressScreen(PaddingValues(0.dp), stringResource(R.string.repo_state_fetching))
    }
}
