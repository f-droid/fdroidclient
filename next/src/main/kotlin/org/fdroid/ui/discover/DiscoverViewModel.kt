package org.fdroid.ui.discover

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.fdroid.appsearch.AppSearchManager
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.getDownloadRequest
import org.fdroid.index.RepoManager
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    db: FDroidDatabase,
    updatesManager: UpdatesManager,
    private val repoManager: RepoManager,
    private val appSearchManager: AppSearchManager,
    @IoDispatcher private val ioScope: CoroutineScope,
) : AndroidViewModel(app) {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

    val numUpdates = updatesManager.numUpdates
    val apps = db.getAppDao().getAppOverviewItems().asFlow().map { list ->
        list.mapNotNull {
            val repository = repoManager.getRepository(it.repoId)
                ?: return@mapNotNull null
            AppDiscoverItem(
                packageName = it.packageName,
                name = it.name ?: "Unknown",
                isNew = it.lastUpdated == it.added,
                lastUpdated = it.lastUpdated,
                iconDownloadRequest = it.getIcon(localeList)
                    ?.getDownloadRequest(repository),
            )
        }
    }
    val categories = db.getRepositoryDao().getLiveCategories().asFlow().map { categories ->
        val collator = Collator.getInstance(Locale.getDefault())
        categories.map { category ->
            CategoryItem(
                id = category.id,
                name = category.getName(localeList) ?: "Unknown Category",
            )
        }.sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
    }
    private val searchResults = MutableStateFlow<List<AppListItem>?>(null)

    val localeList = LocaleListCompat.getDefault()
    val discoverModel: StateFlow<DiscoverModel> = scope.launchMolecule(mode = ContextClock) {
        DiscoverPresenter(
            appsFlow = apps,
            categoriesFlow = categories,
            repositoriesFlow = repoManager.repositoriesState,
            searchResultsFlow = searchResults,
        )
    }

    suspend fun search(term: String) = withContext(ioScope.coroutineContext) {
        searchResults.value = appSearchManager.search(term).mapNotNull {
            val repository = repoManager.getRepository(it.repoId)
                ?: return@mapNotNull null
            AppListItem(
                packageName = it.packageName,
                name = it.name ?: "Unknown",
                summary = it.summary ?: "Unknown",
                lastUpdated = it.lastUpdated,
                iconDownloadRequest = it.icon?.getDownloadRequest(repository),
            )
        }
    }

    fun onSearchCleared() {
        searchResults.value = null
    }
}
