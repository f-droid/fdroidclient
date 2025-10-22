package org.fdroid.ui.repositories

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.R
import org.fdroid.ui.utils.asRelativeTimeString

@Composable
fun RepositoriesPresenter(
    context: Context,
    repositoriesFlow: StateFlow<List<RepositoryItem>?>,
    repoSortingMapFlow: StateFlow<Map<Long, Int>>,
    showOnboardingFlow: StateFlow<Boolean>,
): RepositoryModel {
    val repositories = repositoriesFlow.collectAsState().value
    val repoSortingMap = repoSortingMapFlow.collectAsState().value
    val lastUpdated = repositories?.maxBy { it.lastUpdated ?: 0 }?.lastUpdated
    return RepositoryModel(
        repositories = repositories?.sortedBy { repo ->
            repoSortingMap[repo.repoId] ?: repoSortingMap.size
        },
        showOnboarding = showOnboardingFlow.collectAsState().value,
        lastCheckForUpdate = if (lastUpdated == null || lastUpdated <= 0) {
            context.getString(R.string.repositories_last_update_never)
        } else {
            lastUpdated.asRelativeTimeString()
        }
    )
}
