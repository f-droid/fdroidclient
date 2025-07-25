package org.fdroid.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.lists.AppListRow
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppsSearch(
    searchBarState: SearchBarState,
    searchResults: List<AppListItem>?,
    onSearch: suspend (String) -> Unit,
    onItemSelected: (AppListItem) -> Unit,
    onSearchCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopSearchBar(
        state = searchBarState,
        windowInsets = WindowInsets(),
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
        } else if (searchResults.isEmpty()) {
            Text(
                text = stringResource(R.string.search_no_results),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults, key = { it.packageName }) { item ->
                    AppListRow(item, false, modifier.clickable {
                        onItemSelected(item)
                    })
                }
            }
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
            AppsSearch(state, emptyList(), {}, {}, {})
        }
    }
}
