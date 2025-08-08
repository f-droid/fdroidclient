package org.fdroid.ui.lists

import android.app.Application
import androidx.annotation.UiThread
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.application
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import mu.KotlinLogging
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.AppOverviewItem
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.getDownloadRequest
import org.fdroid.index.RepoManager
import org.fdroid.next.R
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.repositories.RepositoryItem
import java.text.Collator
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    private val db: FDroidDatabase,
    private val repoManager: RepoManager,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger { }
    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

    private val localeList = LocaleListCompat.getDefault()
    private val apps = MutableStateFlow<List<AppListItem>?>(null)
    private var appsLiveData: LiveData<*>? = null
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

    private val currentList =
        MutableStateFlow<AppListType>(AppListType.New(app.getString(R.string.app_list_new)))
    private val showFilters = savedStateHandle.getMutableStateFlow("showFilters", false)
    private val sortBy = MutableStateFlow(AppListSortOrder.LAST_UPDATED)
    private val filteredCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val filteredRepositoryIds = MutableStateFlow<Set<Long>>(emptySet())

    val appListModel: StateFlow<AppListModel> = scope.launchMolecule(mode = ContextClock) {
        AppListPresenter(
            listFlow = currentList,
            appsFlow = apps,
            areFiltersShownFlow = showFilters,
            sortByFlow = sortBy,
            categoriesFlow = categories,
            filteredCategoryIdsFlow = filteredCategoryIds,
            repositoriesFlow = repositories,
            filteredRepositoryIdsFlow = filteredRepositoryIds,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val appsObserver = Observer<Any> { items ->
        val list = items as? List<*> ?: return@Observer
        if (list.isEmpty()) {
            apps.value = emptyList()
        } else {
            when (items[0]) {
                is AppOverviewItem -> onAppOverviewItems(items as List<AppOverviewItem>)
                is org.fdroid.database.AppListItem -> {
                    onAppListItems(items as List<org.fdroid.database.AppListItem>)
                }
                else -> error("Unknown item class: ${items[0]?.javaClass?.name}")
            }
        }
    }

    init {
        log.info("init $this")
    }

    override fun onCleared() {
        log.info("onCleared $this")
        appsLiveData?.removeObserver(appsObserver)
    }

    @UiThread
    fun load(type: AppListType) {
        apps.value = null
        appsLiveData?.removeObserver(appsObserver)
        appsLiveData = null

        currentList.value = type
        when (type) { // TODO we may want to clean up the mess below and introduce new DB methods
            is AppListType.Author -> {
                appsLiveData = db.getAppDao().getAppListItemsForAuthor(
                    packageManager = application.packageManager,
                    author = type.authorName,
                    searchQuery = null,
                    sortOrder = sortBy.value,
                ).apply {
                    observeForever(appsObserver)
                }
            }
            is AppListType.Category -> {
                appsLiveData = db.getAppDao().getAppListItems(
                    packageManager = application.packageManager,
                    category = type.categoryId,
                    searchQuery = null,
                    sortOrder = sortBy.value,
                ).apply {
                    observeForever(appsObserver)
                }
            }
            is AppListType.New -> {
                val cutOffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14)
                appsLiveData = db.getAppDao().getAppOverviewItems().map { list ->
                    list.filter { it.added > cutOffMillis && it.lastUpdated == it.added }
                }.apply {
                    observeForever(appsObserver)
                }
            }
            is AppListType.RecentlyUpdated -> {
                appsLiveData = db.getAppDao().getAppOverviewItems().apply {
                    observeForever(appsObserver)
                }
            }
            is AppListType.All -> {
                appsLiveData = db.getAppDao().getAppListItems(
                    packageManager = application.packageManager,
                    searchQuery = null,
                    sortOrder = sortBy.value,
                ).apply {
                    observeForever(appsObserver)
                }
            }
            is AppListType.Repository -> {
                appsLiveData = db.getAppDao().getAppListItems(
                    packageManager = application.packageManager,
                    repoId = type.repoId,
                    searchQuery = null,
                    sortOrder = sortBy.value,
                ).apply {
                    observeForever(appsObserver)
                }
            }
        }
    }

    fun toggleListFilterVisibility() {
        showFilters.update { !it }
    }

    fun sortBy(sort: AppListSortOrder) {
        sortBy.update { sort }
    }

    fun addCategory(category: String) {
        filteredCategoryIds.update {
            filteredCategoryIds.value.toMutableSet().apply {
                add(category)
            }
        }
    }

    fun removeCategory(category: String) {
        filteredCategoryIds.update {
            filteredCategoryIds.value.toMutableSet().apply {
                remove(category)
            }
        }
    }

    fun addRepository(repoId: Long) {
        filteredRepositoryIds.update {
            filteredRepositoryIds.value.toMutableSet().apply {
                add(repoId)
            }
        }
    }

    fun removeRepository(repoId: Long) {
        filteredRepositoryIds.update {
            filteredRepositoryIds.value.toMutableSet().apply {
                remove(repoId)
            }
        }
    }

    fun onAppOverviewItems(items: List<AppOverviewItem>) {
        apps.value = items.mapNotNull {
            val repository = repoManager.getRepository(it.repoId)
                ?: return@mapNotNull null
            AppListItem(
                repoId = it.repoId,
                packageName = it.packageName,
                name = it.name ?: "Unknown",
                summary = it.summary ?: "Unknown",
                lastUpdated = it.lastUpdated,
                iconDownloadRequest = it.getIcon(localeList)?.getDownloadRequest(repository),
                categoryIds = it.categories?.toSet(),
            )
        }
    }

    private fun onAppListItems(items: List<org.fdroid.database.AppListItem>) {
        apps.value = items.mapNotNull {
            val repository = repoManager.getRepository(it.repoId)
                ?: return@mapNotNull null
            AppListItem(
                repoId = it.repoId,
                packageName = it.packageName,
                name = it.name ?: "Unknown",
                summary = it.summary ?: "Unknown",
                lastUpdated = it.lastUpdated,
                iconDownloadRequest = it.getIcon(localeList)?.getDownloadRequest(repository),
                categoryIds = it.categories?.toSet(),
            )
        }
    }
}
