package org.fdroid.ui.search

import android.content.res.Configuration
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.search.SavedSearch
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.utils.appListItems
import org.fdroid.ui.utils.categoryItems

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchEmptyTest() =
  ScreenshotTest {
    val textFieldState = rememberTextFieldState()
    GlobalSearch(
      textFieldState = textFieldState,
      searchResults = null,
      savedSearches = emptyList(),
      onSearch = {},
      onClearSavedSearches = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchEmptyStateTest() =
  ScreenshotTest {
    val savedSearches =
      listOf(SavedSearch(1, "Browser"), SavedSearch(2, "media player"), SavedSearch(3, "email app"))
    val textFieldState = rememberTextFieldState()
    GlobalSearch(
      textFieldState = textFieldState,
      searchResults = null,
      savedSearches = savedSearches,
      onSearch = {},
      onClearSavedSearches = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchLoadingTest() =
  ScreenshotTest {
    val textFieldState = rememberTextFieldState("foo bar")
    GlobalSearch(
      textFieldState = textFieldState,
      searchResults = null,
      savedSearches = emptyList(),
      onSearch = {},
      onClearSavedSearches = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchNoResultsTest() =
  ScreenshotTest {
    val textFieldState = rememberTextFieldState("foo")
    GlobalSearch(
      textFieldState = textFieldState,
      searchResults = SearchResults(emptyList(), emptyList()),
      savedSearches = emptyList(),
      onSearch = {},
      onClearSavedSearches = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchResultsTest() =
  ScreenshotTest {
    val textFieldState = rememberTextFieldState("foo bar")
    GlobalSearch(
      textFieldState = textFieldState,
      searchResults = SearchResults(appListItems, categoryItems.subList(0, 4)),
      savedSearches = emptyList(),
      onSearch = {},
      onClearSavedSearches = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchOnlyCategoriesTest() =
  ScreenshotTest {
    val textFieldState = rememberTextFieldState("foo bar")
    GlobalSearch(
      textFieldState = textFieldState,
      searchResults = SearchResults(emptyList(), categoryItems.subList(5, 9)),
      savedSearches = emptyList(),
      onSearch = {},
      onClearSavedSearches = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
fun GlobalSearchResultsNightTest() =
  ScreenshotTest {
    val textFieldState = rememberTextFieldState("foo bar")
    GlobalSearch(
      textFieldState = textFieldState,
      searchResults = SearchResults(appListItems, categoryItems.subList(0, 4)),
      savedSearches = emptyList(),
      onSearch = {},
      onClearSavedSearches = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }
