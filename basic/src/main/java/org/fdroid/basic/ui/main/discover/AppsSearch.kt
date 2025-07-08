package org.fdroid.basic.ui.main.discover

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.fdroid.basic.ui.categories.Category
import org.fdroid.basic.ui.categories.CategoryList
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppsSearch(
    searchBarState: SearchBarState,
    categories: List<Category>?,
    onItemClick: (AppNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldState = rememberTextFieldState()
    val inputField = @Composable {
        AppSearchInputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
        )
    }
    TopSearchBar(
        state = searchBarState,
        windowInsets = WindowInsets(),
        inputField = inputField,
        modifier = modifier,
    )
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        CategoryList(categories)
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppsSearchPreview() {
    FDroidContent {
        val state = rememberSearchBarState(SearchBarValue.Expanded)
        AppsSearch(state, emptyList(), {})
    }
}
