package org.fdroid.basic.repo

import android.content.Context
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.fdroid.basic.ui.main.repositories.Repository
import org.fdroid.index.RepoManager
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.Added
import org.fdroid.repo.FetchResult
import org.fdroid.repo.Fetching
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // TODO maybe more like a ViewModel name clash with RepoManager
class RepositoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repoManager: RepoManager,
) {

    val repos: Flow<List<Repository>> = repoManager.repositoriesState.map { repos ->
        repos.map {
            Repository(
                address = it.address,
                timestamp = it.timestamp,
                lastUpdated = it.lastUpdated,
                weight = it.weight,
                enabled = it.enabled,
                name = it.getName(LocaleListCompat.getDefault()) ?: "Unknown Repo",
            )
        }
    }

    private val _visibleRepository = MutableStateFlow<Repository?>(null)
    val visibleRepository = _visibleRepository.asStateFlow()

    fun setVisibleRepository(repository: Repository?) {
        _visibleRepository.value = repository
    }

    fun getRepository(repoId: Long) = repoManager.getRepository(repoId)

    fun addRepo() {
        // just temp code to get repo into DB
        repoManager.fetchRepositoryPreview("https://f-droid.org/repo")
        GlobalScope.launch(Dispatchers.IO) {
            var hasAdded = false
            repoManager.addRepoState.collect {
                if (it is Fetching) {
                    if (!hasAdded && it.fetchResult is FetchResult.IsNewRepository) {
                        hasAdded = true
                        repoManager.addFetchedRepository()
                    }
                } else if (it is Added ) {
                    delay(1000) // wait for repo available in DB
                    RepoUpdateWorker.updateNow(context,it.repo.repoId)
                    cancel()
                } else if (it is AddRepoError) {
                    cancel()
                }
            }
        }
    }

}
