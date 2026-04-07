package org.fdroid.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.appListItems
import org.fdroid.ui.utils.categoryItems

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GlobalSearch(
  textFieldState: TextFieldState,
  searchResults: SearchResults?,
  onSearch: suspend (String) -> Unit,
  onNav: (NavigationKey) -> Unit,
  onBack: () -> Unit,
  onSearchCleared: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopSearchBar(
        searchFieldState = textFieldState,
        onSearch = onSearch,
        onSearchCleared = onSearchCleared,
        onHideSearch = onBack,
      )
    },
  ) { paddingValues ->
    HorizontalDivider(
      color = SearchBarDefaults.colors().dividerColor,
      modifier = Modifier.padding(
        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
        top = paddingValues.calculateTopPadding(),
      ),
    )
    SearchResults(
      searchResults = searchResults,
      textFieldState = textFieldState,
      onNav = onNav,
      paddingValues = paddingValues,
      modifier = Modifier,
    )
  }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchLoadingPreview() {
  FDroidContent {
    val textFieldState = rememberTextFieldState("foo bar")
    Box(Modifier.fillMaxSize()) { GlobalSearch(textFieldState, null, {}, {}, {}, {}) }
  }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppsSearchEmptyPreview() {
  FDroidContent {
    val textFieldState = rememberTextFieldState("foo")
    Box(Modifier.fillMaxSize()) {
      GlobalSearch(textFieldState, SearchResults(emptyList(), emptyList()), {}, {}, {}, {})
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
      GlobalSearch(
        textFieldState = textFieldState,
        searchResults = SearchResults(emptyList(), categoryItems.subList(3, 5)),
        onSearch = {},
        onNav = {},
        onBack = {},
        onSearchCleared = {},
      )
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
      GlobalSearch(
        textFieldState = textFieldState,
        searchResults = SearchResults(appListItems, categoryItems.subList(0, 4)),
        onSearch = {},
        onNav = {},
        onBack = {},
        onSearchCleared = {},
      )
    }
  }
}
