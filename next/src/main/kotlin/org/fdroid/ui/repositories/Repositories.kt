package org.fdroid.ui.repositories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viktormykhailiv.compose.hints.rememberHint
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.OnboardingCard
import org.fdroid.ui.utils.getHintOverlayColor
import org.fdroid.ui.utils.getRepositoriesInfo

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Repositories(
    info: RepositoryInfo,
    onBackClicked: () -> Unit,
) {
    val hintController = rememberHintController(
        overlay = getHintOverlayColor(),
    )
    val hint = rememberHint {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            OnboardingCard(
                title = stringResource(R.string.repo_list_info_title),
                message = stringResource(R.string.repo_list_info_text),
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                onGotIt = {
                    info.onOnboardingSeen()
                    hintController.dismiss()
                },
            )
        }
    }
    val hintAnchor = rememberHintAnchorState(hint)
    LaunchedEffect(info.model.showOnboarding) {
        if (info.model.showOnboarding) {
            hintController.show(hintAnchor)
            info.onOnboardingSeen()
        }
    }
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
                // TODO show when repos were last updated
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = info::onAddRepo,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.menu_add_repo),
                )
            }
        }
    ) { paddingValues ->
        if (info.model.repositories == null) BigLoadingIndicator()
        else RepositoriesList(
            info = info,
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
        val model = RepositoryModel(null, false)
        val info = getRepositoriesInfo(model)
        Repositories(info) {}
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
                enabled = false,
                name = "My second repository",
            ),
        )
        val model = RepositoryModel(repos, false)
        val info = getRepositoriesInfo(model, repos[0].repoId)
        Repositories(info) { }
    }
}
