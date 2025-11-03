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
import app.cash.molecule.RecompositionMode.Immediate
import app.cash.molecule.launchMolecule
import coil3.SingletonImageLoader
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
import org.fdroid.getCacheKey
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.RepoManager
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstallState
import org.fdroid.repo.RepoPreLoader
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    private val app: Application,
    @param:IoDispatcher private val scope: CoroutineScope,
    private val db: FDroidDatabase,
    private val repoManager: RepoManager,
    private val repoPreLoader: RepoPreLoader,
    private val updateChecker: UpdateChecker,
    private val updatesManager: UpdatesManager,
    private val settingsManager: SettingsManager,
    private val appInstallManager: AppInstallManager,
) : AndroidViewModel(app) {
    private val log = KotlinLogging.logger { }
    private val packageInfoFlow = MutableStateFlow<AppInfo?>(null)
    private val currentRepoIdFlow = MutableStateFlow<Long?>(null)

    val appDetails: StateFlow<AppDetailsItem?> = viewModelScope.launchMolecule(
        context = Dispatchers.IO, mode = Immediate,
    ) {
        DetailsPresenter(
            db = db,
            repoManager = repoManager,
            repoPreLoader = repoPreLoader,
            updateChecker = updateChecker,
            appInstallManager = appInstallManager,
            viewModel = this,
            packageInfoFlow = packageInfoFlow,
            currentRepoIdFlow = currentRepoIdFlow,
            settingsManager = settingsManager,
        )
    }

    fun setAppDetails(packageName: String) {
        packageInfoFlow.value = null
        loadPackageInfoFlow(packageName)
    }

    private fun loadPackageInfoFlow(packageName: String) {
        val packageManager = app.packageManager
        scope.launch {
            val packageInfo = try {
                packageManager.getPackageInfo(packageName, GET_SIGNATURES)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
            packageInfoFlow.value = if (packageInfo == null) {
                AppInfo(packageName)
            } else {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                AppInfo(packageName, packageInfo, intent)
            }
        }
    }

    @UiThread
    fun install(appMetadata: AppMetadata, version: AppVersion, iconModel: Any?) {
        scope.launch(Dispatchers.Main) {
            val result = appInstallManager.install(
                appMetadata = appMetadata,
                version = version,
                currentVersionName = packageInfoFlow.value?.packageInfo?.versionName,
                repo = repoManager.getRepository(version.repoId) ?: return@launch, // TODO
                iconModel = iconModel,
                canAskPreApprovalNow = true,
            )
            if (result is InstallState.Installed) {
                // to reload packageInfoFlow with fresh packageInfo
                loadPackageInfoFlow(appMetadata.packageName)
            }
        }
    }

    @UiThread
    fun requestUserConfirmation(
        packageName: String,
        installState: InstallState.UserConfirmationNeeded,
    ) {
        scope.launch(Dispatchers.Main) {
            val result = appInstallManager.requestUserConfirmation(packageName, installState)
            if (result is InstallState.Installed) withContext(Dispatchers.Main) {
                // to reload packageInfoFlow with fresh packageInfo
                loadPackageInfoFlow(packageName)
            }
        }
    }

    @UiThread
    fun checkUserConfirmation(
        packageName: String,
        installState: InstallState.UserConfirmationNeeded,
    ) {
        scope.launch(Dispatchers.Main) {
            delay(500) // wait a moment to increase chance that state got updated
            appInstallManager.checkUserConfirmation(packageName, installState)
        }
    }

    @UiThread
    fun cancelInstall(packageName: String) {
        appInstallManager.cancel(packageName)
    }

    @UiThread
    fun onUninstallResult(packageName: String, activityResult: ActivityResult) {
        val result = appInstallManager.onUninstallResult(packageName, activityResult)
        if (result is InstallState.Uninstalled) {
            // to reload packageInfoFlow with fresh packageInfo
            loadPackageInfoFlow(packageName)
        }
    }

    @UiThread
    fun onRepoChanged(repoId: Long) {
        currentRepoIdFlow.update { repoId }
    }

    @UiThread
    fun onPreferredRepoChanged(repoId: Long) {
        val packageName = packageInfoFlow.value?.packageName ?: error("Had not package name")
        repoManager.setPreferredRepoId(packageName, repoId)
    }

    override fun onCleared() {
        val packageName = packageInfoFlow.value?.packageName
        log.info { "App details screen left: $packageName" }
        packageName?.let {
            appInstallManager.cleanUp(it)
        }
        // remove screenshots from disk cache to not fill it up quickly with large images
        val diskCache = SingletonImageLoader.get(application).diskCache
        if (diskCache != null) scope.launch {
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
}

class AppInfo(
    val packageName: String,
    val packageInfo: PackageInfo? = null,
    val launchIntent: Intent? = null,
)
