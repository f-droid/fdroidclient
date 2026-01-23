package org.fdroid.ui.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.FlowPreview
import org.fdroid.R

/**
 * This is a top app bar that isn't mean to ever expand with results, but for in-list filtering.
 * There may still be potential to factor out common code with [AppSearchInputField].
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
fun TopSearchBar(
    searchFieldState: TextFieldState = rememberTextFieldState(),
    onSearch: suspend (String) -> Unit,
    onSearchCleared: () -> Unit,
    onHideSearch: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onHideSearch) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        title = {
            AppSearchInputField(
                searchBarState = rememberSearchBarState(),
                textFieldState = searchFieldState,
                onSearch = onSearch,
                onSearchCleared = {
                    searchFieldState.setTextAndPlaceCursorAtEnd("")
                    onSearchCleared()
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
