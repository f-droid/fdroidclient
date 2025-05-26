package org.fdroid.basic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import org.fdroid.basic.manager.AppDetailsManager
import org.fdroid.basic.manager.MyAppsManager
import org.fdroid.basic.manager.RepositoryManager
import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.basic.ui.main.discover.AppNavigationItem
import org.fdroid.basic.ui.main.discover.FilterModel
import org.fdroid.basic.ui.main.discover.FilterPresenter
import org.fdroid.basic.ui.main.discover.NUM_ITEMS
import org.fdroid.basic.ui.main.discover.Names
import org.fdroid.basic.ui.main.discover.Sort
import org.fdroid.basic.ui.main.lists.AppList
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    val myAppsManager: MyAppsManager,
    private val appDetailsManager: AppDetailsManager,
    val repositoryManager: RepositoryManager,
) : AndroidViewModel(app) {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    val categories = listOf(
        Pair(app.getString(R.string.category_Internet), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Games), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Navigation), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Multimedia), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Security), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Reading), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Time), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Money), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Theming), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Connectivity), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Phone_SMS), R.drawable.ic_launcher),
        Pair(
            app.getString(R.string.category_Science_Education),
            R.drawable.ic_launcher
        ),
        Pair(app.getString(R.string.category_Sports_Health), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_System), R.drawable.ic_launcher),
        Pair(app.getString(R.string.category_Writing), R.drawable.ic_launcher),
    )

    val initialApps = buildList {
        repeat(NUM_ITEMS) { i ->
            val category = categories.getOrElse(i) { categories.random() }.first
            val navItem = AppNavigationItem(
                packageName = "$i",
                name = Names.randomName,
                summary = "Summary of the app • $category",
                isNew = i > NUM_ITEMS - 4,
            )
            add(navItem)
        }
    }

    private val _currentList = MutableStateFlow<AppList>(AppList.New)
    val currentList = _currentList.asStateFlow()
    private val _showFilters = savedStateHandle.getMutableStateFlow("showFilters", true)
    val showFilters = _showFilters.asStateFlow()
    private val _sortBy = MutableStateFlow<Sort>(Sort.LATEST)
    val sortBy = _sortBy.asStateFlow<Sort>()
    private val _addedCategories = MutableStateFlow<List<String>>(emptyList())
    val addedCategories = _addedCategories.asStateFlow<List<String>>()

    val filterModel: StateFlow<FilterModel> = scope.launchMolecule(mode = ContextClock) {
        FilterPresenter(
            areFiltersShownFlow = showFilters,
            appsFlow = flow { emit(initialApps) },
            sortByFlow = sortBy,
            allCategories = categories.map { it.first },
            addedCategoriesFlow = addedCategories,
        )
    }
    val updates = myAppsManager.updates
    val installed = myAppsManager.installed
    val numUpdates = myAppsManager.numUpdates
    val appDetails = appDetailsManager.appDetails

    fun setAppList(appList: AppList) {
        _currentList.value = appList
    }

    fun setAppDetails(app: MinimalApp) {
        val newApp = filterModel.value.apps.find { it.packageName == app.packageName }
            ?: (updates.value.find { it.packageName == app.packageName }
                ?: installed.value.find { it.packageName == app.packageName })?.let {
                AppNavigationItem(it.packageName, it.name ?: "Unknown app", "Summary", false)
            }
        appDetailsManager.setAppDetails(newApp)
    }

    fun toggleListFilterVisibility() {
        _showFilters.update { !it }
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
}
