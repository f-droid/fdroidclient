package org.fdroid.ui.repositories

import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.fdroid.database.Repository
import org.fdroid.index.RepoManager
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.settings.OnboardingManager
import org.fdroid.settings.SettingsManager
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject

@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    app: Application,
    private val repoManager: RepoManager,
    private val settingsManager: SettingsManager,
    private val onboardingManager: OnboardingManager,
    @param:IoDispatcher private val ioScope: CoroutineScope,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger { }
    private val localeList = LocaleListCompat.getDefault()
    private val moleculeScope =
        CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    private val repos = MutableStateFlow<List<RepositoryItem>?>(null)
    private val repoSortingMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val showOnboarding = onboardingManager.showRepositoriesOnboarding

    init {
        viewModelScope.launch {
            repoManager.repositoriesState.collect {
                onRepositoriesChanged(it)
            }
        }
    }

    // define below init, because this only defines repoSortingMap
    val model: StateFlow<RepositoryModel> = moleculeScope.launchMolecule(mode = ContextClock) {
        RepositoriesPresenter(
            context = application,
            repositoriesFlow = repos,
            repoSortingMapFlow = repoSortingMap,
            showOnboardingFlow = showOnboarding,
            lastUpdateFlow = settingsManager.lastRepoUpdateFlow,
        )
    }

    private fun onRepositoriesChanged(repositories: List<Repository>) {
        log.info("onRepositoriesChanged(${repositories.size})")
        repos.update {
            repositories.mapNotNull {
                if (it.isArchiveRepo) null
                else RepositoryItem(it, localeList)
            }
        }
        repoSortingMap.update {
            // just add repos to sortingMap, because they are already pre-sorted by weight
            mutableMapOf<Long, Int>().apply {
                repositories.forEachIndexed { index, repository ->
                    this[repository.repoId] = index
                }
            }
        }
    }

    fun onRepositoryEnabled(repoId: Long, enabled: Boolean) {
        ioScope.launch {
            repoManager.setRepositoryEnabled(repoId, enabled)
            if (enabled) withContext(Dispatchers.Main) {
                RepoUpdateWorker.updateNow(application, repoId)
            }
        }
    }

    fun onRepositoriesMoved(fromRepoId: Long, toRepoId: Long) {
        log.info { "onRepositoriesMoved($fromRepoId, $toRepoId)" }
        repoSortingMap.update {
            repoSortingMap.value.toMutableMap().apply {
                val toIndex = get(toRepoId) ?: error("No position for toRepoId $toRepoId")
                replace(toRepoId, replace(fromRepoId, toIndex)!!)
            }
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

    fun onOnboardingSeen() = onboardingManager.onRepositoriesOnboardingSeen()
}
