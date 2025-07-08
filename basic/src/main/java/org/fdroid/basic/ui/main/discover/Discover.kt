package org.fdroid.basic.ui.main.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fdroid.basic.ui.categories.Category
import org.fdroid.basic.ui.categories.CategoryList
import org.fdroid.basic.ui.main.MainOverFlowMenu
import org.fdroid.basic.ui.main.lists.AppList
import org.fdroid.basic.ui.main.topBarMenuItems

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Discover(
    apps: List<AppNavigationItem>,
    categories: List<Category>?,
    modifier: Modifier = Modifier,
    onTitleTap: (AppList) -> Unit,
    onAppTap: (AppNavigationItem) -> Unit,
    onMainNav: (String) -> Unit,
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
                        IconButton(onClick = { onMainNav(dest.id) }) {
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
                        onMainNav(it.id)
                    }) {
                        menuExpanded = false
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            AppsSearch(searchBarState, categories, onAppTap, modifier = Modifier.padding(top = 8.dp))
            AppCarousel(
                title = AppList.New.title,
                apps = apps.filter { it.isNew },
                onTitleTap = { onTitleTap(AppList.New) },
                onAppTap = onAppTap,
            )
            AppCarousel(
                title = AppList.RecentlyUpdated.title,
                apps = apps.filter { !it.isNew },
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
            CategoryList(categories, Modifier.heightIn(max = 1000.dp))
        }
    }
}
