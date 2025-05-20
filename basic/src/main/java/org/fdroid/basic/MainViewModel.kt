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
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    private val savedStateHandle: SavedStateHandle,
    val myAppsManager: MyAppsManager,
    private val appDetailsManager: AppDetailsManager,
    val repositoryManager: RepositoryManager,
) : AndroidViewModel(app) {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    val categories = listOf(
        Pair(app.getString(R.string.category_Internet), R.drawable.category_internet),
        Pair(app.getString(R.string.category_Games), R.drawable.category_games),
        Pair(app.getString(R.string.category_Navigation), R.drawable.category_navigation),
        Pair(app.getString(R.string.category_Multimedia), R.drawable.category_money),
        Pair(app.getString(R.string.category_Security), R.drawable.category_security),
        Pair(app.getString(R.string.category_Reading), R.drawable.category_reading),
        Pair(app.getString(R.string.category_Time), R.drawable.category_theming),
        Pair(app.getString(R.string.category_Money), R.drawable.category_money),
        Pair(app.getString(R.string.category_Theming), R.drawable.category_theming),
        Pair(app.getString(R.string.category_Connectivity), R.drawable.category_connectivity),
        Pair(app.getString(R.string.category_Phone_SMS), R.drawable.category_system),
        Pair(
            app.getString(R.string.category_Science_Education),
            R.drawable.category_science_education
        ),
        Pair(app.getString(R.string.category_Sports_Health), R.drawable.category_security),
        Pair(app.getString(R.string.category_System), R.drawable.category_system),
        Pair(app.getString(R.string.category_Writing), R.drawable.category_writing),
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

    private val _onlyInstalledApps = MutableStateFlow(false)
    val onlyInstalledApps = _onlyInstalledApps.asStateFlow<Boolean>()
    private val _sortBy = MutableStateFlow<Sort>(Sort.LATEST)
    val sortBy = _sortBy.asStateFlow<Sort>()
    private val _addedCategories = MutableStateFlow<List<String>>(emptyList())
    val addedCategories = _addedCategories.asStateFlow<List<String>>()

    val filterModel: StateFlow<FilterModel> = scope.launchMolecule(mode = ContextClock) {
        FilterPresenter(
            appsFlow = flow { emit(initialApps) },
            onlyInstalledAppsFlow = onlyInstalledApps,
            sortByFlow = sortBy,
            allCategories = categories.map { it.first },
            addedCategoriesFlow = addedCategories,
        )
    }
    val updates = myAppsManager.updates
    val installed = myAppsManager.installed
    val numUpdates = myAppsManager.numUpdates
    val appDetails = appDetailsManager.appDetails


    fun setAppDetails(app: MinimalApp) {
        val newApp = filterModel.value.apps.find { it.packageName == app.packageName }
            ?: (updates.value.find { it.packageName == app.packageName }
                ?: installed.value.find { it.packageName == app.packageName })?.let {
                AppNavigationItem(it.packageName, it.name ?: "Unknown app", "Summary", false)
            }
        appDetailsManager.setAppDetails(newApp)
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
