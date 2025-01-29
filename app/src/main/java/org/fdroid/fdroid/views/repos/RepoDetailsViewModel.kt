package org.fdroid.fdroid.views.repos

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.guardianproject.netcipher.NetCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fdroid.database.Repository
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.data.DBHelper
import org.fdroid.fdroid.generateQrBitmapKt
import org.fdroid.fdroid.work.RepoUpdateWorker

data class RepoDetailsState(
    val repo: Repository,
    val archiveState: ArchiveState,
)

enum class ArchiveState {
    ENABLED,
    DISABLED,
    UNKNOWN,
}

class RepoDetailsViewModel(
    app: Application,
    initialRepo: Repository,
) : AndroidViewModel(app) {

    companion object {
        // TODO: Use androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
        // That seems to require setting up dependency injection.
        val APP_KEY = object : CreationExtras.Key<Application> {}
        val REPO_KEY = object : CreationExtras.Key<Repository> {}
        val Factory = viewModelFactory {
            initializer {
                val app = this[APP_KEY] as Application
                val repo = this[REPO_KEY] as Repository
                RepoDetailsViewModel(app, repo)
            }
        }
    }

    private val repoManager = FDroidApp.getRepoManager(app)
    private val repositoryDao = DBHelper.getDb(app).getRepositoryDao()
    private val appDao = DBHelper.getDb(app).getAppDao()

    private val _state = MutableStateFlow(
        RepoDetailsState(initialRepo, initialRepo.archiveState())
    )
    val state = _state.asStateFlow()
    val liveData = _state.asLiveData()

    val repoFlow = combine(_state, repoManager.repositoriesState) { s, reposState ->
        reposState.find { repo -> repo.repoId == s.repo.repoId }
    }.distinctUntilChanged()
    val repoLiveData = repoFlow.asLiveData()

    val numberAppsFlow: Flow<Int> = repoFlow.map { repo ->
        if (repo != null) {
            appDao.getNumberOfAppsInRepository(repo.repoId)
        } else 0
    }.flowOn(Dispatchers.IO).distinctUntilChanged()
    val numberOfAppsLiveData = numberAppsFlow.asLiveData()

    val qrCodeLiveData = MutableLiveData<Bitmap?>(null)

    fun setArchiveRepoEnabled(enabled: Boolean) {
        val repo = _state.value.repo
        _state.value = _state.value.copy(archiveState = ArchiveState.UNKNOWN)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val repoId = repoManager.setArchiveRepoEnabled(repo, enabled, NetCipher.getProxy())
                _state.value = _state.value.copy(archiveState = enabled.toArchiveState())
                if (enabled && repoId != null) withContext(Dispatchers.Main) {
                    RepoUpdateWorker.updateNow(getApplication(), repoId)
                }
            } catch (e: Exception) {
                Log.e(this.javaClass.simpleName, "Error toggling archive repo: ", e)
                _state.value = _state.value.copy(archiveState = repo.archiveState())
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), R.string.repo_archive_failed, LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun deleteRepository() {
        val repoId = _state.value.repo.repoId
        viewModelScope.launch(Dispatchers.IO) {
            repositoryDao.deleteRepository(repoId)
        }
    }

    fun updateUsernameAndPassword(username: String, password: String) {
        val repoId = _state.value.repo.repoId
        viewModelScope.launch(Dispatchers.IO) {
            repositoryDao.updateUsernameAndPassword(repoId, username, password)
        }
    }

    fun updateDisabledMirrors(toDisable: List<String>) {
        val repoId = _state.value.repo.repoId
        viewModelScope.launch(Dispatchers.IO) {
            repositoryDao.updateDisabledMirrors(repoId, toDisable)
        }
    }

    private fun Repository.archiveState(): ArchiveState {
        val isEnabled = repoManager.getRepositories().find { r ->
            r.isArchiveRepo && r.certificate == certificate
        }?.enabled
        return when (isEnabled) {
            true -> ArchiveState.ENABLED
            false -> ArchiveState.DISABLED
            null -> ArchiveState.UNKNOWN
        }
    }

    private fun Boolean.toArchiveState(): ArchiveState {
        return if (this) ArchiveState.ENABLED else ArchiveState.DISABLED
    }

    // TODO: initialise this once on ViewModel creation, and don't take an Activity, do fixed size
    fun generateQrCode(activity: AppCompatActivity) {
        val repo = _state.value.repo
        if (repo.address.startsWith("content://") || repo.address.startsWith("file://")) {
            // no need to show a QR Code, it is not shareable
            qrCodeLiveData.value = null
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = generateQrBitmapKt(activity, repo.shareUri)
            withContext(Dispatchers.Main) {
                qrCodeLiveData.value = bitmap
            }
        }
    }
}
