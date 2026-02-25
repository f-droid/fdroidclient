package org.fdroid.ui.repositories.add

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.download.NetworkMonitor
import org.fdroid.index.RepoManager
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.AddRepoState
import org.fdroid.repo.Added
import org.fdroid.repo.Adding
import org.fdroid.repo.Fetching
import org.fdroid.repo.None
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher

@HiltViewModel
class AddRepoViewModel
@Inject
constructor(
  app: Application,
  networkMonitor: NetworkMonitor,
  settingsManager: SettingsManager,
  private val repoManager: RepoManager,
  private val updateManager: UpdatesManager,
  @param:IoDispatcher private val ioScope: CoroutineScope,
) : AndroidViewModel(app) {

  private val log = KotlinLogging.logger {}
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
    ioScope.launch {
      repoManager.addFetchedRepository()
      // wait for repo to get added, so we can load updates afterward
      repoManager.addRepoState.collect {
        when (it) {
          is Fetching,
          Adding,
          None -> {}
          is Added -> {
            updateManager.loadUpdates().join()
            cancel()
          }
          is AddRepoError -> cancel()
        }
      }
    }
  }
}
