package org.fdroid.ui.details

import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import app.cash.molecule.RecompositionMode.Immediate
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.fdroid.UpdateChecker
import org.fdroid.database.FDroidDatabase
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.RepoManager
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    private val app: Application,
    @IoDispatcher private val scope: CoroutineScope,
    private val db: FDroidDatabase,
    private val repoManager: RepoManager,
    private val updateChecker: UpdateChecker,
    private val updatesManager: UpdatesManager,
) : AndroidViewModel(app) {
    private val packageInfoFlow = MutableStateFlow<AppInfo?>(null)

    val appDetails: StateFlow<AppDetailsItem?> = scope.launchMolecule(
        context = Dispatchers.IO, mode = Immediate,
    ) {
        DetailsPresenter(
            db = db,
            repoManager = repoManager,
            updateChecker = updateChecker,
            viewModel = this,
            packageInfoFlow = packageInfoFlow,
        )
    }

    fun setAppDetails(packageName: String) {
        packageInfoFlow.value = null
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

    // TODO update app search when preferred repo changes
}

class AppInfo(
    val packageName: String,
    val packageInfo: PackageInfo? = null,
    val launchIntent: Intent? = null,
)
