package org.fdroid.ui.apps

import android.app.Application
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.app.ShareCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.DownloadRequest
import org.fdroid.download.NetworkMonitor
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstallConfirmationState
import org.fdroid.install.InstallState
import org.fdroid.install.InstalledAppsCache
import org.fdroid.settings.OnboardingManager
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.utils.startActivitySafe
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher

@HiltViewModel
class MyAppsViewModel
@Inject
constructor(
  app: Application,
  @param:IoDispatcher private val scope: CoroutineScope,
  savedStateHandle: SavedStateHandle,
  private val db: FDroidDatabase,
  private val settingsManager: SettingsManager,
  installedAppsCache: InstalledAppsCache,
  private val onboardingManager: OnboardingManager,
  private val appInstallManager: AppInstallManager,
  private val networkMonitor: NetworkMonitor,
  private val updatesManager: UpdatesManager,
  private val repoManager: RepoManager,
) : AndroidViewModel(app), MyAppsActions {

  private val log = KotlinLogging.logger {}
  private val localeList = LocaleListCompat.getDefault()
  private val moleculeScope =
    CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

  private val updates = updatesManager.updates

  @OptIn(ExperimentalCoroutinesApi::class)
  private val installedAppItems =
    installedAppsCache.installedApps.flatMapLatest { installedApps ->
      val proxyConfig = settingsManager.proxyConfig
      db.getAppDao().getInstalledAppListItems(installedApps).map { list ->
        list.map { app ->
          val backupModel =
            repoManager.getRepository(app.repoId)?.let { repo ->
              app.getIcon(localeList)?.getImageModel(repo, proxyConfig)
            } as? DownloadRequest
          InstalledAppItem(
            packageName = app.packageName,
            name = app.name ?: "Unknown app",
            installedVersionName = app.installedVersionName ?: "???",
            installedVersionCode = app.installedVersionCode ?: 0,
            lastUpdated = app.lastUpdated,
            iconModel = PackageName(app.packageName, backupModel),
          )
        }
      }
    }

  private val searchQuery = savedStateHandle.getMutableStateFlow("query", "")
  private val sortOrder = MutableStateFlow(settingsManager.myAppsSortOrder)
  val myAppsModel: StateFlow<MyAppsModel> by
    lazy(LazyThreadSafetyMode.NONE) {
      moleculeScope.launchMolecule(mode = ContextClock) {
        MyAppsPresenter(
          appUpdatesFlow = updates,
          appInstallStatesFlow = appInstallManager.appInstallStates,
          appsWithIssuesFlow = updatesManager.appsWithIssues,
          installedAppsFlow = installedAppItems,
          showAppIssueHintFlow = onboardingManager.showAppIssueHint,
          searchQueryFlow = searchQuery,
          sortOrderFlow = sortOrder,
          networkStateFlow = networkMonitor.networkState,
        )
      }
    }

  override fun updateAll() {
    scope.launch { updatesManager.updateAll(true) }
  }

  override fun search(query: String) {
    searchQuery.value = query
  }

  override fun changeSortOrder(sort: AppListSortOrder) {
    sortOrder.value = sort
    settingsManager.myAppsSortOrder = sort
  }

  override fun confirmAppInstall(packageName: String, state: InstallConfirmationState) {
    log.info { "Asking user to confirm install of $packageName..." }
    scope.launch(Dispatchers.Main) {
      when (state) {
        is InstallState.PreApprovalConfirmationNeeded -> {
          appInstallManager.requestPreApprovalConfirmation(packageName, state)
        }
        is InstallState.UserConfirmationNeeded -> {
          appInstallManager.requestUserConfirmation(packageName, state)
        }
      }
    }
  }

  override fun ignoreAppIssue(item: AppWithIssueItem) {
    settingsManager.ignoreAppIssue(item.packageName, item.installedVersionCode)
    updatesManager.loadUpdates()
  }

  override fun onAppIssueHintSeen() = onboardingManager.onAppIssueHintSeen()

  override fun exportInstalledApps() {
    scope.launch {
      val stringBuilder =
        StringBuilder().apply {
          append("packageName,versionCode,versionName\n")
          for (app in installedAppItems.first()) {
            append(app.packageName).append(',')
            append(app.installedVersionCode).append(',')
            append(app.installedVersionName).append('\n')
          }
        }
      val title = application.resources.getString(R.string.send_installed_apps)
      val intentBuilder =
        ShareCompat.IntentBuilder(application)
          .setSubject(title)
          .setChooserTitle(title)
          .setText(stringBuilder.toString())
          .setType("text/csv")
      val chooserIntent =
        Intent.createChooser(intentBuilder.getIntent(), title).apply {
          addFlags(FLAG_ACTIVITY_NEW_TASK)
        }
      withContext(Dispatchers.Main) { application.startActivitySafe(chooserIntent) }
    }
  }
}
