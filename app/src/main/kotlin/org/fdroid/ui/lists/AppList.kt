package org.fdroid.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition.Companion.Below
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
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
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.search.TopSearchBar
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.OnboardingCard
import org.fdroid.ui.utils.TopAppBarButton
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
                TopSearchBar(
                    onSearch = appListInfo.actions::onSearch,
                    onSearchCleared = onSearchCleared,
                    onHideSearch = {
                        searchActive = false
                        onSearchCleared()
                    },
                    actions = {
                        FilterButton(
                            showFilterBadge = appListInfo.model.showFilterBadge,
                            toggleFilterVisibility = appListInfo.actions::toggleFilterVisibility,
                        )
                    }
                )
            } else TopAppBar(
                title = {
                    Text(
                        text = appListInfo.list.title,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                },
                navigationIcon = {
                    BackButton(onClick = {
                        if (searchActive) searchActive = false else onBackClicked()
                    })
                },
                actions = {
                    TopAppBarButton(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.menu_search),
                        onClick = { searchActive = true },
                    )
                    FilterButton(
                        showFilterBadge = appListInfo.model.showFilterBadge,
                        toggleFilterVisibility = appListInfo.actions::toggleFilterVisibility,
                        modifier = Modifier.hintAnchor(
                            state = hintAnchor,
                            shape = RoundedCornerShape(16.dp),
                        )
                    )
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
                .fillMaxSize()
                .imePadding()
        ) {
            val apps = appListInfo.model.apps
            if (apps == null) BigLoadingIndicator()
            else if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_filter_no_results),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                )
            } else LazyColumn(
                state = listState,
                contentPadding = paddingValues + PaddingValues(top = 8.dp),
                verticalArrangement = spacedBy(8.dp),
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

@Composable
private fun FilterButton(
    showFilterBadge: Boolean,
    toggleFilterVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TooltipBox(
        positionProvider =
        TooltipDefaults.rememberTooltipPositionProvider(Below),
        tooltip = { PlainTooltip { Text(stringResource(R.string.filter)) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = toggleFilterVisibility,
            modifier = modifier,
        ) {
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
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        val model = AppListModel(
            apps = listOf(
                AppListItem(1, "1", "This is app 1", "It has summary 2", 0, false, true),
                AppListItem(2, "2", "This is app 2", "It has summary 2", 0, true, true),
            ),
            showFilterBadge = true,
            sortBy = AppListSortOrder.NAME,
            filterIncompatible = true,
            categories = null,
            filteredCategoryIds = emptySet(),
            antiFeatures = null,
            filteredAntiFeatureIds = emptySet(),
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
            showFilterBadge = false,
            sortBy = AppListSortOrder.NAME,
            filterIncompatible = false,
            categories = null,
            filteredCategoryIds = emptySet(),
            antiFeatures = null,
            filteredAntiFeatureIds = emptySet(),
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
            showFilterBadge = false,
            sortBy = AppListSortOrder.NAME,
            filterIncompatible = false,
            categories = null,
            filteredCategoryIds = emptySet(),
            antiFeatures = null,
            filteredAntiFeatureIds = emptySet(),
            repositories = emptyList(),
            filteredRepositoryIds = emptySet(),
        )
        val info = getAppListInfo(model)
        AppList(appListInfo = info, currentPackageName = null, onBackClicked = {}, onItemClick = {})
    }
}
