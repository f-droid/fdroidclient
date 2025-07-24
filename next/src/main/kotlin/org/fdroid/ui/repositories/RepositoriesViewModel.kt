package org.fdroid.ui.repositories

import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.fdroid.download.getDownloadRequest
import org.fdroid.index.RepoManager
import javax.inject.Inject

@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    app: Application,
    repoManager: RepoManager,
) : AndroidViewModel(app) {

    private val localeList = LocaleListCompat.getDefault()
    val repos: Flow<List<RepositoryItem>> = repoManager.repositoriesState.map { repos ->
        repos.map {
            RepositoryItem(
                repoId = it.repoId,
                address = it.address,
                name = it.getName(localeList) ?: "Unknown Repo",
                icon = it.getIcon(localeList)?.getDownloadRequest(it),
                timestamp = it.timestamp,
                lastUpdated = it.lastUpdated,
                weight = it.weight,
                enabled = it.enabled,
            )
        }
    }

    private val _visibleRepositoryItem = MutableStateFlow<RepositoryItem?>(null)
    val visibleRepository = _visibleRepositoryItem.asStateFlow()

    fun setVisibleRepository(repositoryItem: RepositoryItem?) {
        _visibleRepositoryItem.value = repositoryItem
    }

}
