package org.fdroid.ui.repositories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RepositoriesPresenter(
    repositoriesFlow: Flow<List<RepositoryItem>>,
    repoSortingMapFlow: StateFlow<Map<Long, Int>>,
): RepositoryModel {
    val repositories = repositoriesFlow.collectAsState(null).value
    val repoSortingMap = repoSortingMapFlow.collectAsState().value
    return RepositoryModel(
        repositories = repositories?.sortedBy { repo ->
            // newly added repos will not be in repoSortingMap, so they need fallback
            repoSortingMap[repo.repoId] ?: repositories.find { it.repoId == repo.repoId }?.weight
        },
    )
}
