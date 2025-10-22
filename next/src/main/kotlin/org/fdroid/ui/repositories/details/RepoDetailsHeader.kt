package org.fdroid.ui.repositories.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.fdroid.R
import org.fdroid.database.Repository
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.repositories.RepoIcon
import org.fdroid.ui.utils.FDroidOutlineButton
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.getRepository

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun RepoDetailsHeader(
    repo: Repository,
    numberOfApps: Int?,
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
    val lastIndexUpdate = stringResource(R.string.repo_last_update_upstream, lastIndexTime)

    val lastUpdatedTime = repo.lastUpdated?.asRelativeTimeString()
        ?: stringResource(R.string.repositories_last_update_never)
    val lastUpdated = stringResource(R.string.last_updated, lastUpdatedTime)

    Column(
        verticalArrangement = spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            horizontalArrangement = spacedBy(8.dp),
        ) {
            RepoIcon(repo, Modifier.size(64.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = name,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                )
                Text(
                    text = repo.address
                        .replaceFirst("https://", "")
                        .replaceFirst("/repo", ""),
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
                    text = lastIndexUpdate,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = lastUpdated,
                    style = MaterialTheme.typography.bodySmall,
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
        RepoDetailsHeader(getRepository(), 45) { _, _ -> }
    }
}
