package org.fdroid.ui.repositories.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import io.ktor.client.engine.ProxyConfig
import org.fdroid.R
import org.fdroid.database.Repository
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.repositories.RepoIcon
import org.fdroid.ui.utils.FDroidOutlineButton
import org.fdroid.ui.utils.addressForUi
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.getRepository

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun RepoDetailsHeader(
    repo: Repository,
    numberOfApps: Int?,
    proxy: ProxyConfig?,
    onShowAppsClicked: (String, Long) -> Unit,
) {
    val localeList = LocaleListCompat.getDefault()
    val name = repo.getName(LocaleListCompat.getDefault()) ?: "Unknown Repo"
    val description = repo.getDescription(localeList)?.replace("\n", " ")

    val lastIndexTime = if (repo.timestamp < 0) {
        stringResource(R.string.repositories_last_update_never)
    } else {
        repo.timestamp.asRelativeTimeString()
    }
    val lastPublishedTime = stringResource(R.string.repo_last_update_upstream, lastIndexTime)

    val lastDownloadedTime = repo.lastUpdated?.asRelativeTimeString()
        ?: stringResource(R.string.repositories_last_update_never)
    val lastUpdated = stringResource(R.string.repo_last_downloaded, lastDownloadedTime)

    Column(
        verticalArrangement = spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            horizontalArrangement = spacedBy(8.dp),
        ) {
            RepoIcon(repo, proxy, Modifier.size(64.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = name,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                )
                Text(
                    text = repo.addressForUi,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (numberOfApps != null) Text(
                    text = pluralStringResource(
                        R.plurals.repo_num_apps_text,
                        numberOfApps,
                        numberOfApps,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lastPublishedTime,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = lastUpdated,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (repo.lastError != null) ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Row(
                horizontalArrangement = spacedBy(16.dp),
                verticalAlignment = CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.WarningAmber, null)
                Text(
                    text = stringResource(R.string.repo_has_update_error_intro) +
                        "\n\n${repo.lastError}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        if (repo.enabled) FDroidOutlineButton(
            stringResource(R.string.repo_num_apps_button),
            onClick = { onShowAppsClicked(name, repo.repoId) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (description?.isNotBlank() == true) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        RepoDetailsHeader(getRepository(), 45, null) { _, _ -> }
    }
}
