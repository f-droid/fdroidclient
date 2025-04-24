package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fdroid.basic.ui.main.MainOverFlowMenu
import org.fdroid.basic.ui.main.topBarMenuItems

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Discover(
    apps: List<AppNavigationItem>,
    modifier: Modifier = Modifier,
    onTitleTap: () -> Unit,
    onAppTap: (AppNavigationItem) -> Unit,
    onMainNav: (String) -> Unit,
) {
    val searchBarState = rememberSearchBarState()
    val scrollState = rememberScrollState()
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
                .scrollable(scrollState, Orientation.Vertical)
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AppsSearch(searchBarState, onAppTap)
            AppCarousel(
                title = "New apps",
                apps = apps,
                onTitleTap = onTitleTap,
                onAppTap = onAppTap,
            )
            AppCarousel(
                title = "Other apps",
                apps = apps.filter { it.packageName.toInt() % 2 == 0 },
                onTitleTap = onTitleTap,
                onAppTap = onAppTap,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTitleTap)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Text(
                    text = "All apps",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
            TextButton(
                onClick = onTitleTap,
                modifier = Modifier
                    .align(End)
                    .padding(16.dp),
            ) {
                Text("All apps")
            }
        }
    }
}
