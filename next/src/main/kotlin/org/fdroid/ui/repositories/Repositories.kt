package org.fdroid.ui.repositories

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viktormykhailiv.compose.hints.HintHost
import com.viktormykhailiv.compose.hints.rememberHint
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import org.fdroid.R
import org.fdroid.download.NetworkState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.OnboardingCard
import org.fdroid.ui.utils.getHintOverlayColor
import org.fdroid.ui.utils.getRepositoriesInfo
import org.fdroid.ui.utils.repoItems

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun Repositories(
    info: RepositoryInfo,
    isBigScreen: Boolean,
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
        if (!isBigScreen && info.model.showOnboarding) {
            hintController.show(hintAnchor)
            info.onOnboardingSeen()
        }
    }
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val showFab by remember {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
                true
            } else if (listState.isScrollInProgress) {
                false
            } else {
                listState.canScrollForward
            }
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
                subtitle = {
                    val lastUpdated = info.model.lastCheckForUpdate
                    Text(stringResource(R.string.repo_last_update_check, lastUpdated))
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(showFab) {
                FloatingActionButton(
                    onClick = info::onAddRepo,
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.menu_add_repo),
                    )
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        if (info.model.repositories == null) BigLoadingIndicator()
        else RepositoriesList(
            info = info,
            listState = listState,
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
    HintHost {
        FDroidContent {
            val model = RepositoryModel(
                repositories = null,
                showOnboarding = false,
                lastCheckForUpdate = "never",
                networkState = NetworkState(isOnline = true, isMetered = false),
            )
            val info = getRepositoriesInfo(model)
            Repositories(info, true) {}
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
)
@Composable
private fun RepositoriesScaffoldPreview() {
    HintHost {
        FDroidContent {
            val model = RepositoryModel(
                repositories = repoItems,
                showOnboarding = false,
                lastCheckForUpdate = "42min. ago",
                networkState = NetworkState(isOnline = true, isMetered = false),
            )
            val info = getRepositoriesInfo(model, repoItems[0].repoId)
            Repositories(info, true) { }
        }
    }
}
