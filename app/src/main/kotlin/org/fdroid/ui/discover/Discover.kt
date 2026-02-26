package org.fdroid.ui.discover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateProgress
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.topBarMenuItems
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.TopAppBarButton
import org.fdroid.ui.utils.TopAppBarOverflowButton

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun Discover(
    discoverModel: DiscoverModel,
    onListTap: (AppListType) -> Unit,
    onAppTap: (AppDiscoverItem) -> Unit,
    onNav: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    topBarMenuItems.forEach { dest ->
                        BadgedBox(badge = {
                            val hasRepoIssues =
                                (discoverModel as? LoadedDiscoverModel)?.hasRepoIssues == true
                            if (dest.id == NavigationKey.Repos && hasRepoIssues) Badge(
                                content = null,
                                modifier = Modifier.size(8.dp)
                            )
                        }) {
                            TopAppBarButton(
                                imageVector = dest.icon,
                                contentDescription = stringResource(dest.label),
                                onClick = { onNav(dest.id) },
                            )
                        }
                    }
                    TopAppBarOverflowButton { onDismissRequest ->
                        DiscoverOverFlowMenu {
                            onDismissRequest()
                            onNav(it.id)
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        when (discoverModel) {
            is FirstStartDiscoverModel -> FirstStart(
                networkState = discoverModel.networkState,
                repoUpdateState = discoverModel.repoUpdateState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
            is LoadingDiscoverModel -> BigLoadingIndicator(Modifier.padding(paddingValues))
            is LoadedDiscoverModel -> {
                DiscoverContent(
                    discoverModel = discoverModel,
                    onListTap = onListTap,
                    onAppTap = onAppTap,
                    onNav = onNav,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues),
                )
            }
            NoEnabledReposDiscoverModel -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Text(
                        text = stringResource(R.string.no_repos_enabled),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun FirstStartDiscoverPreview() {
    FDroidContent {
        Discover(
            discoverModel = FirstStartDiscoverModel(
                NetworkState(true, isMetered = false),
                RepoUpdateProgress(1, true, 0.25f),
            ),
            onListTap = {},
            onAppTap = {},
            onNav = {},
        )
    }
}

@Preview
@Composable
fun LoadingDiscoverPreview() {
    FDroidContent {
        Discover(
            discoverModel = LoadingDiscoverModel,
            onListTap = {},
            onAppTap = {},
            onNav = {},
        )
    }
}

@Preview
@Composable
private fun NoEnabledReposPreview() {
    FDroidContent {
        Discover(
            discoverModel = NoEnabledReposDiscoverModel,
            onListTap = {},
            onAppTap = {},
            onNav = {},
        )
    }
}
