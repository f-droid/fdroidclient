package org.fdroid.fdroid.views.repos

import android.app.Application
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import info.guardianproject.netcipher.NetCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fdroid.database.Repository
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.UpdateService

data class RepoDetailsState(
    val repo: Repository?,
    val archiveEnabled: Boolean? = null,
)

class RepoDetailsViewModel(app: Application) : AndroidViewModel(app) {

    private val repoManager = FDroidApp.getRepoManager(app)
    private val _state = MutableStateFlow<RepoDetailsState?>(null)
    val state = _state.asStateFlow()
    val liveData = _state.asLiveData()

    fun initRepo(repoId: Long) {
        val repo = repoManager.getRepository(repoId)
        if (repo == null) {
            _state.value = RepoDetailsState(null)
        } else {
            _state.value = RepoDetailsState(
                repo = repo,
                archiveEnabled = repo.isArchiveEnabled(),
            )
        }
    }

    fun setArchiveRepoEnabled(repo: Repository, enabled: Boolean) {
        // archiveEnabled = null means we don't know current state, it's in progress
        _state.value = _state.value?.copy(archiveEnabled = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repoManager.setArchiveRepoEnabled(repo, enabled, NetCipher.getProxy())
                _state.value = _state.value?.copy(archiveEnabled = enabled)
                if (enabled) withContext(Dispatchers.Main) {
                    val address = repo.address.replace(Regex("repo/?$"), "archive")
                    UpdateService.updateRepoNow(getApplication(), address)
                }
            } catch (e: Exception) {
                Log.e(this.javaClass.simpleName, "Error toggling archive repo: ", e)
                _state.value = _state.value?.copy(archiveEnabled = repo.isArchiveEnabled())
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), R.string.repo_archive_failed, LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun Repository.isArchiveEnabled(): Boolean {
        return repoManager.getRepositories().find { r ->
            r.isArchiveRepo && r.certificate == certificate
        }?.enabled ?: false
    }

}
