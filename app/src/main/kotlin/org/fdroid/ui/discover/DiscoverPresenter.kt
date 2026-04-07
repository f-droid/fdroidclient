package org.fdroid.ui.discover

import android.content.pm.PackageInfo
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.os.LocaleListCompat
import io.ktor.client.engine.ProxyConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.database.AppOverviewItem
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.download.NetworkState
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.repo.RepoUpdateState
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.categories.CategoryItem

@Composable
fun DiscoverPresenter(
  newAppsFlow: Flow<List<AppOverviewItem>>,
  recentlyUpdatedAppsFlow: Flow<List<AppOverviewItem>>,
  mostDownloadedAppsFlow: Flow<List<AppOverviewItem>>,
  categoriesFlow: Flow<List<CategoryItem>>,
  installedAppsFlow: StateFlow<Map<String, PackageInfo>>,
  searchTextFieldState: TextFieldState,
  isFirstStart: Boolean,
  networkState: NetworkState,
  repoUpdateStateFlow: StateFlow<RepoUpdateState?>,
  hasRepoIssuesFlow: Flow<Boolean>,
  repoManager: RepoManager,
  settingsManager: SettingsManager,
): DiscoverModel {
  val localeList = LocaleListCompat.getDefault()
  val installedApps = installedAppsFlow.collectAsState().value

  fun AppOverviewItem.toAppDiscoverItem(
    repository: Repository,
    proxyConfig: ProxyConfig?,
  ): AppDiscoverItem {
    val isInstalled = installedApps.contains(packageName)
    val imageModel = getIcon(localeList)?.getImageModel(repository, proxyConfig) as? DownloadRequest
    return AppDiscoverItem(
      packageName = packageName,
      name = getName(localeList) ?: "Unknown App",
      lastUpdated = lastUpdated,
      isInstalled = isInstalled,
      imageModel =
        if (isInstalled) {
          PackageName(packageName, imageModel)
        } else {
          imageModel
        },
    )
  }

  val proxyConfig = settingsManager.proxyConfig
  // load carousel content in reverse order because that looked best with AnimatedVisibility
  val mostDownloadedApps =
    mostDownloadedAppsFlow.collectAsState(null).value?.mapNotNull {
      val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
      it.toAppDiscoverItem(repository, proxyConfig)
    }
  val recentlyUpdatedApps =
    recentlyUpdatedAppsFlow.collectAsState(null).value?.mapNotNull {
      val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
      it.toAppDiscoverItem(repository, proxyConfig)
    }
  val newApps =
    newAppsFlow.collectAsState(null).value?.mapNotNull {
      val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
      it.toAppDiscoverItem(repository, proxyConfig)
    }
  val categories = categoriesFlow.collectAsState(null).value
  val repositoriesFlow = repoManager.repositoriesState

  return if (
    !mostDownloadedApps.isNullOrEmpty() ||
      !newApps.isNullOrEmpty() ||
      !categories.isNullOrEmpty() ||
      !recentlyUpdatedApps.isNullOrEmpty()
  ) {
    // As soon as we loaded a list,
    // we start showing it on screen and update when other lists load.
    // This is to speed up the time to first content on initial screen.
    LoadedDiscoverModel(
      newApps = newApps ?: emptyList(),
      recentlyUpdatedApps = recentlyUpdatedApps ?: emptyList(),
      mostDownloadedApps = mostDownloadedApps,
      categories = categories?.groupBy { it.group },
      searchTextFieldState = searchTextFieldState,
      hasRepoIssues = hasRepoIssuesFlow.collectAsState(false).value,
    )
  } else {
    // everything is still null or empty, so figure out why
    val repositories = repositoriesFlow.collectAsState().value
    if (repositories?.all { !it.enabled } == true) {
      NoEnabledReposDiscoverModel
    } else if (isFirstStart || recentlyUpdatedApps?.size == 0) {
      // There should always be recently updated apps,
      // because those don't have a freshness constraint.
      // In case the DB got cleared (e.g. though panic action or failed migration),
      // the isFirstStart condition would be false,
      // but we still want to go down first start path to update repos again.
      FirstStartDiscoverModel(networkState, repoUpdateStateFlow.collectAsState().value)
    } else {
      LoadingDiscoverModel
    }
  }
}
