package org.fdroid.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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
    val searchFieldState = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }
    val scrollBehavior = exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val addedRepos = remember { mutableStateListOf<String>() }
    Scaffold(
        topBar = {
            if (searchActive) {
                SearchBarDefaults.InputField(
                    state = searchFieldState,
                    leadingIcon = {
                        IconButton(onClick = { searchActive = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    onSearch = appListInfo::onSearch,
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .statusBarsPadding()
                        .height(TopAppBarDefaults.TopAppBarExpandedHeight)
                        .padding(horizontal = 8.dp)
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    snapshotFlow { searchFieldState.text }
                        .debounce(500)
                        .collectLatest {
                            if (it.length >= 2) {
                                appListInfo.onSearch(searchFieldState.text.toString())
                            }
                        }
                }
            } else TopAppBar(
                title = {
                    Text(
                        appListInfo.model.list.title,
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
                            Icons.Filled.Search,
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = { appListInfo.toggleFilterVisibility() }) {
                        val showFilterBadge = addedRepos.isNotEmpty() ||
                            appListInfo.model.filteredCategoryIds.isNotEmpty()
                        BadgedBox(badge = {
                            if (showFilterBadge) Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                            )
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
private fun PreviewEmpty() {
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
