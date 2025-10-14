package org.fdroid.ui.repositories.add

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AddRepoProgressScreen(text: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = spacedBy(16.dp, CenterVertically),
        horizontalAlignment = CenterHorizontally,
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
        )
        CircularWavyProgressIndicator(modifier = Modifier.size(64.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent(pureBlack = true) {
        AddRepoProgressScreen(stringResource(R.string.repo_state_fetching))
    }
}
