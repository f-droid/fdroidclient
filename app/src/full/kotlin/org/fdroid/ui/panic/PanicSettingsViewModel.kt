package org.fdroid.ui.panic

import android.app.Application
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.guardianproject.panic.PanicResponder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.fdroid.database.FDroidDatabase
import org.fdroid.repo.RepoPreLoader
import org.fdroid.settings.SettingsManager
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject

@HiltViewModel
class PanicSettingsViewModel @Inject constructor(
    app: Application,
    private val db: FDroidDatabase,
    private val repoPreLoader: RepoPreLoader,
    private val settingsManager: SettingsManager,
    @param:IoDispatcher private val ioScope: CoroutineScope,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger {}

    val prefsFlow = settingsManager.prefsFlow
    val appFlow = prefsFlow.map { it.get<String>("pref_panic_app") }.distinctUntilChanged()
    val resetRepos get() = settingsManager.prefs.getBoolean("pref_panic_reset_repos", false)
    val exitApp get() = settingsManager.prefs.getBoolean("pref_panic_exit", true)

    private val pm = app.packageManager
    private val _state = MutableStateFlow(PanicSettingsState())
    val state = _state.asStateFlow()

    init {
        ioScope.launch {
            val apps = listOf(null) + PanicResponder.resolveTriggerApps(pm).map { info ->
                info.activityInfo.toPanicApp()
            }
            val selected = PanicResponder.getTriggerPackageName(application)
            _state.value = PanicSettingsState(
                panicApps = apps,
                selectedPanicApp = if (selected == null) {
                    null
                } else {
                    getPanicApp(selected)
                },
            )
        }
        // react to panic app changes right away
        viewModelScope.launch {
            appFlow.drop(1).collect { packageName ->
                _state.update {
                    it.copy(selectedPanicApp = getPanicApp(packageName))
                }
            }
        }
    }

    fun resetDb() {
        val job = ioScope.launch {
            db.getRepositoryDao().clearAll()
            repoPreLoader.addPreloadedRepositories(db)
        }
        // hard wait for data to be cleared
        runBlocking {
            job.join()
        }
    }

    private fun getPanicApp(packageName: String?): PanicApp? {
        if (packageName == null) return null
        return pm.getPackageInfo(packageName, 0)?.applicationInfo?.toPanicApp()
    }

    private fun ApplicationInfo.toPanicApp() = PanicApp(
        packageName = packageName,
        name = loadLabel(pm).toString(),
    )

    private fun ActivityInfo.toPanicApp() = PanicApp(
        packageName = packageName,
        name = loadLabel(pm).toString(),
    )

}
