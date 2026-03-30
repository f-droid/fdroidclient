package org.fdroid.ui.nearby

import android.annotation.SuppressLint
import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.AppMetadata
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.fdroid.nearby.SwapService
import org.fdroid.index.v1.AppV1
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v1.PackageV1
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstallConfirmationState
import org.fdroid.install.InstallState
import org.fdroid.install.InstalledAppsCache
import org.fdroid.settings.SettingsManager

@HiltViewModel
class SwapSuccessViewModel
@Inject
constructor(
  app: Application,
  private val appInstallManager: AppInstallManager,
  private val installedAppsCache: InstalledAppsCache,
  private val settingsManager: SettingsManager,
) : AndroidViewModel(app) {

  private val log = KotlinLogging.logger {}

  // the service should be running for the lifetime of the view model
  @SuppressLint("StaticFieldLeak") private var service: SwapService? = null

  private val installedApps
    get() = installedAppsCache.installedApps.value

  private val localeList = LocaleListCompat.getDefault()
  private val peerRepo = MutableStateFlow<Repository?>(null)
  private val repoApps = MutableStateFlow<List<SwapRepoApp>?>(null)

  val model: StateFlow<SwapSuccessModel> =
    combine(repoApps, appInstallManager.appInstallStates) { repoApps, installStates ->
        val apps = repoApps?.map { repoApp ->
          val installedPackage = installedApps[repoApp.packageName]
          val iconModel =
            if (installedPackage != null) {
              PackageName(repoApp.packageName, repoApp.iconRequest)
            } else {
              repoApp.iconRequest
            }
          SwapSuccessItem(
            packageName = repoApp.packageName,
            name = repoApp.name,
            versionName = repoApp.versionName,
            versionCode = repoApp.versionCode,
            installedVersionName = installedPackage?.versionName,
            installedVersionCode = installedPackage?.let(PackageInfoCompat::getLongVersionCode),
            iconModel = iconModel,
            installState = installStates[repoApp.packageName] ?: InstallState.Unknown,
          )
        }
        SwapSuccessModel(
          apps = apps ?: emptyList(),
          loading = apps == null,
          appToConfirm =
            apps
              ?.filter { it.installState is InstallConfirmationState }
              ?.minByOrNull { (it.installState as InstallConfirmationState).creationTimeMillis },
        )
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SwapSuccessModel(loading = true),
      )

  fun onServiceConnected(service: SwapService) {
    log.info { "Connected to service: $service" }
    service.index.observeForever(this::onIndexChanged)
    this.service = service
  }

  fun onServiceDisconnected(service: SwapService) {
    log.info { "Disconnected from service: $service" }
    service.index.removeObserver(this::onIndexChanged)
    this.service = null
  }

  fun onIndexChanged(indexV1: IndexV1) {
    val repo = service?.peerRepo
    peerRepo.value = repo
    repoApps.value =
      repo?.let { repo ->
        indexV1.apps.mapNotNull { app ->
          app.toSwapRepoApp(indexV1.packages[app.packageName], repo)
        }
      } ?: emptyList()
  }

  fun install(packageName: String) {
    val repoApp = repoApps.value?.firstOrNull { it.packageName == packageName } ?: return
    val repo = peerRepo.value ?: return
    val currentVersionName =
      model.value.apps.firstOrNull { it.packageName == packageName }?.installedVersionName
    viewModelScope.launch(Dispatchers.Main) {
      appInstallManager.install(
        packageName = packageName,
        appMetadata = repoApp.appMetadata,
        version = repoApp.packageVersion,
        currentVersionName = currentVersionName,
        repo = repo,
        iconModel = repoApp.iconRequest,
        canAskPreApprovalNow = true,
      )
    }
  }

  fun confirmAppInstall(packageName: String, state: InstallConfirmationState) {
    viewModelScope.launch(Dispatchers.Main) {
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

  fun checkUserConfirmation(packageName: String, state: InstallState.UserConfirmationNeeded) {
    viewModelScope.launch(Dispatchers.Main) {
      delay(500)
      appInstallManager.checkUserConfirmation(packageName, state)
    }
  }

  fun cancelInstall(packageName: String) {
    appInstallManager.cancel(packageName)
  }

  override fun onCleared() {
    repoApps.value?.forEach { appInstallManager.cleanUp(it.packageName) }
    super.onCleared()
  }

  private data class SwapRepoApp(
    val packageName: String,
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val iconRequest: DownloadRequest?,
    val appMetadata: AppMetadata,
    val packageVersion: PackageVersionV2,
  )

  private fun AppV1.toSwapRepoApp(
    packages: List<PackageV1>?,
    repository: Repository,
  ): SwapRepoApp? {
    val packageV1 = packages?.firstOrNull() ?: return null
    val packageVersion =
      packageV1.toPackageVersionV2(
        releaseChannels = emptyList(),
        appAntiFeatures = emptyMap(),
        whatsNew = emptyMap(),
      )
    val metadataV2 = toMetadataV2(packageVersion.signer?.sha256?.firstOrNull())
    val metadata = metadataV2.toAppMetadata(repository.repoId, packageName, localeList)
    val iconRequest =
      metadataV2.icon
        ?.getBestLocale(localeList)
        ?.getImageModel(repository, settingsManager.proxyConfig) as? DownloadRequest
    return SwapRepoApp(
      packageName = packageName,
      name = metadata.localizedName ?: name ?: packageName,
      versionName = packageVersion.versionName,
      versionCode = packageVersion.versionCode,
      iconRequest = iconRequest,
      appMetadata = metadata,
      packageVersion = packageVersion,
    )
  }
}

private fun MetadataV2.toAppMetadata(
  repoId: Long,
  packageName: String,
  localeList: LocaleListCompat,
): AppMetadata =
  AppMetadata(
    repoId = repoId,
    packageName = packageName,
    added = added,
    lastUpdated = lastUpdated,
    name = name,
    summary = summary,
    description = description,
    localizedName = name.getBestLocale(localeList),
    localizedSummary = summary.getBestLocale(localeList),
    webSite = webSite,
    changelog = changelog,
    license = license,
    sourceCode = sourceCode,
    issueTracker = issueTracker,
    translation = translation,
    preferredSigner = preferredSigner,
    video = video,
    authorName = authorName,
    authorEmail = authorEmail,
    authorWebSite = authorWebSite,
    authorPhone = authorPhone,
    donate = donate,
    liberapayID = liberapayID,
    liberapay = liberapay,
    openCollective = openCollective,
    bitcoin = bitcoin,
    litecoin = litecoin,
    flattrID = flattrID,
    categories = categories,
    isCompatible = true,
  )
