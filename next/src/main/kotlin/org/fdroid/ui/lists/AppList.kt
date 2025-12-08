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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.viktormykhailiv.compose.hints.hintAnchor
import com.viktormykhailiv.compose.hints.rememberHint
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.OnboardingCard
import org.fdroid.ui.utils.getAppListInfo
import org.fdroid.ui.utils.getHintOverlayColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppList(
    appListInfo: AppListInfo,
    currentPackageName: String?,
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())

    val hintController = rememberHintController(
        overlay = getHintOverlayColor(),
    )
    val hint = rememberHint {
        OnboardingCard(
            title = stringResource(R.string.onboarding_app_list_filter_title),
            message = stringResource(R.string.onboarding_app_list_filter_message),
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            onGotIt = {
                appListInfo.actions.onOnboardingSeen()
                hintController.dismiss()
            },
        )
    }
    val hintAnchor = rememberHintAnchorState(hint)
    LaunchedEffect(appListInfo.showOnboarding) {
        if (appListInfo.showOnboarding) {
            hintController.show(hintAnchor)
            appListInfo.actions.onOnboardingSeen()
        }
    }

    Scaffold(
        topBar = {
            if (searchActive) {
                val onSearchCleared = { appListInfo.actions.onSearch("") }
                TopSearchBar(onSearch = appListInfo.actions::onSearch, onSearchCleared) {
                    searchActive = false
                    onSearchCleared()
                }
            } else TopAppBar(
                title = {
                    Text(
                        text = appListInfo.list.title,
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
                    IconButton(
                        onClick = { appListInfo.actions.toggleFilterVisibility() },
                        modifier = Modifier.hintAnchor(
                            state = hintAnchor,
                            shape = RoundedCornerShape(16.dp),
                        )
                    ) {
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
                                contentDescription = stringResource(R.string.filter),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        val listState = rememberSaveable(saver = LazyListState.Saver) {
            LazyListState()
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
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
                state = listState,
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
            if (appListInfo.showFilters) {
                ModalBottomSheet(
                    modifier = Modifier.fillMaxHeight(),
                    sheetState = sheetState,
                    onDismissRequest = { appListInfo.actions.toggleFilterVisibility() },
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
            apps = listOf(
                AppListItem(1, "1", "This is app 1", "It has summary 2", 0, false, true, null),
                AppListItem(2, "2", "This is app 2", "It has summary 2", 0, true, true, null),
            ),
            sortBy = AppListSortOrder.NAME,
            filterIncompatible = true,
            categories = null,
            filteredCategoryIds = emptySet(),
            repositories = emptyList(),
            filteredRepositoryIds = emptySet(),
        )
        val info = getAppListInfo(model)
        AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
}

@Preview
@Composable
private fun PreviewLoading() {
    FDroidContent {
        val model = AppListModel(
            apps = null,
            sortBy = AppListSortOrder.NAME,
            filterIncompatible = false,
            categories = null,
            filteredCategoryIds = emptySet(),
            repositories = emptyList(),
            filteredRepositoryIds = emptySet(),
        )
        val info = getAppListInfo(model)
        AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    FDroidContent {
        val model = AppListModel(
            apps = emptyList(),
            sortBy = AppListSortOrder.NAME,
            filterIncompatible = false,
            categories = null,
            filteredCategoryIds = emptySet(),
            repositories = emptyList(),
            filteredRepositoryIds = emptySet(),
        )
        val info = getAppListInfo(model)
        AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
}
