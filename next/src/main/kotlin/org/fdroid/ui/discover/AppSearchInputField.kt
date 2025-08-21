package org.fdroid.ui.discover

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.fdroid.next.R

@Composable
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
fun AppSearchInputField(
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    onSearch: suspend (String) -> Unit,
    onSearchCleared: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // set-up search as you type
    LaunchedEffect(Unit) {
        textFieldState.edit { placeCursorAtEnd() }
        snapshotFlow { textFieldState.text }
            .distinctUntilChanged()
            .debounce(500)
            .collectLatest {
                if (it.isEmpty()) {
                    onSearchCleared()
                } else if (it.length >= SEARCH_THRESHOLD) {
                    onSearch(textFieldState.text.toString())
                }
            }
    }
    SearchBarDefaults.InputField(
        modifier = Modifier,
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = {
            scope.launch { onSearch(it) }
        },
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                IconButton(
                    onClick = { scope.launch { searchBarState.animateToCollapsed() } }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.menu_search),
                )
            }
        },
        trailingIcon = {
            if (textFieldState.text.isNotEmpty()) {
                IconButton(onClick = onSearchCleared) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                    )
                }
            }
        }
    )
}
