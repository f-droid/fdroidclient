package org.fdroid.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import org.fdroid.search.SearchManager

@HiltViewModel
class SearchViewModel
@Inject
constructor(app: Application, private val searchManager: SearchManager) : AndroidViewModel(app) {

  val searchResults = searchManager.searchResults
  val savedSearchesFlow = searchManager.savedSearches

  suspend fun search(term: String) = searchManager.search(term)

  fun onSearchCleared() = searchManager.onSearchCleared()

  fun onClearSearchHistory() = viewModelScope.launch { searchManager.onClearSearchHistory() }
}
