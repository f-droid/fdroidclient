package org.fdroid.ui.lists

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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.database.AppListSortOrder
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun AppList(
    appListInfo: AppListInfo,
    currentPackageName: String?,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val scrollBehavior = exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val addedRepos = remember { mutableStateListOf<String>() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(appListInfo.model.list.title)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "back")
                    }
                },
                actions = {
                    IconButton(onClick = { appListInfo.toggleFilterVisibility() }) {
                        val showFilterBadge = addedRepos.isNotEmpty() ||
                            appListInfo.model.addedCategories.isNotEmpty()
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
                info = appListInfo,
                addedRepos = addedRepos,
                modifier = Modifier.background(TopAppBarDefaults.topAppBarColors().containerColor),
            )
            val apps = appListInfo.model.apps
            if (apps == null) BigLoadingIndicator()
            else LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.then(
                    if (currentPackageName == null) Modifier
                    else Modifier.selectableGroup()
                ),
            ) {
                items(apps, key = { it.packageName }, contentType = { "A" }) { navItem ->
                    val isSelected = currentPackageName == navItem.packageName
                    val interactionModifier = if (currentPackageName == null) {
                        Modifier.clickable(
                            onClick = { onItemClick(navItem.packageName) }
                        )
                    } else {
                        Modifier.selectable(
                            selected = isSelected,
                            onClick = { onItemClick(navItem.packageName) }
                        )
                    }
                    val modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .padding(horizontal = 8.dp)
                        .then(interactionModifier)
                    AppListRow(
                        item = navItem,
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

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        val model = AppListModel(
            list = AppListType.New("New"),
            apps = listOf(
                AppListItem("1", "This is app 1", "It has summary 2", 0, null),
                AppListItem("2", "This is app 2", "It has summary 2", 0, null),
            ),
            areFiltersShown = true,
            sortBy = AppListSortOrder.NAME,
            allCategories = null,
            addedCategories = emptyList(),
        )
        val info = object : AppListInfo {
            override val model: AppListModel = model
            override fun toggleFilterVisibility() {}
            override fun sortBy(sort: AppListSortOrder) {}
            override fun addCategory(category: String) {}
            override fun removeCategory(category: String) {}
        }
        AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    FDroidContent {
        val model = AppListModel(
            list = AppListType.New("New"),
            apps = null,
            areFiltersShown = true,
            sortBy = AppListSortOrder.NAME,
            allCategories = null,
            addedCategories = emptyList(),
        )
        val info = object : AppListInfo {
            override val model: AppListModel = model
            override fun toggleFilterVisibility() {}
            override fun sortBy(sort: AppListSortOrder) {}
            override fun addCategory(category: String) {}
            override fun removeCategory(category: String) {}
        }
        AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
}
