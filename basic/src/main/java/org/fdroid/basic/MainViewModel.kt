package org.fdroid.basic

import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.fdroid.basic.download.getDownloadRequest
import org.fdroid.basic.manager.AppDetailsManager
import org.fdroid.basic.manager.MyAppsManager
import org.fdroid.basic.repo.RepositoryManager
import org.fdroid.basic.ui.categories.Category
import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.basic.ui.main.discover.AppNavigationItem
import org.fdroid.basic.ui.main.discover.FilterModel
import org.fdroid.basic.ui.main.discover.FilterPresenter
import org.fdroid.basic.ui.main.discover.Sort
import org.fdroid.basic.ui.main.lists.AppList
import org.fdroid.database.FDroidDatabase
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    val myAppsManager: MyAppsManager,
    private val db: FDroidDatabase,
    private val appDetailsManager: AppDetailsManager,
    val repositoryManager: RepositoryManager,
) : AndroidViewModel(app) {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

    val categories = db.getRepositoryDao().getLiveCategories().asFlow().map { categories ->
        val collator = Collator.getInstance(Locale.getDefault())
        categories.map { category ->
            Category(
                id = category.id,
                name = category.getName(localeList) ?: "Unknown Category",
            )
        }.sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
    }

    private val _currentList = MutableStateFlow<AppList>(AppList.New)
    val currentList = _currentList.asStateFlow()
    private val _showFilters = savedStateHandle.getMutableStateFlow("showFilters", true)
    val showFilters = _showFilters.asStateFlow()
    private val _sortBy = MutableStateFlow<Sort>(Sort.LATEST)
    val sortBy = _sortBy.asStateFlow<Sort>()
    private val _addedCategories = MutableStateFlow<List<String>>(emptyList())
    val addedCategories = _addedCategories.asStateFlow<List<String>>()

    val localeList = LocaleListCompat.getDefault()
    val filterModel: StateFlow<FilterModel> = scope.launchMolecule(mode = ContextClock) {
        FilterPresenter(
            areFiltersShownFlow = showFilters,
            appsFlow = db.getAppDao().getAppOverviewItems().asFlow().map { list ->
                list.mapNotNull {
                    val repository = repositoryManager.getRepository(it.repoId)
                        ?: return@mapNotNull null
                    AppNavigationItem(
                        packageName = it.packageName,
                        name = it.name ?: "Unknown",
                        summary = it.summary ?: "Unknown",
                        isNew = it.lastUpdated == it.added,
                        lastUpdated = it.lastUpdated,
                        iconDownloadRequest = it.getIcon(localeList)
                            ?.getDownloadRequest(repository),
                    )
                }
            },
            sortByFlow = sortBy,
            allCategoriesFlow = categories,
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
                AppNavigationItem(
                    packageName = it.packageName,
                    name = it.name ?: "Unknown app",
                    summary = "Summary",
                    isNew = false,
                )
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
