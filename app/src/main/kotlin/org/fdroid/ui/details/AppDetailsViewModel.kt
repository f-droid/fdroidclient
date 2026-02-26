package org.fdroid.ui.details

import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import androidx.activity.result.ActivityResult
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import coil3.SingletonImageLoader
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.fdroid.UpdateChecker
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.DownloadRequest
import org.fdroid.download.NetworkMonitor
import org.fdroid.getCacheKey
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.RepoManager
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstallState
import org.fdroid.repo.RepoPreLoader
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher

@HiltViewModel(assistedFactory = AppDetailsViewModel.Factory::class)
class AppDetailsViewModel
@AssistedInject
constructor(
  private val app: Application,
  @Assisted private val packageName: String,
  @param:IoDispatcher private val scope: CoroutineScope,
  private val db: FDroidDatabase,
  private val repoManager: RepoManager,
  private val repoPreLoader: RepoPreLoader,
  private val updateChecker: UpdateChecker,
  private val updatesManager: UpdatesManager,
  private val networkMonitor: NetworkMonitor,
  private val settingsManager: SettingsManager,
  private val appInstallManager: AppInstallManager,
) : AndroidViewModel(app) {
  private val log = KotlinLogging.logger {}
  private val packageInfoFlow = MutableStateFlow<AppInfo?>(null)
  private val currentRepoIdFlow = MutableStateFlow<Long?>(null)
  private val moleculeScope =
    CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

  val appDetails: StateFlow<AppDetailsItem?> by
    lazy(LazyThreadSafetyMode.NONE) {
      moleculeScope.launchMolecule(mode = ContextClock) {
        DetailsPresenter(
          db = db,
          scope = scope,
          repoManager = repoManager,
          repoPreLoader = repoPreLoader,
          updateChecker = updateChecker,
          settingsManager = settingsManager,
          appInstallManager = appInstallManager,
          viewModel = this,
          packageInfoFlow = packageInfoFlow,
          currentRepoIdFlow = currentRepoIdFlow,
          appsWithIssuesFlow = updatesManager.appsWithIssues,
          networkStateFlow = networkMonitor.networkState,
        )
      }
    }

  init {
    loadPackageInfoFlow()
  }

  private fun loadPackageInfoFlow() {
    val packageManager = app.packageManager
    scope.launch {
      val packageInfo =
        try {
          @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, GET_SIGNATURES)
        } catch (_: PackageManager.NameNotFoundException) {
          null
        }
      packageInfoFlow.value =
        if (packageInfo == null) {
          AppInfo(packageName)
        } else {
          val intent =
            if (packageName == app.packageName) {
              null // we shouldn't launch ourselves, so no launch intent here
            } else {
              packageManager.getLaunchIntentForPackage(packageName)
            }
          AppInfo(packageName, packageInfo, intent)
        }
    }
  }

  @UiThread
  fun install(appMetadata: AppMetadata, version: AppVersion, iconModel: Any?) {
    scope.launch(Dispatchers.Main) {
      val result =
        appInstallManager.install(
          appMetadata = appMetadata,
          version = version,
          currentVersionName = packageInfoFlow.value?.packageInfo?.versionName,
          repo = repoManager.getRepository(version.repoId) ?: return@launch, // TODO
          iconModel = iconModel,
          canAskPreApprovalNow = true,
        )
      if (result is InstallState.Installed) {
        // to reload packageInfoFlow with fresh packageInfo
        loadPackageInfoFlow()
      }
    }
  }

  @UiThread
  fun requestUserConfirmation(installState: InstallState.UserConfirmationNeeded) {
    scope.launch(Dispatchers.Main) {
      val result = appInstallManager.requestUserConfirmation(packageName, installState)
      if (result is InstallState.Installed)
        withContext(Dispatchers.Main) {
          // to reload packageInfoFlow with fresh packageInfo
          loadPackageInfoFlow()
        }
    }
  }

  @UiThread
  fun checkUserConfirmation(installState: InstallState.UserConfirmationNeeded) {
    scope.launch(Dispatchers.Main) {
      delay(500) // wait a moment to increase chance that state got updated
      appInstallManager.checkUserConfirmation(packageName, installState)
    }
  }

  @UiThread
  fun cancelInstall() {
    appInstallManager.cancel(packageName)
  }

  @UiThread
  fun onUninstallResult(activityResult: ActivityResult) {
    val name = appDetails.value?.name
    val result = appInstallManager.onUninstallResult(packageName, name, activityResult)
    if (result is InstallState.Uninstalled) {
      // to reload packageInfoFlow with fresh packageInfo
      loadPackageInfoFlow()
    }
  }

  @UiThread
  fun onRepoChanged(repoId: Long) {
    currentRepoIdFlow.update { repoId }
  }

  @UiThread
  fun onPreferredRepoChanged(repoId: Long) {
    scope.launch {
      repoManager.setPreferredRepoId(packageName, repoId).join()
      updatesManager.loadUpdates()
    }
  }

  override fun onCleared() {
    log.info { "App details screen left: $packageName" }
    appInstallManager.cleanUp(packageName)
    // remove screenshots from disk cache to not fill it up quickly with large images
    val diskCache = SingletonImageLoader.get(application).diskCache
    if (diskCache != null)
      scope.launch {
        appDetails.value?.phoneScreenshots?.forEach { screenshot ->
          if (screenshot is DownloadRequest) {
            diskCache.remove(screenshot.getCacheKey())
          }
        }
      }
  }

  @UiThread
  fun allowBetaUpdates() {
    val appPrefs = appDetails.value?.appPrefs ?: return
    scope.launch {
      db.getAppPrefsDao().update(appPrefs.toggleReleaseChannel(RELEASE_CHANNEL_BETA))
      updatesManager.loadUpdates()
    }
  }

  @UiThread
  fun ignoreAllUpdates() {
    val appPrefs = appDetails.value?.appPrefs ?: return
    scope.launch {
      db.getAppPrefsDao().update(appPrefs.toggleIgnoreAllUpdates())
      updatesManager.loadUpdates()
    }
  }

  @UiThread
  fun ignoreThisUpdate() {
    val appPrefs = appDetails.value?.appPrefs ?: return
    val versionCode = appDetails.value?.possibleUpdate?.versionCode ?: return
    scope.launch {
      db.getAppPrefsDao().update(appPrefs.toggleIgnoreVersionCodeUpdate(versionCode))
      updatesManager.loadUpdates()
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(packageName: String): AppDetailsViewModel
  }
}

class AppInfo(
  val packageName: String,
  val packageInfo: PackageInfo? = null,
  val launchIntent: Intent? = null,
)
