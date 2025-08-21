package org.fdroid.ui.lists

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import org.fdroid.next.R
import org.fdroid.ui.discover.AppSearchInputField
import org.fdroid.ui.discover.SEARCH_THRESHOLD

/**
 * This is a top app bar that isn't mean to ever expand with results, but for in-list filtering.
 * There may still be potential to factor out common code with [AppSearchInputField].
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
fun TopSearchBar(
    onSearch: (String) -> Unit,
    onSearchCleared: () -> Unit,
    onHideSearch: () -> Unit,
) {
    val searchFieldState = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }
    AppBarWithSearch(
        state = rememberSearchBarState(),
        inputField = {
            SearchBarDefaults.InputField(
                state = searchFieldState,
                leadingIcon = {
                    IconButton(onClick = onHideSearch) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                trailingIcon = {
                    if (searchFieldState.text.isNotEmpty()) {
                        IconButton(onClick = {
                            searchFieldState.setTextAndPlaceCursorAtEnd("")
                            onSearchCleared()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                },
                onSearch = onSearch,
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        snapshotFlow { searchFieldState.text }
            .debounce(500)
            .collectLatest {
                if (it.length >= SEARCH_THRESHOLD || it.isEmpty()) {
                    onSearch(searchFieldState.text.toString())
                }
            }
    }
}
