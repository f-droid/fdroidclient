package org.fdroid.fdroid.views.repos

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.guardianproject.netcipher.NetCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fdroid.database.Repository
import org.fdroid.download.Mirror
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.data.DBHelper
import org.fdroid.fdroid.generateQrBitmapKt
import org.fdroid.fdroid.work.RepoUpdateWorker

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
        private const val TAG = "RepoDetailsViewModel"

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

    private val repoId = initialRepo.repoId

    private val repoManager = FDroidApp.getRepoManager(app)
    private val repositoryDao = DBHelper.getDb(app).getRepositoryDao()
    private val appDao = DBHelper.getDb(app).getAppDao()

    val repoFlow: StateFlow<Repository?> = repoManager.repositoriesState.map { reposState ->
        reposState.find { repo -> repo.repoId == repoId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    val numberAppsFlow: Flow<Int> = repoFlow.map { repo ->
        if (repo != null) {
            appDao.getNumberOfAppsInRepository(repo.repoId)
        } else 0
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    val archiveStateFlow = MutableStateFlow(initialRepo.archiveState())

    fun setArchiveRepoEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = repoFlow.value ?: return@launch
            archiveStateFlow.emit(ArchiveState.UNKNOWN)
            try {
                val repoId = repoManager.setArchiveRepoEnabled(repo, enabled, NetCipher.getProxy())
                archiveStateFlow.emit(enabled.toArchiveState())
                if (enabled && repoId != null) withContext(Dispatchers.Main) {
                    RepoUpdateWorker.updateNow(getApplication(), repoId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling archive repo: ", e)
                archiveStateFlow.emit(repo.archiveState())
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), R.string.repo_archive_failed, LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun deleteRepository() {
        viewModelScope.launch(Dispatchers.IO) {
            repoManager.deleteRepository(repoId)
        }
    }

    fun updateUsernameAndPassword(username: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repositoryDao.updateUsernameAndPassword(repoId, username, password)
        }
    }

    fun setMirrorEnabled(mirror: Mirror, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repoManager.setMirrorEnabled(repoId, mirror, enabled)
        }
    }

    fun deleteUserMirror(mirror: Mirror) {
        viewModelScope.launch(Dispatchers.IO) {
            repoManager.deleteUserMirror(repoId, mirror)
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

    suspend fun generateQrCode(activity: AppCompatActivity): Bitmap? {
        val repo = repoFlow.value ?: return null
        if (repo.address.startsWith("content://") || repo.address.startsWith("file://")) {
            // no need to show a QR Code, it is not shareable
            return null
        }
        return generateQrBitmapKt(activity, repo.shareUri)
    }
}
