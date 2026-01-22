package org.fdroid.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.navigation.NavigationKey

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExpandedSearch(
    textFieldState: TextFieldState,
    searchResults: SearchResults?,
    onSearch: suspend (String) -> Unit,
    onNav: (NavigationKey) -> Unit,
    onBack: () -> Unit,
    onSearchCleared: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (searchBarState.currentValue == SearchBarValue.Expanded) {
                            keyboardController?.hide()
                            scope.launch { searchBarState.animateToCollapsed() }
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = {
                    AppSearchInputField(
                        searchBarState = searchBarState,
                        textFieldState = textFieldState,
                        onSearch = onSearch,
                        onSearchCleared = {
                            textFieldState.setTextAndPlaceCursorAtEnd("")
                            onSearchCleared()
                        },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
            )
        }
    ) { paddingValues ->
        HorizontalDivider(
            color = SearchBarDefaults.colors().dividerColor,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
        )
        SearchResults(
            searchResults = searchResults,
            textFieldState = textFieldState,
            onNav = onNav,
            paddingValues = paddingValues,
            modifier = Modifier
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchLoadingPreview() {
    FDroidContent {
        val textFieldState = rememberTextFieldState("foo bar")
        Box(Modifier.fillMaxSize()) {
            ExpandedSearch(textFieldState, null, {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchEmptyPreview() {
    FDroidContent {
        val textFieldState = rememberTextFieldState("foo")
        Box(Modifier.fillMaxSize()) {
            ExpandedSearch(textFieldState, SearchResults(emptyList(), emptyList()), {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchOnlyCategoriesPreview() {
    FDroidContent {
        val textFieldState = rememberTextFieldState()
        Box(Modifier.fillMaxSize()) {
            val categories = listOf(
                CategoryItem("Bookmark", "Bookmark"),
                CategoryItem("Browser", "Browser"),
                CategoryItem("Calculator", "Calc"),
                CategoryItem("Money", "Money"),
            )
            ExpandedSearch(textFieldState, SearchResults(emptyList(), categories), {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchPreview() {
    FDroidContent {
        val textFieldState = rememberTextFieldState()
        Box(Modifier.fillMaxSize()) {
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
            ExpandedSearch(textFieldState, SearchResults(apps, categories), {}, {}, {}, {})
        }
    }
}
