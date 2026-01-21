package org.fdroid.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import kotlinx.coroutines.launch
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.categories.CategoryChip
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.lists.AppListRow
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.BigLoadingIndicator

/**
 * The minimum amount of characters we start auto-searching for.
 */
const val SEARCH_THRESHOLD = 2

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun AppsSearch(
    searchBarState: SearchBarState,
    searchResults: SearchResults?,
    onSearch: suspend (String) -> Unit,
    onNav: (NavigationKey) -> Unit,
    onSearchCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val textFieldState = rememberTextFieldState()
    SearchBar(
        state = searchBarState,
        inputField = {
            // InputField is different from ExpandedFullScreenSearchBar to separate onSearch()
            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        // we hide the placeholder, because TalkBack is already saying "Search"
                        modifier = Modifier.semantics { hideFromAccessibility() },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.semantics { hideFromAccessibility() },
                    )
                },
                onSearch = { },
            )
        },
        modifier = modifier,
    )
    // rememberLazyListState done differently, so it refreshes for different searchResults
    val listState = rememberSaveable(searchResults, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
    val inputField = @Composable {
        AppSearchInputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = onSearch,
            onSearchCleared = {
                textFieldState.setTextAndPlaceCursorAtEnd("")
                onSearchCleared()
            },
        )
    }
    val results = @Composable {
        if (searchResults == null) {
            if (textFieldState.text.length >= SEARCH_THRESHOLD) BigLoadingIndicator()
        } else if (searchResults.apps.isEmpty() && textFieldState.text.length >= SEARCH_THRESHOLD) {
            if (searchResults.categories.isNotEmpty()) {
                CategoriesFlowRow(searchResults.categories, onNav)
            }
            Text(
                text = stringResource(R.string.search_no_results),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (searchResults.categories.isNotEmpty()) {
                    item(
                        key = "categories",
                        contentType = "category",
                    ) {
                        CategoriesFlowRow(searchResults.categories, onNav)
                    }
                }
                if (searchResults.apps.isNotEmpty()) {
                    item(
                        key = "appsHeader",
                        contentType = "appsHeader",
                    ) {
                        Column {
                            if (searchResults.categories.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.apps),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }
                    }
                }
                items(
                    searchResults.apps,
                    key = { it.packageName },
                    contentType = { "app" },
                ) { item ->
                    AppListRow(
                        item = item,
                        isSelected = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .clickable {
                                // workaround for https://issuetracker.google.com/issues/471730911
                                // still crashes sometimes, but at least back gesture works
                                scope.launch {
                                    searchBarState.animateToCollapsed()
                                }
                                onNav(NavigationKey.AppDetails(item.packageName))
                            }
                    )
                }
            }
        }
    }
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val isBigScreen =
        windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    if (isBigScreen) {
        ExpandedDockedSearchBar(
            state = searchBarState,
            inputField = inputField,
        ) { results() }
    } else {
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = inputField,
        ) { results() }
    }
}

@Composable
private fun CategoriesFlowRow(categories: List<CategoryItem>, onNav: (NavigationKey) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(
            text = stringResource(R.string.main_menu__categories),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(8.dp)
        )
        FlowRow {
            categories.forEach { item ->
                CategoryChip(categoryItem = item, onClick = {
                    val type = AppListType.Category(item.name, item.id)
                    val navKey = NavigationKey.AppList(type)
                    onNav(navKey)
                })
            }
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchCollapsedPreview() {
    FDroidContent {
        Box(Modifier.fillMaxSize()) {
            val state = rememberSearchBarState()
            AppsSearch(state, null, {}, {}, {})
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchLoadingPreview() {
    FDroidContent {
        Box(Modifier.fillMaxSize()) {
            val state = rememberSearchBarState(SearchBarValue.Expanded)
            AppsSearch(state, null, {}, {}, {})
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchEmptyPreview() {
    FDroidContent {
        Box(Modifier.fillMaxSize()) {
            val state = rememberSearchBarState(SearchBarValue.Expanded)
            AppsSearch(state, SearchResults(emptyList(), emptyList()), {}, {}, {})
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchOnlyCategoriesPreview() {
    FDroidContent {
        Box(Modifier.fillMaxSize()) {
            val state = rememberSearchBarState(SearchBarValue.Expanded)
            val categories = listOf(
                CategoryItem("Bookmark", "Bookmark"),
                CategoryItem("Browser", "Browser"),
                CategoryItem("Calculator", "Calc"),
                CategoryItem("Money", "Money"),
            )
            AppsSearch(state, SearchResults(emptyList(), categories), {}, {}, {})
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchPreview() {
    FDroidContent {
        Box(Modifier.fillMaxSize()) {
            val state = rememberSearchBarState(SearchBarValue.Expanded)
            val categories = listOf(
                CategoryItem("Bookmark", "Bookmark"),
                CategoryItem("Browser", "Browser"),
                CategoryItem("Calculator", "Calc"),
                CategoryItem("Money", "Money"),
            )
            val apps = listOf(
                AppListItem(1, "1", "This is app 1", "It has summary 2", 0, false, true, null),
                AppListItem(2, "2", "This is app 2", "It has summary 2", 0, true, true, null),
            )
            AppsSearch(state, SearchResults(apps, categories), {}, {}, {})
        }
    }
}
