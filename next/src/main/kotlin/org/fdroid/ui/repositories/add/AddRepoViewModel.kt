package org.fdroid.ui.repositories.add

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import mu.KotlinLogging
import org.fdroid.index.RepoManager
import org.fdroid.repo.AddRepoState
import javax.inject.Inject

@HiltViewModel
class AddRepoViewModel @Inject constructor(
    app: Application,
    private val repoManager: RepoManager,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger { }
    val state: StateFlow<AddRepoState> = repoManager.addRepoState

    override fun onCleared() {
        log.info { "onCleared() abort adding repository" }
        repoManager.abortAddingRepository()
    }

    fun onFetchRepo(uriStr: String) {
        val uri = uriStr.trim().toUri()
        if (repoManager.isSwapUri(uri)) {
            // TODO full only
        } else {
            repoManager.abortAddingRepository()
            // TODO support proxy
            repoManager.fetchRepositoryPreview(uri.toString(), proxy = null)
        }
    }

    fun addFetchedRepository() {
        log.info { "addFetchedRepository()" }
        repoManager.addFetchedRepository()
    }
}
