package org.fdroid.ui.repositories.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.database.Repository
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.getRepoDetailsInfo

@Composable
fun RepoDetailsContent(
    info: RepoDetailsInfo,
    onShowAppsClicked: (String, Long) -> Unit,
    modifier: Modifier,
) {
    val repo = info.model.repo as Repository
    val context = LocalContext.current
    Column(
        verticalArrangement = spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        RepoDetailsHeader(
            repo = repo,
            numberOfApps = info.model.numberApps,
            proxy = info.model.proxy,
            onShowAppsClicked = onShowAppsClicked,
        )
        if (info.model.showOfficialMirrors) {
            OfficialMirrors(
                mirrors = info.model.officialMirrors,
                setMirrorEnabled = { m, e ->
                    info.actions.setMirrorEnabled(repo.repoId, m, e)
                },
            )
        }
        if (info.model.showUserMirrors) {
            UserMirrors(
                mirrors = info.model.userMirrors,
                setMirrorEnabled = { m, e ->
                    info.actions.setMirrorEnabled(repo.repoId, m, e)
                },
                onShareMirror = { mirror ->
                    mirror.share(context, repo.fingerprint)
                },
                onDeleteMirror = { info.actions.deleteUserMirror(repo.repoId, it) },
            )
        }
        FingerprintExpandable(repo.fingerprint)
        RepoSettings(
            repo = repo,
            archiveState = info.model.archiveState,
            onToggleArchiveClicked = info.actions::setArchiveRepoEnabled,
            onCredentialsUpdated = info.actions::updateUsernameAndPassword,
        )
    }
}

@Composable
private fun FingerprintExpandable(
    fingerprint: String,
) {
    ExpandableSection(
        icon = rememberVectorPainter(Icons.Default.Fingerprint),
        title = stringResource(R.string.repo_fingerprint),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            text = fingerprint,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
@Preview
private fun Preview() {
    FDroidContent {
        RepoDetails(getRepoDetailsInfo(), { _, _ -> }, {})
    }
}
