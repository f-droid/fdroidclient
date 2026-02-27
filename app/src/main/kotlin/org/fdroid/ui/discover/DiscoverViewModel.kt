package org.fdroid.ui.discover

import android.annotation.SuppressLint
import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.NetworkMonitor
import org.fdroid.index.RepoManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.repo.RepoUpdateManager
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.search.SearchManager

@HiltViewModel
class DiscoverViewModel
@Inject
constructor(
  private val app: Application,
  savedStateHandle: SavedStateHandle,
  private val db: FDroidDatabase,
  networkMonitor: NetworkMonitor,
  private val settingsManager: SettingsManager,
  private val searchManager: SearchManager,
  private val repoManager: RepoManager,
  private val repoUpdateManager: RepoUpdateManager,
  private val installedAppsCache: InstalledAppsCache,
) : AndroidViewModel(app) {

  private val log = KotlinLogging.logger {}
  private val moleculeScope =
    CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
  private val collator = Collator.getInstance(Locale.getDefault())

  private val mostDownloadedApps =
    flow {
        val packageNames =
          try {
            app.assets.open("most_downloaded_apps.json").use { inputStream ->
              @OptIn(ExperimentalSerializationApi::class)
              Json.decodeFromStream<List<String>>(inputStream)
            }
          } catch (e: Exception) {
            log.error(e) { "Error loading most downloaded apps: " }
            return@flow
          }
        db.getAppDao().getAppsFlow(packageNames).collect { apps -> emit(apps) }
      }
      .flowOn(Dispatchers.IO)
  private val categories =
    db.getRepositoryDao().getLiveCategories().asFlow().map { categories ->
      categories
        .map { category ->
          CategoryItem(id = category.id, name = category.getName(localeList) ?: "Unknown Category")
        }
        .sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
    }
  private val hasRepoIssues =
    repoManager.repositoriesState.map { repos -> repos.any { it.enabled && it.errorCount >= 5 } }

  val localeList = LocaleListCompat.getDefault()
  val discoverModel: StateFlow<DiscoverModel> by
    lazy(LazyThreadSafetyMode.NONE) {
      @SuppressLint("StateFlowValueCalledInComposition") // see comment below
      moleculeScope.launchMolecule(mode = ContextClock) {
        DiscoverPresenter(
          newAppsFlow = db.getAppDao().getNewAppsFlow(),
          recentlyUpdatedAppsFlow = db.getAppDao().getRecentlyUpdatedAppsFlow(),
          mostDownloadedAppsFlow = mostDownloadedApps,
          categoriesFlow = categories,
          installedAppsFlow = installedAppsCache.installedApps,
          searchTextFieldState = searchManager.textFieldState,
          isFirstStart = settingsManager.isFirstStart,
          // not observing the flow, but just taking the current value,
          // because we kick off repo updates from the UI depending on this state
          networkState = networkMonitor.networkState.value,
          repoUpdateStateFlow = repoUpdateManager.repoUpdateState,
          hasRepoIssuesFlow = hasRepoIssues,
          repoManager = repoManager,
          settingsManager = settingsManager,
        )
      }
    }
}
