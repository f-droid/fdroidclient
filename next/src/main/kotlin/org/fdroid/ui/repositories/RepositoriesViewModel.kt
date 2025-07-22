package org.fdroid.ui.repositories

import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.fdroid.index.RepoManager
import javax.inject.Inject

@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    app: Application,
    private val repoManager: RepoManager,
) : AndroidViewModel(app) {

    val repos: Flow<List<Repository>> = repoManager.repositoriesState.map { repos ->
        repos.map {
            Repository(
                repoId = it.repoId,
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

}
