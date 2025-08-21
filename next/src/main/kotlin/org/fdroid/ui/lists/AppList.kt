package org.fdroid.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import org.fdroid.database.AppListSortOrder
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    FlowPreview::class
)
fun AppList(
    appListInfo: AppListInfo,
    currentPackageName: String?,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            if (searchActive) {
                val onSearchCleared = { appListInfo.onSearch("") }
                TopSearchBar(onSearch = appListInfo::onSearch, onSearchCleared) {
                    searchActive = false
                    onSearchCleared()
                }
            } else TopAppBar(
                title = {
                    Text(
                        text = appListInfo.model.list.title,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (searchActive) searchActive = false else onBackClicked()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchActive = true }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.menu_search),
                        )
                    }
                    IconButton(onClick = { appListInfo.toggleFilterVisibility() }) {
                        val showFilterBadge =
                            appListInfo.model.filteredRepositoryIds.isNotEmpty() ||
                                appListInfo.model.filteredCategoryIds.isNotEmpty()
                        BadgedBox(badge = {
                            if (showFilterBadge) Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = stringResource(
                                    R.string.search_filter_no_results
                                ),
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
            modifier = Modifier.padding(paddingValues),
        ) {
            val apps = appListInfo.model.apps
            if (apps == null) BigLoadingIndicator()
            else if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_filter_no_results),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                )
            } else LazyColumn(
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
                    AppListRow(
                        item = navItem,
                        isSelected = isSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(horizontal = 8.dp)
                            .then(interactionModifier)
                    )
                }
                item(contentType = "S") {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                }
            }
            // Bottom Sheet
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            if (appListInfo.model.areFiltersShown) {
                ModalBottomSheet(
                    modifier = Modifier.fillMaxHeight(),
                    sheetState = sheetState,
                    onDismissRequest = { appListInfo.toggleFilterVisibility() },
                ) {
                    AppsFilter(info = appListInfo)
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
                AppListItem(1, "1", "This is app 1", "It has summary 2", 0, null),
                AppListItem(2, "2", "This is app 2", "It has summary 2", 0, null),
            ),
            areFiltersShown = true,
            sortBy = AppListSortOrder.NAME,
            categories = null,
            filteredCategoryIds = emptySet(),
            repositories = emptyList(),
            filteredRepositoryIds = emptySet(),
        )
        val info = object : AppListInfo {
            override val model: AppListModel = model
            override fun toggleFilterVisibility() {}
            override fun sortBy(sort: AppListSortOrder) {}
            override fun addCategory(categoryId: String) {}
            override fun removeCategory(categoryId: String) {}
            override fun addRepository(repoId: Long) {}
            override fun removeRepository(repoId: Long) {}
            override fun onSearch(query: String) {}
        }
        AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
}

@Preview
@Composable
private fun PreviewLoading() {
    FDroidContent {
        val model = AppListModel(
            list = AppListType.New("New"),
            apps = null,
            areFiltersShown = true,
            sortBy = AppListSortOrder.NAME,
            categories = null,
            filteredCategoryIds = emptySet(),
            repositories = emptyList(),
            filteredRepositoryIds = emptySet(),
        )
        val info = object : AppListInfo {
            override val model: AppListModel = model
            override fun toggleFilterVisibility() {}
            override fun sortBy(sort: AppListSortOrder) {}
            override fun addCategory(categoryId: String) {}
            override fun removeCategory(categoryId: String) {}
            override fun addRepository(repoId: Long) {}
            override fun removeRepository(repoId: Long) {}
            override fun onSearch(query: String) {}
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
            apps = emptyList(),
            areFiltersShown = true,
            sortBy = AppListSortOrder.NAME,
            categories = null,
            filteredCategoryIds = emptySet(),
            repositories = emptyList(),
            filteredRepositoryIds = emptySet(),
        )
        val info = object : AppListInfo {
            override val model: AppListModel = model
            override fun toggleFilterVisibility() {}
            override fun sortBy(sort: AppListSortOrder) {}
            override fun addCategory(categoryId: String) {}
            override fun removeCategory(categoryId: String) {}
            override fun addRepository(repoId: Long) {}
            override fun removeRepository(repoId: Long) {}
            override fun onSearch(query: String) {}
        }
        AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
}
