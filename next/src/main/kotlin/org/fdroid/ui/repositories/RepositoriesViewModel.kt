package org.fdroid.ui.repositories

import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.fdroid.index.RepoManager
import org.fdroid.settings.SettingsManager
import javax.inject.Inject

@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    app: Application,
    private val repoManager: RepoManager,
    private val settingsManager: SettingsManager,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger { }
    private val localeList = LocaleListCompat.getDefault()
    private val moleculeScope =
        CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    private val repos: Flow<List<RepositoryItem>> = repoManager.repositoriesState.map { repos ->
        repos.mapNotNull {
            if (it.isArchiveRepo) null
            else RepositoryItem(it, localeList)
        }
    }
    private val repoSortingMap: MutableStateFlow<Map<Long, Int>>
    private val showOnboarding = settingsManager.showRepositoriesOnboarding

    init {
        // just add repos to sortingMap, because they are already pre-sorted by weight
        val sortingMap = mutableMapOf<Long, Int>()
        repoManager.getRepositories().forEachIndexed { index, repository ->
            sortingMap[repository.repoId] = index
        }
        repoSortingMap = MutableStateFlow(sortingMap)
    }

    // define below init, because this only defines repoSortingMap
    val model: StateFlow<RepositoryModel> = moleculeScope.launchMolecule(mode = ContextClock) {
        RepositoriesPresenter(
            repositoriesFlow = repos,
            repoSortingMapFlow = repoSortingMap,
            showOnboardingFlow = showOnboarding,
        )
    }

    fun onRepositoriesMoved(fromIndex: Int, toIndex: Int) {
        log.info { "onRepositoriesMoved($fromIndex, $toIndex)" }
        val repoItems = model.value.repositories ?: error("Model had null repositories")
        val fromItem = repoItems[fromIndex]
        val toItem = repoItems[toIndex]
        repoSortingMap.value = repoSortingMap.value.toMutableMap().apply {
            replace(fromItem.repoId, toIndex)
            replace(toItem.repoId, fromIndex)
        }
    }

    fun onRepositoriesFinishedMoving(fromRepoId: Long, toRepoId: Long) {
        log.info { "onRepositoriesFinishedMoving($fromRepoId, $toRepoId)" }
        val fromRepo = repoManager.getRepository(fromRepoId)
            ?: error("No repo for repoId $fromRepoId")
        val toRepo = repoManager.getRepository(toRepoId)
            ?: error("No repo for repoId $toRepoId")
        log.info { "  ${fromRepo.address} => ${toRepo.address}" }
        repoManager.reorderRepositories(fromRepo, toRepo)
    }

    fun addRepo() {
    }

    fun onOnboardingSeen() = settingsManager.onRepositoriesOnboardingSeen()
}
