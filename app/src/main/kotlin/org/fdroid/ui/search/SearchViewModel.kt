package org.fdroid.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class SearchViewModel
@Inject
constructor(app: Application, private val searchManager: SearchManager) : AndroidViewModel(app) {

  val textFieldState = searchManager.textFieldState
  val searchResults = searchManager.searchResults

  suspend fun search(term: String) = searchManager.search(term)

  fun onSearchCleared() = searchManager.onSearchCleared()
}
