package org.fdroid.basic.ui.main.apps

import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.basic.download.getDownloadRequest
import org.fdroid.basic.manager.MyAppsManager
import org.fdroid.basic.repo.RepositoryManager
import org.fdroid.database.AppListItem
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.DownloadRequest
import javax.inject.Inject

data class InstalledAppItem(
    val packageName: String,
    val name: String,
    val installedVersionName: String,
    val lastUpdated: Long,
    val iconDownloadRequest: DownloadRequest? = null,
)

@HiltViewModel
class MyAppsViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    private val db: FDroidDatabase,
    private val myAppsManager: MyAppsManager,
    private val repositoryManager: RepositoryManager,
) : AndroidViewModel(app) {

    val updates = myAppsManager.updates
    val numUpdates = myAppsManager.numUpdates
    private val installedApps = MutableStateFlow<List<InstalledAppItem>?>(null)
    private var installedAppsLiveData =
        db.getAppDao().getInstalledAppListItems(application.packageManager)
    private val sortOrder = savedStateHandle.getMutableStateFlow("sort", AppListSortOrder.NAME)
    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    private val localeList = LocaleListCompat.getDefault()
    private val installedAppsObserver = Observer<List<AppListItem>> { list ->
        installedApps.value = list.map { app ->
            InstalledAppItem(
                packageName = app.packageName,
                name = app.name ?: "Unknown app",
                installedVersionName = app.installedVersionName ?: "???",
                lastUpdated = app.lastUpdated,
                iconDownloadRequest = repositoryManager.getRepository(app.repoId)?.let { repo ->
                    app.getIcon(localeList)?.getDownloadRequest(repo)
                },
            )
        }
    }
    val myAppsModel: StateFlow<MyAppsModel> = scope.launchMolecule(mode = ContextClock) {
        MyAppsPresenter(
            appUpdatesFlow = updates,
            installedAppsFlow = installedApps,
            sortOrderFlow = sortOrder,
        )
    }

    init {
        installedAppsLiveData.observeForever(installedAppsObserver)
    }

    override fun onCleared() {
        installedAppsLiveData.removeObserver(installedAppsObserver)
    }

    fun changeSortOrder(sort: AppListSortOrder) {
        sortOrder.value = sort
    }

    fun refresh() {
        myAppsManager.loadUpdates()

        // need to get new liveData from the DB, so it re-queries installed packages
        installedAppsLiveData.removeObserver(installedAppsObserver)
        installedAppsLiveData =
            db.getAppDao().getInstalledAppListItems(application.packageManager).apply {
                observeForever(installedAppsObserver)
            }
    }
}
