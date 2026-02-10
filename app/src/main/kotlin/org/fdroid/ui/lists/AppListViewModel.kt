package org.fdroid.ui.lists

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.DownloadRequest
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.settings.OnboardingManager
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.repositories.RepositoryItem
import java.text.Collator
import java.util.Locale

@HiltViewModel(assistedFactory = AppListViewModel.Factory::class)
class AppListViewModel @AssistedInject constructor(
    private val app: Application,
    @Assisted val type: AppListType,
    savedStateHandle: SavedStateHandle,
    private val db: FDroidDatabase,
    private val repoManager: RepoManager,
    private val settingsManager: SettingsManager,
    private val onboardingManager: OnboardingManager,
    private val installedAppsCache: InstalledAppsCache,
) : AndroidViewModel(app), AppListActions {

    private val moleculeScope =
        CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

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
        val proxyConfig = settingsManager.proxyConfig
        repositories.mapNotNull {
            if (it.enabled) RepositoryItem(it, localeList, proxyConfig)
            else null
        }.sortedBy { it.weight }
    }
    private val query = MutableStateFlow("")

    private val _showFilters = savedStateHandle.getMutableStateFlow("showFilters", false)
    val showFilters = _showFilters.asStateFlow()

    private val sortBy = MutableStateFlow(settingsManager.appListSortOrder)
    private val filterIncompatible = MutableStateFlow(settingsManager.filterIncompatible)
    private val filteredCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val filteredRepositoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val showOnboarding = onboardingManager.showFilterOnboarding

    val appListModel: StateFlow<AppListModel> by lazy(LazyThreadSafetyMode.NONE) {
        moleculeScope.launchMolecule(mode = ContextClock) {
            AppListPresenter(
                type = type,
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
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            apps.value = loadApps(type)
        }
    }

    @WorkerThread
    private suspend fun loadApps(type: AppListType): List<AppListItem> {
        val appDao = db.getAppDao()
        val proxyConfig = settingsManager.proxyConfig
        return when (type) {
            is AppListType.Author -> appDao.getAppsByAuthor(type.authorName)
            is AppListType.Category -> appDao.getAppsByCategory(type.categoryId)
            is AppListType.New -> appDao.getNewApps()
            is AppListType.RecentlyUpdated -> appDao.getRecentlyUpdatedApps()
            is AppListType.MostDownloaded -> {
                val packageNames = app.assets.open("most_downloaded_apps.json").use { inputStream ->
                    @OptIn(ExperimentalSerializationApi::class)
                    Json.decodeFromStream<List<String>>(inputStream)
                }
                appDao.getApps(packageNames)
            }
            is AppListType.All -> appDao.getAllApps()
            is AppListType.Repository -> appDao.getAppsByRepository(type.repoId)
        }.mapNotNull {
            val repository = repoManager.getRepository(it.repoId)
                ?: return@mapNotNull null
            val iconModel = it.getIcon(localeList)?.getImageModel(repository, proxyConfig)
                as? DownloadRequest
            val isInstalled = installedAppsCache.isInstalled(it.packageName)
            AppListItem(
                repoId = it.repoId,
                packageName = it.packageName,
                name = it.getName(localeList) ?: "Unknown App",
                summary = it.getSummary(localeList) ?: "Unknown",
                lastUpdated = it.lastUpdated,
                isInstalled = isInstalled,
                isCompatible = it.isCompatible,
                iconModel = if (isInstalled) {
                    PackageName(it.packageName, iconModel)
                } else {
                    iconModel
                },
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

    override fun onOnboardingSeen() = onboardingManager.onFilterOnboardingSeen()

    @AssistedFactory
    interface Factory {
        fun create(type: AppListType): AppListViewModel
    }
}
