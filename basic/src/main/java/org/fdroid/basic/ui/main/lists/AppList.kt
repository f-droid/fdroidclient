package org.fdroid.basic.ui.main.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import org.fdroid.basic.details.AppDetailsItem
import org.fdroid.basic.ui.main.apps.MinimalApp

sealed class AppList(val title: String) {
    data object New : AppList("New apps")
    data object RecentlyUpdated : AppList("Recently updated")
    data object All : AppList("All apps")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppList(
    appList: AppList,
    filterInfo: FilterInfo,
    currentItem: AppDetailsItem?,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onItemClick: (MinimalApp) -> Unit,
) {
    val scrollBehavior = exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val addedRepos = remember { mutableStateListOf<String>() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(appList.title)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "back")
                    }
                },
                actions = {
                    IconButton(onClick = { filterInfo.toggleFilterVisibility() }) {
                        val showFilterBadge = addedRepos.isNotEmpty() ||
                            filterInfo.model.addedCategories.isNotEmpty()
                        BadgedBox(badge = {
                            if (showFilterBadge) Badge(containerColor = MaterialTheme.colorScheme.secondary)
                        }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = null,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues),
        ) {
            AppsFilter(
                filter = filterInfo,
                addedRepos = addedRepos,
                modifier = Modifier.background(TopAppBarDefaults.topAppBarColors().containerColor),
            )
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.then(
                    if (currentItem == null) Modifier
                    else Modifier.selectableGroup()
                ),
            ) {
                val apps = filterInfo.model.apps
                items(apps, key = { it.packageName }, contentType = { "A" }) { navItem ->
                    val isSelected = currentItem?.app?.packageName == navItem.packageName
                    val interactionModifier = if (currentItem == null) {
                        Modifier.clickable(
                            onClick = { onItemClick(navItem) }
                        )
                    } else {
                        Modifier.selectable(
                            selected = isSelected,
                            onClick = { onItemClick(navItem) }
                        )
                    }
                    val modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .padding(horizontal = 8.dp)
                        .then(interactionModifier)
                    AppItem(
                        name = navItem.name,
                        summary = navItem.summary,
                        downloadRequest = navItem.iconDownloadRequest,
                        isNew = navItem.isNew,
                        isSelected = isSelected,
                        modifier = modifier,
                    )
                }
                item(contentType = "S") {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                }
            }
        }
    }
}
