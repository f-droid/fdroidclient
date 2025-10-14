package org.fdroid.ui.repositories.add

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import org.fdroid.index.IndexUpdateResult
import org.fdroid.next.R
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.AddRepoState
import org.fdroid.repo.Added
import org.fdroid.repo.Adding
import org.fdroid.repo.FetchResult
import org.fdroid.repo.Fetching
import org.fdroid.repo.None
import org.fdroid.repo.RepoUpdateWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepo(
    state: AddRepoState,
    onFetchRepo: (String) -> Unit,
    onAddRepo: () -> Unit,
    onExistingRepo: (Long) -> Unit,
    onRepoAdded: (String, Long) -> Unit,
    onBackClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                title = {
                    Text(
                        text = if (state is Fetching) {
                            when (state.fetchResult) {
                                is FetchResult.IsNewMirror,
                                is FetchResult.IsExistingMirror -> {
                                    stringResource(R.string.repo_add_mirror)
                                }
                                else -> stringResource(R.string.repo_add_new_title)
                            }
                        } else {
                            stringResource(R.string.repo_add_new_title)
                        },
                    )
                },
            )
        },
    ) { paddingValues ->
        when (state) {
            None -> AddRepoIntroContent(onFetchRepo, Modifier.padding(paddingValues))
            is Fetching -> {
                if (state.receivedRepo == null) {
                    AddRepoProgressScreen(
                        text = stringResource(R.string.repo_state_fetching),
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    AddRepoPreviewScreen(
                        state = state,
                        onAddRepo = onAddRepo,
                        onExistingRepo = onExistingRepo,
                        modifier = Modifier.padding(paddingValues),
                    )
                }
            }
            Adding -> AddRepoProgressScreen(
                text = stringResource(R.string.repo_state_adding),
                modifier = Modifier.padding(paddingValues)
            )
            is Added -> {
                val context = LocalContext.current
                LaunchedEffect(state) {
                    if (state.updateResult is IndexUpdateResult.Error) {
                        // try updating newly added repo again
                        RepoUpdateWorker.updateNow(context, state.repo.repoId)
                    }
                    // tell UI that repo got added, so it can show info on it
                    val name = state.repo.getName(LocaleListCompat.getDefault()) ?: "Unknown Repo"
                    onRepoAdded(name, state.repo.repoId)
                }
            }
            is AddRepoError -> AddRepoErrorScreen(state, Modifier.padding(paddingValues))
        }
    }
}
