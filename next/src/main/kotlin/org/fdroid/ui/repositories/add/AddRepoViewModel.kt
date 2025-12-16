package org.fdroid.ui.repositories.add

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import mu.KotlinLogging
import org.fdroid.download.NetworkMonitor
import org.fdroid.index.RepoManager
import org.fdroid.repo.AddRepoState
import org.fdroid.settings.SettingsManager
import javax.inject.Inject

@HiltViewModel
class AddRepoViewModel @Inject constructor(
    app: Application,
    networkMonitor: NetworkMonitor,
    settingsManager: SettingsManager,
    private val repoManager: RepoManager,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger { }
    val state: StateFlow<AddRepoState> = repoManager.addRepoState

    val proxyConfig = settingsManager.proxyConfig
    val networkState = networkMonitor.networkState

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
            repoManager.fetchRepositoryPreview(uri.toString(), proxyConfig)
        }
    }

    fun addFetchedRepository() {
        log.info { "addFetchedRepository()" }
        repoManager.addFetchedRepository()
    }
}
