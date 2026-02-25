package org.fdroid.ui.repositories.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.database.Repository
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.FDroidOutlineButton
import org.fdroid.ui.utils.FDroidSwitchRow
import org.fdroid.ui.utils.getRepository

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun RepoSettings(
  repo: Repository,
  archiveState: ArchiveState,
  onToggleArchiveClicked: (Boolean) -> Unit,
  onCredentialsUpdated: (String, String) -> Unit,
) {
  ExpandableSection(
    icon = rememberVectorPainter(Icons.Default.Settings),
    title = stringResource(R.string.menu_settings),
    modifier = Modifier.padding(horizontal = 16.dp),
  ) {
    Column(verticalArrangement = spacedBy(16.dp)) {
      when (archiveState) {
        ArchiveState.UNKNOWN -> {
          Column(verticalArrangement = spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.repo_archive_unknown))
            FDroidOutlineButton(
              text = stringResource(R.string.repo_archive_check),
              onClick = { onToggleArchiveClicked(true) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
        ArchiveState.LOADING -> {
          LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
        }
        else -> {
          FDroidSwitchRow(
            text = stringResource(R.string.repo_archive_toggle_description),
            checked = archiveState == ArchiveState.ENABLED,
            enabled = true,
            onCheckedChange = onToggleArchiveClicked,
          )
        }
      }
      val username = repo.username
      if (!username.isNullOrBlank()) {
        BasicAuth(username) { username, password -> onCredentialsUpdated(username, password) }
      }
    }
  }
}

@Preview
@Composable
private fun PreviewUnknown() {
  FDroidContent { RepoSettings(getRepository(), ArchiveState.UNKNOWN, {}) { _, _ -> } }
}

@Preview
@Composable
private fun PreviewLoading() {
  FDroidContent { RepoSettings(getRepository(), ArchiveState.LOADING, {}) { _, _ -> } }
}

@Preview
@Composable
private fun PreviewEnabled() {
  FDroidContent { RepoSettings(getRepository(), ArchiveState.ENABLED, {}) { _, _ -> } }
}

@Preview
@Composable
private fun PreviewDisabled() {
  FDroidContent { RepoSettings(getRepository(), ArchiveState.DISABLED, {}) { _, _ -> } }
}
