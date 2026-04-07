package org.fdroid.ui.search

import android.content.res.Configuration
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.utils.appListItems
import org.fdroid.ui.utils.categoryItems

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchEmptyTest() =
  ScreenshotTest(showBottomBar = false) {
    val textFieldState = rememberTextFieldState()
    GlobalSearch(
      textFieldState,
      null,
      onSearch = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchLoadingTest() =
  ScreenshotTest(showBottomBar = false) {
    val textFieldState = rememberTextFieldState("foo bar")
    GlobalSearch(
      textFieldState,
      null,
      onSearch = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchNoResultsTest() =
  ScreenshotTest(showBottomBar = false) {
    val textFieldState = rememberTextFieldState("foo")
    GlobalSearch(
      textFieldState,
      SearchResults(emptyList(), emptyList()),
      onSearch = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchResultsTest() =
  ScreenshotTest(showBottomBar = false) {
    val textFieldState = rememberTextFieldState("foo bar")
    GlobalSearch(
      textFieldState,
      SearchResults(appListItems, categoryItems.subList(0, 4)),
      onSearch = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun GlobalSearchOnlyCategoriesTest() =
  ScreenshotTest(showBottomBar = false) {
    val textFieldState = rememberTextFieldState("foo bar")
    GlobalSearch(
      textFieldState,
      SearchResults(emptyList(), categoryItems.subList(5, 9)),
      onSearch = {},
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
  ScreenshotTest(showBottomBar = false) {
    val textFieldState = rememberTextFieldState("foo bar")
    GlobalSearch(
      textFieldState,
      SearchResults(appListItems, categoryItems.subList(0, 4)),
      onSearch = {},
      onNav = {},
      onBack = {},
      onSearchCleared = {},
    )
  }
