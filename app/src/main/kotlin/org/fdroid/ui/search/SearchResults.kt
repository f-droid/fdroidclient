package org.fdroid.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.categories.CategoryChip
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.categories.ChipFlowRow
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.lists.AppListRow
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.BigLoadingIndicator

data class SearchResults(
    val apps: List<AppListItem>,
    val categories: List<CategoryItem>,
)

@Composable
fun SearchResults(
    searchResults: SearchResults?,
    textFieldState: TextFieldState,
    onNav: (NavigationKey) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier,
) {
    // rememberLazyListState done differently, so it refreshes for different searchResults
    val listState = rememberSaveable(searchResults, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
    if (searchResults == null) {
        if (textFieldState.text.length >= SEARCH_THRESHOLD) {
            BigLoadingIndicator(
                modifier
                    .padding(paddingValues)
                    .imePadding()
            )
        }
    } else if (searchResults.apps.isEmpty() && textFieldState.text.length >= SEARCH_THRESHOLD) {
        Column(
            modifier = modifier
                .padding(paddingValues)
                .imePadding()
        ) {
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
        }
    } else {
        LazyColumn(
            state = listState,
            contentPadding = paddingValues,
            modifier = modifier
                .fillMaxSize()
                .imePadding()
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
                            onNav(NavigationKey.AppDetails(item.packageName))
                        }
                )
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
        ChipFlowRow {
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
