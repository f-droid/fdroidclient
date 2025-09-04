package org.fdroid.ui.lists

import android.app.Application
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.getDownloadRequest
import org.fdroid.index.RepoManager
import org.fdroid.next.R
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.repositories.RepositoryItem
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    private val db: FDroidDatabase,
    private val repoManager: RepoManager,
    private val settingsManager: SettingsManager,
) : AndroidViewModel(app), AppListActions {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

    private val localeList = LocaleListCompat.getDefault()
    private val apps = MutableStateFlow<List<AppListItem>?>(null)
    private val categories = db.getRepositoryDao().getLiveCategories().asFlow().map { categories ->
        val collator = Collator.getInstance(Locale.getDefault())
        categories.map { category ->
            CategoryItem(
                id = category.id,
                name = category.getName(localeList) ?: "Unknown Category",
            )
        }.sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
    }
    private val repositories = repoManager.repositoriesState.map { repositories ->
        repositories.mapNotNull {
            if (it.enabled) RepositoryItem(it, localeList)
            else null
        }.sortedBy { it.weight }
    }
    private val query = MutableStateFlow("")

    private val _currentList =
        MutableStateFlow<AppListType>(AppListType.New(app.getString(R.string.app_list_new)))
    val currentList = _currentList.asStateFlow()
    private val _showFilters = savedStateHandle.getMutableStateFlow("showFilters", false)
    val showFilters = _showFilters.asStateFlow()

    private val sortBy = MutableStateFlow(settingsManager.appListSortOrder.value)
    private val filterIncompatible = MutableStateFlow(settingsManager.filterIncompatible.value)
    private val filteredCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val filteredRepositoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val showOnboarding = settingsManager.showFilterOnboarding

    val appListModel: StateFlow<AppListModel> = scope.launchMolecule(mode = ContextClock) {
        AppListPresenter(
            appsFlow = apps,
            sortByFlow = sortBy,
            filterIncompatibleFlow = filterIncompatible,
            categoriesFlow = categories,
            filteredCategoryIdsFlow = filteredCategoryIds,
            repositoriesFlow = repositories,
            filteredRepositoryIdsFlow = filteredRepositoryIds,
            searchQueryFlow = query,
        )
    }

    @UiThread
    fun load(type: AppListType) {
        apps.value = null
        _currentList.value = type

        viewModelScope.launch(Dispatchers.IO) {
            apps.value = loadApps(type)
        }
    }

    @WorkerThread
    private suspend fun loadApps(type: AppListType): List<AppListItem> {
        val appDao = db.getAppDao()
        return when (type) {
            is AppListType.Author -> appDao.getAppsByAuthor(type.authorName)
            is AppListType.Category -> appDao.getAppsByCategory(type.categoryId)
            is AppListType.New -> appDao.getNewApps()
            is AppListType.RecentlyUpdated -> appDao.getRecentlyUpdatedApps()
            is AppListType.All -> appDao.getAllApps()
            is AppListType.Repository -> appDao.getAppsByRepository(type.repoId)
        }.mapNotNull {
            val repository = repoManager.getRepository(it.repoId)
                ?: return@mapNotNull null
            AppListItem(
                repoId = it.repoId,
                packageName = it.packageName,
                name = it.getName(localeList) ?: "Unknown App",
                summary = it.getSummary(localeList) ?: "Unknown",
                lastUpdated = it.lastUpdated,
                isCompatible = it.isCompatible,
                iconDownloadRequest = it.getIcon(localeList)?.getDownloadRequest(repository),
                categoryIds = it.categories?.toSet(),
            )
        }
    }

    override fun toggleFilterVisibility() {
        _showFilters.update { !it }
    }

    override fun sortBy(sort: AppListSortOrder) {
        sortBy.update { sort }
    }

    override fun toggleFilterIncompatible() {
        filterIncompatible.update { !it }
    }

    override fun addCategory(categoryId: String) {
        filteredCategoryIds.update {
            filteredCategoryIds.value.toMutableSet().apply {
                add(categoryId)
            }
        }
    }

    override fun removeCategory(categoryId: String) {
        filteredCategoryIds.update {
            filteredCategoryIds.value.toMutableSet().apply {
                remove(categoryId)
            }
        }
    }

    override fun addRepository(repoId: Long) {
        filteredRepositoryIds.update {
            filteredRepositoryIds.value.toMutableSet().apply {
                add(repoId)
            }
        }
    }

    override fun removeRepository(repoId: Long) {
        filteredRepositoryIds.update {
            filteredRepositoryIds.value.toMutableSet().apply {
                remove(repoId)
            }
        }
    }

    override fun saveFilters() {
        settingsManager.saveAppListFilter(sortBy.value, filterIncompatible.value)
    }

    override fun clearFilters() {
        filterIncompatible.value = false
        filteredCategoryIds.value = emptySet()
        filteredRepositoryIds.value = emptySet()
    }

    override fun onSearch(query: String) {
        this.query.value = query
    }

    override fun onOnboardingSeen() = settingsManager.onFilterOnboardingSeen()
}
