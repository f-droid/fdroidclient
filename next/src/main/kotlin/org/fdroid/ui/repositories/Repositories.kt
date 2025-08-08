package org.fdroid.ui.repositories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
fun Repositories(
    repositories: List<RepositoryItem>?,
    currentRepositoryId: Long?,
    onRepositorySelected: (RepositoryItem) -> Unit,
    onAddRepo: () -> Unit,
    onBackClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = {
                    Text(stringResource(R.string.app_details_repositories))
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRepo,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.menu_add_repo),
                )
            }
        }
    ) { paddingValues ->
        if (repositories == null) BigLoadingIndicator()
        else RepositoriesList(
            repositories = repositories,
            currentRepositoryId = currentRepositoryId,
            onRepositorySelected = {
                onRepositorySelected(it)
            },
            modifier = Modifier
                .padding(paddingValues),
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
)
@Composable
fun RepositoriesScaffoldLoadingPreview() {
    FDroidContent {
        Repositories(null, null, {}, {}) { }
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
)
@Composable
fun RepositoriesScaffoldPreview() {
    FDroidContent {
        val repos = listOf(
            RepositoryItem(
                repoId = 1,
                address = "http://example.org",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 1,
                enabled = true,
                name = "My first repository",
            ),
            RepositoryItem(
                repoId = 2,
                address = "http://example.com",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 2,
                enabled = true,
                name = "My second repository",
            ),
        )
        Repositories(repos, repos[0].repoId, {}, {}) { }
    }
}
