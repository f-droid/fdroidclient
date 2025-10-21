package org.fdroid.ui.repositories.add

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.fdroid.R
import org.fdroid.database.MinimalApp
import org.fdroid.download.getImageModel
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.index.v2.FileV2
import org.fdroid.repo.FetchResult.IsNewRepoAndNewMirror
import org.fdroid.repo.FetchResult.IsNewRepository
import org.fdroid.repo.Fetching
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.lists.AppListRow
import org.fdroid.ui.utils.getRepository

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddRepoPreviewScreen(
    state: Fetching,
    modifier: Modifier = Modifier,
    onAddRepo: () -> Unit,
    onExistingRepo: (Long) -> Unit,
) {
    val localeList = LocaleListCompat.getDefault()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalArrangement = spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        item {
            RepoPreviewHeader(
                state = state,
                onAddRepo = onAddRepo,
                onExistingRepo = onExistingRepo,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(horizontal = 8.dp),
                localeList = localeList,
            )
        }
        if (state.fetchResult == null ||
            state.fetchResult is IsNewRepository ||
            state.fetchResult is IsNewRepoAndNewMirror
        ) {
            item {
                Row(
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.repo_preview_included_apps),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = state.apps.size.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (!state.done) {
                        LinearWavyProgressIndicator(modifier = Modifier.weight(1f))
                    }
                }
            }
            items(items = state.apps, key = { it.packageName }) { app ->
                val repo = state.receivedRepo ?: error("no repo")
                val item = AppListItem(
                    repoId = repo.repoId,
                    packageName = app.packageName,
                    name = app.name ?: "Unknown app",
                    summary = app.summary ?: "",
                    iconModel = app.getIcon(localeList)?.getImageModel(repo),
                    lastUpdated = 1L,
                    isCompatible = true,
                )
                AppListRow(
                    item = item,
                    isSelected = false,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    val address = "https://example.org"
    val repo = getRepository(address)
    val app1 = object : MinimalApp {
        override val repoId = 0L
        override val packageName = "org.example"
        override val name: String = "App 1 with a long name"
        override val summary: String = "Summary of App1 which can also be a bit longer"
        override fun getIcon(localeList: LocaleListCompat): FileV2? = null
    }
    val app2 = object : MinimalApp {
        override val repoId = 0L
        override val packageName = "com.example"
        override val name: String = "App 2 with a name that is even longer than the first app"
        override val summary: String =
            "Summary of App2 which can also be a bit longer, even longer than other apps."

        override fun getIcon(localeList: LocaleListCompat): FileV2? = null
    }
    val app3 = object : MinimalApp {
        override val repoId = 0L
        override val packageName = "net.example"
        override val name: String = "App 3"
        override val summary: String = "short summary"

        override fun getIcon(localeList: LocaleListCompat): FileV2? = null
    }
    FDroidContent(pureBlack = true) {
        AddRepoPreviewScreen(
            Fetching(address, repo, listOf(app1, app2, app3), IsNewRepository),
            onAddRepo = { },
            onExistingRepo = {},
        )
    }
}
