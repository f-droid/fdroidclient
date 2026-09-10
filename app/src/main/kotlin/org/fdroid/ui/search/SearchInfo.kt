package org.fdroid.ui.search

import org.fdroid.search.SavedSearch
import org.fdroid.ui.categories.CategoryItem

interface SearchInfo {
  val searchResults: SearchResults?
  val savedSearches: List<SavedSearch>?
  val categories: List<CategoryItem>?
  val autoShowKeyboard: Boolean
  val showKeyboard: Boolean
  val actions: SearchActions

  fun onKeyboardShown()
}

interface SearchActions {
  suspend fun onSearch(term: String)

  fun onSearchCleared()

  fun onClearSearchHistory()

  fun setAutoShowKeyboard(autoShow: Boolean)
}
