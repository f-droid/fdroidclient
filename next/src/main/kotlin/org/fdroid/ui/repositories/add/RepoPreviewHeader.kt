package org.fdroid.ui.repositories.add

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.repo.FetchResult.IsExistingMirror
import org.fdroid.repo.FetchResult.IsExistingRepository
import org.fdroid.repo.FetchResult.IsNewMirror
import org.fdroid.repo.FetchResult.IsNewRepoAndNewMirror
import org.fdroid.repo.FetchResult.IsNewRepository
import org.fdroid.repo.Fetching
import org.fdroid.ui.repositories.RepoIcon
import org.fdroid.ui.utils.FDroidButton
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.utils.getRepository

@Composable
fun RepoPreviewHeader(
    state: Fetching,
    onAddRepo: () -> Unit,
    onExistingRepo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    localeList: LocaleListCompat,
) {
    val repo = state.receivedRepo ?: error("repo was null")
    val isDevPreview = LocalInspectionMode.current

    val buttonText = when (state.fetchResult) {
        is IsNewRepository -> stringResource(R.string.repo_add_new_title)
        is IsNewRepoAndNewMirror -> stringResource(R.string.repo_add_repo_and_mirror)
        is IsNewMirror -> stringResource(R.string.repo_add_mirror)
        is IsExistingRepository, is IsExistingMirror -> stringResource(R.string.repo_view_repo)
        else -> error("Unexpected fetch state: ${state.fetchResult}")
    }
    val buttonAction: () -> Unit = when (val res = state.fetchResult) {
        is IsNewRepository, is IsNewRepoAndNewMirror, is IsNewMirror -> onAddRepo
        is IsExistingRepository -> {
            { onExistingRepo(res.existingRepoId) }
        }
        is IsExistingMirror -> {
            { onExistingRepo(res.existingRepoId) }
        }
        else -> error("Unexpected fetch state: ${state.fetchResult}")
    }

    val warningText: String? = when (state.fetchResult) {
        is IsNewRepository -> null
        is IsNewRepoAndNewMirror -> stringResource(
            R.string.repo_and_mirror_add_both_info,
            state.fetchUrl
        )
        is IsNewMirror -> stringResource(R.string.repo_mirror_add_info, state.fetchUrl)
        is IsExistingRepository -> stringResource(R.string.repo_exists)
        is IsExistingMirror -> stringResource(R.string.repo_mirror_exists, state.fetchUrl)
        else -> error("Unexpected fetch state: ${state.fetchResult}")
    }

    Column(
        verticalArrangement = spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = spacedBy(16.dp),
            verticalAlignment = CenterVertically,
        ) {
            RepoIcon(repo, Modifier.size(48.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = repo.getName(localeList) ?: "Unknown Repository",
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = repo.address.replaceFirst("https://", ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = repo.timestamp.asRelativeTimeString(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (warningText != null) Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.inverseSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = warningText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }

        FDroidButton(
            text = buttonText,
            onClick = buttonAction,
            modifier = Modifier.align(End),
        )

        val description = if (isDevPreview) {
            LoremIpsum(42).values.joinToString(" ")
        } else {
            repo.getDescription(localeList)
        }
        if (description != null) Text(
            // repos are still messing up their line breaks
            text = description.replace("\n", " "),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 720, heightDp = 360)
fun RepoPreviewScreenNewMirrorPreview() {
    val repo = getRepository("https://example.org")
    FDroidContent {
        RepoPreviewHeader(
            Fetching("https://mirror.example.org", repo, emptyList(), IsNewMirror(0L)),
            onAddRepo = { },
            onExistingRepo = {},
            localeList = LocaleListCompat.getDefault(),
        )
    }
}

@Composable
@Preview
fun RepoPreviewScreenNewRepoAndNewMirrorPreview() {
    val repo = getRepository("https://example.org")
    FDroidContent {
        RepoPreviewHeader(
            state = Fetching(
                fetchUrl = "https://mirror.example.org",
                receivedRepo = repo,
                apps = emptyList(),
                fetchResult = IsNewRepoAndNewMirror,
            ),
            onAddRepo = { },
            onExistingRepo = {},
            localeList = LocaleListCompat.getDefault(),
        )
    }
}

@Preview
@Composable
fun RepoPreviewScreenExistingRepoPreview() {
    val address = "https://example.org"
    val repo = getRepository(address)
    FDroidContent {
        RepoPreviewHeader(
            Fetching(address, repo, emptyList(), IsExistingRepository(0L)),
            onAddRepo = { },
            onExistingRepo = {},
            localeList = LocaleListCompat.getDefault(),
        )
    }
}

@Preview
@Composable
fun RepoPreviewScreenExistingMirrorPreview() {
    val repo = getRepository("https://example.org")
    FDroidContent {
        RepoPreviewHeader(
            Fetching("https://mirror.example.org", repo, emptyList(), IsExistingMirror(0L)),
            onAddRepo = { },
            onExistingRepo = {},
            localeList = LocaleListCompat.getDefault(),
        )
    }
}
