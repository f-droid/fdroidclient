package org.fdroid.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.navigation.NavigationKey

/** The minimum amount of characters we start auto-searching for. */
const val SEARCH_THRESHOLD = 2

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppsSearch(
  textFieldState: TextFieldState,
  onNav: (NavigationKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val searchBarState = rememberSearchBarState()
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
        onSearch = {},
        modifier = Modifier.onFocusChanged { if (it.isFocused) onNav(NavigationKey.Search) },
      )
    },
    modifier = modifier.clickable { onNav(NavigationKey.Search) },
  )
}

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    val textFieldState = rememberTextFieldState()
    Box(Modifier.fillMaxSize()) { AppsSearch(textFieldState, {}) }
  }
}
