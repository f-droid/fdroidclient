package org.fdroid.ui.repositories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RepositoriesPresenter(
    repositoriesFlow: StateFlow<List<RepositoryItem>?>,
    repoSortingMapFlow: StateFlow<Map<Long, Int>>,
    showOnboardingFlow: StateFlow<Boolean>,
): RepositoryModel {
    val repositories = repositoriesFlow.collectAsState().value
    val repoSortingMap = repoSortingMapFlow.collectAsState().value
    return RepositoryModel(
        repositories = repositories?.sortedBy { repo ->
            repoSortingMap[repo.repoId] ?: repoSortingMap.size
        },
        showOnboarding = showOnboardingFlow.collectAsState().value,
    )
}
