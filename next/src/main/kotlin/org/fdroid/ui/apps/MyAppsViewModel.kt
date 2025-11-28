package org.fdroid.ui.apps

import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstallConfirmationState
import org.fdroid.install.InstallState
import org.fdroid.install.InstalledAppsCache
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject

@HiltViewModel
class MyAppsViewModel @Inject constructor(
    app: Application,
    @param:IoDispatcher private val scope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
    private val db: FDroidDatabase,
    private val settingsManager: SettingsManager,
    private val installedAppsCache: InstalledAppsCache,
    private val appInstallManager: AppInstallManager,
    private val updatesManager: UpdatesManager,
    private val repoManager: RepoManager,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger { }
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
                    InstalledAppItem(
                        packageName = app.packageName,
                        name = app.name ?: "Unknown app",
                        installedVersionName = app.installedVersionName ?: "???",
                        lastUpdated = app.lastUpdated,
                        iconModel = repoManager.getRepository(app.repoId)?.let { repo ->
                            app.getIcon(localeList)?.getImageModel(repo, proxyConfig)
                        },
                    )
                }
            }
        }

    private val searchQuery = savedStateHandle.getMutableStateFlow("query", "")
    private val sortOrder = savedStateHandle.getMutableStateFlow("sort", AppListSortOrder.NAME)
    val myAppsModel: StateFlow<MyAppsModel> = moleculeScope.launchMolecule(mode = ContextClock) {
        MyAppsPresenter(
            appUpdatesFlow = updates,
            appInstallStatesFlow = appInstallManager.appInstallStates,
            appsWithIssuesFlow = updatesManager.appsWithIssues,
            installedAppsFlow = installedAppItems,
            searchQueryFlow = searchQuery,
            sortOrderFlow = sortOrder,
        )
    }

    fun updateAll() {
        scope.launch {
            updatesManager.updateAll()
        }
    }

    fun search(query: String) {
        searchQuery.value = query
    }

    fun changeSortOrder(sort: AppListSortOrder) {
        sortOrder.value = sort
    }

    fun refresh() {
        // TODO check if really not needed anymore and if so remove
        // updatesManager.loadUpdates()
    }

    fun confirmAppInstall(packageName: String, state: InstallConfirmationState) {
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
}
