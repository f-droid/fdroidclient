package org.fdroid.fdroid.views.appdetails

import android.app.Application
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fdroid.database.App
import org.fdroid.database.AppPrefs
import org.fdroid.database.AppVersion
import org.fdroid.database.Repository
import org.fdroid.fdroid.AppUpdateStatusManager
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.data.Apk.RELEASE_CHANNEL_BETA
import org.fdroid.fdroid.data.DBHelper

data class AppData(
    val appPrefs: AppPrefs,
    val preferredRepoId: Long,
    /**
     * A list of [Repository]s the app is in. If this is empty, the list doesn't matter,
     * because the user only has one repo.
     */
    val repos: List<Repository>,
)

class AppDetailsViewModel(app: Application) : AndroidViewModel(app) {

    private val _app = MutableLiveData<App?>()
    val app: LiveData<App?> = _app
    private val _versions = MutableLiveData<List<AppVersion>>()
    val versions: LiveData<List<AppVersion>> = _versions
    private val _appData = MutableLiveData<AppData>()
    val appData: LiveData<AppData> = _appData

    private val db = DBHelper.getDb(app.applicationContext)
    private val repoManager = FDroidApp.getRepoManager(app.applicationContext)
    private var packageName: String? = null
    private var appLiveData: LiveData<App?>? = null
    private var versionsLiveData: LiveData<List<AppVersion>>? = null
    private var appPrefsLiveData: LiveData<AppPrefs>? = null
    private var preferredRepoId: Long? = null
    private var repos: List<Repository>? = null

    @UiThread
    fun loadApp(packageName: String) {
        if (this.packageName == packageName) return // already set and loaded
        if (this.packageName != null && this.packageName != packageName) error {
            "Called loadApp() with different packageName."
        }
        this.packageName = packageName

        // load app and observe changes
        // this is a bit hacky, but uses the existing DB API made for old Java code
        appLiveData?.removeObserver(onAppChanged)
        appLiveData = db.getAppDao().getApp(packageName).also { liveData ->
            liveData.observeForever(onAppChanged)
        }
        // load repos for app, if user have more than one (+ one archive) repo
        if (repoManager.getRepositories().size > 2) viewModelScope.launch {
            loadRepos(packageName)
        }
        // load appPrefs
        appPrefsLiveData = db.getAppPrefsDao().getAppPrefs(packageName).also { liveData ->
            liveData.observeForever(onAppPrefsChanged)
        }
    }

    override fun onCleared() {
        appLiveData?.removeObserver(onAppChanged)
        appPrefsLiveData?.removeObserver(onAppPrefsChanged)
        versionsLiveData?.removeObserver(onVersionsChanged)
    }

    @UiThread
    fun selectRepo(repoId: Long) {
        appLiveData?.removeObserver(onAppChanged)
        viewModelScope.launch(Dispatchers.IO) {
            // this will lose observation of changes in the DB, but uses existing API
            _app.postValue(db.getAppDao().getApp(repoId, packageName ?: error("")))
        }
        tryToPublishAppData()
        resetVersionsLiveData(repoId)
    }

    @UiThread
    fun setPreferredRepo(repoId: Long) {
        repoManager.setPreferredRepoId(packageName ?: error(""), repoId)
    }

    private val onAppChanged: Observer<App?> = Observer { app ->
        // set repoIds on first load
        if (_app.value == null && app != null) {
            preferredRepoId = app.repoId // DB loads preferred repo first
            resetVersionsLiveData(app.repoId)
            tryToPublishAppData()
        }
        _app.value = app
    }

    private val onAppPrefsChanged: Observer<AppPrefs> = Observer { appPrefs ->
        if (appPrefs.preferredRepoId != null) preferredRepoId = appPrefs.preferredRepoId
        tryToPublishAppData()
    }

    private val onVersionsChanged: Observer<List<AppVersion>> = Observer { versions ->
        _versions.value = versions
    }

    private suspend fun loadRepos(packageName: String) = withContext(Dispatchers.IO) {
        repos = db.getAppDao().getRepositoryIdsForApp(packageName).mapNotNull { repoId ->
            repoManager.getRepository(repoId)
        }
        tryToPublishAppData()
    }

    private fun tryToPublishAppData() {
        val data = AppData(
            appPrefs = appPrefsLiveData?.value ?: return,
            preferredRepoId = preferredRepoId ?: return,
            repos = repos ?: emptyList(),
        )
        _appData.postValue(data)
    }

    private fun resetVersionsLiveData(repoId: Long) {
        versionsLiveData?.removeObserver(onVersionsChanged)
        val packageName = this.packageName ?: error("packageName not initialized")
        versionsLiveData = db.getVersionDao().getAppVersions(repoId, packageName).also { liveData ->
            liveData.observeForever(onVersionsChanged)
        }
    }

    /* AppPrefs methods */

    fun ignoreAllUpdates() = viewModelScope.launch(Dispatchers.IO) {
        val appPrefs = appPrefsLiveData?.value ?: return@launch
        db.getAppPrefsDao().update(appPrefs.toggleIgnoreAllUpdates())
        AppUpdateStatusManager.getInstance(getApplication()).checkForUpdates()
    }

    fun ignoreVersionCodeUpdate(versionCode: Long) = viewModelScope.launch(Dispatchers.IO) {
        val appPrefs = appPrefsLiveData?.value ?: return@launch
        db.getAppPrefsDao().update(appPrefs.toggleIgnoreVersionCodeUpdate(versionCode))
        AppUpdateStatusManager.getInstance(getApplication()).checkForUpdates()
    }

    fun toggleBetaReleaseChannel() = viewModelScope.launch(Dispatchers.IO) {
        val appPrefs = appPrefsLiveData?.value ?: return@launch
        db.getAppPrefsDao().update(appPrefs.toggleReleaseChannel(RELEASE_CHANNEL_BETA))
        AppUpdateStatusManager.getInstance(getApplication()).checkForUpdates()
    }

}
