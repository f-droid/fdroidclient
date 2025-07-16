package org.fdroid.basic.ui.main.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
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
import org.fdroid.basic.ui.NavigationKey
import org.fdroid.basic.ui.categories.CategoryList
import org.fdroid.basic.ui.main.BottomBar
import org.fdroid.basic.ui.main.MainOverFlowMenu
import org.fdroid.basic.ui.main.lists.AppList
import org.fdroid.basic.ui.main.topBarMenuItems
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun Discover(
    discoverModel: DiscoverModel,
    numUpdates: Int,
    isBigScreen: Boolean,
    modifier: Modifier = Modifier,
    onTitleTap: (AppList) -> Unit,
    onAppTap: (AppNavigationItem) -> Unit,
    onNav: (NavKey) -> Unit,
) {
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("F-Droid")
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
                            "This is the first start, loading repositories...",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        LoadingIndicator(Modifier.size(128.dp))
                    }
                }
                is LoadedDiscoverModel -> {
                    AppsSearch(
                        searchBarState = searchBarState,
                        categories = discoverModel.categories,
                        onItemClick = onAppTap,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    AppCarousel(
                        title = AppList.New.title,
                        apps = discoverModel.newApps,
                        onTitleTap = { onTitleTap(AppList.New) },
                        onAppTap = onAppTap,
                    )
                    AppCarousel(
                        title = AppList.RecentlyUpdated.title,
                        apps = discoverModel.recentlyUpdatedApps,
                        onTitleTap = { onTitleTap(AppList.RecentlyUpdated) },
                        onAppTap = onAppTap,
                    )
                    FilledTonalButton(
                        onClick = { onTitleTap(AppList.All) },
                        modifier = Modifier
                            .align(End)
                            .padding(16.dp),
                    ) {
                        Text(AppList.All.title)
                    }
                }
                NoEnabledReposDiscoverModel -> {
                    Text("No repositories enabled.\nEnable at least one repository to see apps.")
                }
            }
            AnimatedVisibility(discoverModel is LoadedDiscoverModel) {
                val categories = (discoverModel as LoadedDiscoverModel).categories
                // TODO remove max height hack
                CategoryList(categories, Modifier.heightIn(max = 2000.dp))
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
            onTitleTap = {},
            onAppTap = {},
            onNav = {},
        )
    }
}
