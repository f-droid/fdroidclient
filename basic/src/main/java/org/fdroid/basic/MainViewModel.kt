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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.fdroid.basic.download.getDownloadRequest
import org.fdroid.basic.repo.RepositoryManager
import org.fdroid.basic.ui.categories.Category
import org.fdroid.basic.ui.main.discover.AppDiscoverItem
import org.fdroid.basic.ui.main.discover.DiscoverModel
import org.fdroid.basic.ui.main.discover.DiscoverPresenter
import org.fdroid.database.FDroidDatabase
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    db: FDroidDatabase,
    val repositoryManager: RepositoryManager,
) : AndroidViewModel(app) {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

    val apps = db.getAppDao().getAppOverviewItems().asFlow().map { list ->
        list.mapNotNull {
            val repository = repositoryManager.getRepository(it.repoId)
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
            Category(
                id = category.id,
                name = category.getName(localeList) ?: "Unknown Category",
            )
        }.sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
    }

    val localeList = LocaleListCompat.getDefault()
    val discoverModel: StateFlow<DiscoverModel> = scope.launchMolecule(mode = ContextClock) {
        DiscoverPresenter(
            appsFlow = apps,
            categoriesFlow = categories,
            repositoriesFlow = repositoryManager.repos,
        )
    }

}
