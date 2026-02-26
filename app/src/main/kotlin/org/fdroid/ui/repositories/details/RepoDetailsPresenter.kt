package org.fdroid.ui.repositories.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.ktor.client.engine.ProxyConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.Repository
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateState

@Composable
fun RepoDetailsPresenter(
  repoFlow: Flow<Repository?>,
  numAppsFlow: Flow<Int?>,
  archiveStateFlow: StateFlow<ArchiveState>,
  showOnboardingFlow: StateFlow<Boolean>,
  updateFlow: Flow<RepoUpdateState?>,
  networkStateFlow: StateFlow<NetworkState>,
  proxyConfig: ProxyConfig?,
): RepoDetailsModel {
  val repo = repoFlow.collectAsState(null).value
  return RepoDetailsModel(
    repo = repo,
    numberApps = numAppsFlow.collectAsState(null).value,
    officialMirrors =
      repo
        ?.allOfficialMirrors
        ?.map { mirror ->
          val disabledMirrors = repo.disabledMirrors
          OfficialMirrorItem(
            mirror = mirror,
            isEnabled = !disabledMirrors.contains(mirror.baseUrl),
            isRepoAddress = repo.address == mirror.baseUrl,
          )
        }
        ?.sorted() ?: emptyList(),
    userMirrors =
      repo?.allUserMirrors?.map { mirror ->
        UserMirrorItem(mirror, !repo.disabledMirrors.contains(mirror.baseUrl))
      } ?: emptyList(),
    archiveState = archiveStateFlow.collectAsState().value,
    showOnboarding = showOnboardingFlow.collectAsState().value,
    updateState = updateFlow.collectAsState(null).value,
    networkState = networkStateFlow.collectAsState().value,
    proxy = proxyConfig,
  )
}
