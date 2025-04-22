package org.fdroid.basic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import org.fdroid.basic.ui.main.NUM_ITEMS
import org.fdroid.basic.ui.main.Sort
import org.fdroid.basic.ui.main.apps.AppNavigationItem
import org.fdroid.basic.ui.main.apps.FilterModel
import org.fdroid.basic.ui.main.apps.FilterPresenter

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    val categories = listOf(
        app.getString(R.string.category_Time),
        app.getString(R.string.category_Games),
        app.getString(R.string.category_Money),
        app.getString(R.string.category_Reading),
        app.getString(R.string.category_Theming),
        app.getString(R.string.category_Connectivity),
        app.getString(R.string.category_Internet),
        app.getString(R.string.category_Navigation),
        app.getString(R.string.category_Multimedia),
        app.getString(R.string.category_Phone_SMS),
        app.getString(R.string.category_Science_Education),
        app.getString(R.string.category_Security),
        app.getString(R.string.category_Sports_Health),
        app.getString(R.string.category_System),
        app.getString(R.string.category_Writing),
    )

    val initialApps = buildList {
        repeat(NUM_ITEMS) { i ->
            val category = categories.getOrElse(i) { categories.random() }
            val navItem = AppNavigationItem(
                packageName = "$i",
                name = "App $i",
                summary = "Summary of the app • $category",
                isNew = i > NUM_ITEMS - 4,
            )
            add(navItem)
        }
    }

    private val _onlyInstalledApps = MutableStateFlow(false)
    val onlyInstalledApps = _onlyInstalledApps.asStateFlow<Boolean>()
    private val _sortBy = MutableStateFlow<Sort>(Sort.NAME)
    val sortBy = _sortBy.asStateFlow<Sort>()
    private val _addedCategories = MutableStateFlow<List<String>>(emptyList())
    val addedCategories = _addedCategories.asStateFlow<List<String>>()

    val filterModel: StateFlow<FilterModel> = scope.launchMolecule(mode = ContextClock) {
        FilterPresenter(
            appsFlow = flow { emit(initialApps) },
            onlyInstalledAppsFlow = onlyInstalledApps,
            sortByFlow = sortBy,
            allCategories = categories,
            addedCategoriesFlow = addedCategories,
        )
    }

    fun sortBy(sort: Sort) {
        _sortBy.update { sort }
    }

    fun addCategory(category: String) {
        _addedCategories.update {
            addedCategories.value.toMutableList().apply {
                add(category)
            }
        }
    }

    fun removeCategory(category: String) {
        _addedCategories.update {
            addedCategories.value.toMutableList().apply {
                remove(category)
            }
        }
    }

    fun showOnlyInstalledApps(onlyInstalled: Boolean) {
        _onlyInstalledApps.update { onlyInstalled }
    }

}
