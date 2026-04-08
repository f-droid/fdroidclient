package org.fdroid.ui.screenshots

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.core.os.LocaleListCompat
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.search.GlobalSearch
import org.fdroid.ui.search.SearchResults
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SearchScreenshotTest(localeName: String) : LocalizedScreenshotTest(localeName) {

  companion object {
    @JvmStatic @Parameterized.Parameters(name = "{0}") fun locales() = locales
  }

  @Test
  fun search() =
    screenshotTest("2_Search", currentNavKey = NavigationKey.Search, numUpdates = 0) { localeList ->
      val categories = setOf("Network Analyzer", "Social Network", "Workout")
      val results =
        SearchResults(
          categories = getCategoryItems(localeList).filter { it.id in categories },
          apps = getSearchResultItems(localeList),
        )
      GlobalSearch(
        textFieldState = rememberTextFieldState("work"),
        searchResults = results,
        savedSearches = emptyList(),
        onSearch = {},
        onClearSavedSearches = {},
        onNav = {},
        onBack = {},
        onSearchCleared = {},
      )
    }
}

private fun getSearchResultItems(localeList: LocaleListCompat) =
  searchResultApps.map { (packageName, metadata) ->
    AppListItem(
      repoId = 1L,
      packageName = packageName,
      name = metadata.name?.getBestLocale(localeList) ?: "Unknown",
      summary = metadata.summary?.getBestLocale(localeList) ?: "",
      lastUpdated = metadata.lastUpdated,
      isInstalled = packageName.startsWith("de"),
      isCompatible = true,
      iconModel = "https://f-droid.org/repo${metadata.icon.getBestLocale(localeList)?.name}",
    )
  }
