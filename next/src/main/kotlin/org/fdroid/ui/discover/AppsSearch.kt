package org.fdroid.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.ui.NavigationKey
import org.fdroid.ui.categories.CategoryCard
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.lists.AppListRow
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppsSearch(
    searchBarState: SearchBarState,
    searchResults: SearchResults?,
    onSearch: suspend (String) -> Unit,
    onNav: (NavigationKey) -> Unit,
    onSearchCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        state = searchBarState,
        inputField = {
            // InputField is different from ExpandedFullScreenSearchBar to separate textFieldState
            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = rememberTextFieldState(),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                onSearch = { },
            )
        },
        modifier = modifier,
    )
    val textFieldState = rememberTextFieldState()
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = {
            AppSearchInputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = onSearch,
                onSearchCleared = {
                    textFieldState.setTextAndPlaceCursorAtEnd("")
                    onSearchCleared()
                },
            )
        },
    ) {
        if (searchResults == null) {
            if (textFieldState.text.length >= 3) BigLoadingIndicator()
        } else if (searchResults.apps.isEmpty()) {
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                        item = item, isSelected = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .clickable {
                                onNav(NavigationKey.AppDetails(item.packageName))
                            }
                    )
                }
            }
        }
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
                CategoryCard(categoryItem = item, onSelected = {
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
                AppListItem(1, "1", "This is app 1", "It has summary 2", 0, null),
                AppListItem(2, "2", "This is app 2", "It has summary 2", 0, null),
            )
            AppsSearch(state, SearchResults(apps, categories), {}, {}, {})
        }
    }
}
