package org.fdroid.ui.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.BottomBar
import org.fdroid.ui.MainOverFlowMenu
import org.fdroid.ui.NavigationKey
import org.fdroid.ui.categories.CategoryList
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.topBarMenuItems
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun Discover(
    discoverModel: DiscoverModel,
    numUpdates: Int,
    isBigScreen: Boolean,
    onSearch: suspend (String) -> Unit,
    onSearchCleared: () -> Unit,
    onListTap: (AppListType) -> Unit,
    onAppTap: (AppDiscoverItem) -> Unit,
    onNav: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    topBarMenuItems.forEach { dest ->
                        IconButton(onClick = { onNav(dest.id) }) {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = stringResource(dest.label),
                            )
                        }
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = !menuExpanded }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                        )
                    }
                    MainOverFlowMenu(menuExpanded, {
                        menuExpanded = false
                        onNav(it.id)
                    }) {
                        menuExpanded = false
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (!isBigScreen) BottomBar(numUpdates, NavigationKey.Discover, onNav)
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        val scope = rememberCoroutineScope()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            when (discoverModel) {
                is LoadingDiscoverModel -> {
                    AnimatedVisibility(discoverModel.isFirstStart) {
                        Text(
                            stringResource(R.string.first_start_loading),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                    BigLoadingIndicator()
                }
                is LoadedDiscoverModel -> {
                    AppsSearch(
                        searchBarState = searchBarState,
                        searchResults = discoverModel.searchResults,
                        onSearch = onSearch,
                        onNav = {
                            scope.launch {
                                // workaround for crash when navigating back:
                                // IllegalStateException: LayoutCoordinate operations are only valid
                                // when isAttached is true
                                searchBarState.animateToCollapsed()
                            }
                            onNav(it)
                        },
                        onSearchCleared = onSearchCleared,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                    if (discoverModel.newApps.isNotEmpty()) {
                        val listNew = AppListType.New(stringResource(R.string.app_list_new))
                        AppCarousel(
                            title = listNew.title,
                            apps = discoverModel.newApps,
                            onTitleTap = { onListTap(listNew) },
                            onAppTap = onAppTap,
                        )
                    }
                    val listRecentlyUpdated = AppListType.RecentlyUpdated(
                        stringResource(R.string.app_list_recently_updated),
                    )
                    AppCarousel(
                        title = listRecentlyUpdated.title,
                        apps = discoverModel.recentlyUpdatedApps,
                        onTitleTap = { onListTap(listRecentlyUpdated) },
                        onAppTap = onAppTap,
                    )
                    val listAll = AppListType.All(
                        title = stringResource(R.string.app_list_all),
                    )
                    FilledTonalButton(
                        onClick = { onListTap(listAll) },
                        modifier = Modifier
                            .align(End)
                            .padding(16.dp),
                    ) {
                        Text(listAll.title)
                    }
                }
                NoEnabledReposDiscoverModel -> {
                    Text(
                        text = stringResource(R.string.no_repos_enabled),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 64.dp)
                    )
                }
            }
            AnimatedVisibility(discoverModel is LoadedDiscoverModel) {
                CategoryList(
                    categoryMap = (discoverModel as LoadedDiscoverModel).categories,
                    onNav = onNav,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
fun LoadingDiscoverPreview() {
    FDroidContent {
        Discover(
            discoverModel = LoadingDiscoverModel(true),
            numUpdates = 23,
            isBigScreen = false,
            onListTap = {},
            onAppTap = {},
            onNav = {},
            onSearch = {},
            onSearchCleared = {},
        )
    }
}

@Preview
@Composable
private fun NoEnabledReposPreview() {
    FDroidContent {
        Discover(
            discoverModel = NoEnabledReposDiscoverModel,
            numUpdates = 0,
            isBigScreen = false,
            onListTap = {},
            onAppTap = {},
            onNav = {},
            onSearch = {},
            onSearchCleared = {},
        )
    }
}
