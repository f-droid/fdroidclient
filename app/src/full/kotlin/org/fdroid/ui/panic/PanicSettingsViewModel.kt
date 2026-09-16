package org.fdroid.ui.panic

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.guardianproject.panic.Panic
import info.guardianproject.panic.PanicResponder
import info.guardianproject.panic.PanicResponder.PREF_TRIGGER_PACKAGE_NAME
import javax.inject.Inject
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
import org.fdroid.ui.settings.AppIcon
import org.fdroid.ui.settings.AppIconManager
import org.fdroid.utils.IoDispatcher

@HiltViewModel
class PanicSettingsViewModel
@Inject
constructor(
  app: Application,
  private val db: FDroidDatabase,
  private val repoPreLoader: RepoPreLoader,
  private val settingsManager: SettingsManager,
  private val appIconManager: AppIconManager,
  @param:IoDispatcher private val ioScope: CoroutineScope,
) : AndroidViewModel(app) {

  private val log = KotlinLogging.logger {}

  val prefsFlow = settingsManager.prefsFlow
  val appFlow = prefsFlow.map { it.get<String>(PREF_TRIGGER_PACKAGE_NAME) }.distinctUntilChanged()
  val hideApp
    get() = settingsManager.prefs.getBoolean("pref_panic_hide", false)

  val resetRepos
    get() = settingsManager.prefs.getBoolean("pref_panic_reset_repos", false)

  val exitApp
    get() = settingsManager.prefs.getBoolean("pref_panic_exit", true)

  private val pm = app.packageManager
  private val _state = MutableStateFlow(PanicSettingsState())
  val state = _state.asStateFlow()

  var wasExitSet = exitApp

  var wasHideSet = hideApp

  private val syncExitHidePrefs = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    if (key == "pref_panic_exit") {
      val isExitSet = prefs.getBoolean("pref_panic_exit", true)
      // check whether exit preference was changed before attempting to sync
      if (isExitSet != wasExitSet) {
        wasExitSet = isExitSet
        val isHideSet = prefs.getBoolean("pref_panic_hide", false)
        // if exit is off, also set hide off
        if (!isExitSet && isHideSet) {
          prefsFlow.update { it.toMutablePreferences().apply { this["pref_panic_hide"] = false } }
        }
      }
    } else if (key == "pref_panic_hide") {
      val isHideSet = prefs.getBoolean("pref_panic_hide", false)
      // check whether hide preference was changed before attempting to sync
      if (isHideSet != wasHideSet) {
        wasHideSet = isHideSet
        val isExitSet = prefs.getBoolean("pref_panic_exit", true)
        // if hide is on, also set exit on
        if (isHideSet && !isExitSet) {
          prefsFlow.update { it.toMutablePreferences().apply { this["pref_panic_exit"] = true } }
        }
      }
    }
  }

  init {
    settingsManager.prefs.registerOnSharedPreferenceChangeListener(syncExitHidePrefs)
    ioScope.launch {
      val apps =
        listOf(null) +
          PanicResponder.resolveTriggerApps(pm).map { info -> info.activityInfo.toPanicApp() }
      val selected = PanicResponder.getTriggerPackageName(application)
      _state.value =
        PanicSettingsState(
          panicApps = apps,
          selectedPanicApp =
            if (selected == null || selected == Panic.PACKAGE_NAME_NONE) {
              null
            } else {
              getPanicApp(selected)
            },
        )
    }
    // react to panic app changes right away
    viewModelScope.launch {
      appFlow.drop(1).collect { packageName ->
        _state.update { it.copy(selectedPanicApp = getPanicApp(packageName)) }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    // remove listener to avoid leaks
    settingsManager.prefs.unregisterOnSharedPreferenceChangeListener(syncExitHidePrefs)
  }

  fun changeAppIcon(appIcon: AppIcon) {
    appIconManager.setAppIcon(appIcon)
  }

  fun resetDb() {
    val job = ioScope.launch {
      db.getRepositoryDao().clearAll()
      repoPreLoader.addPreloadedRepositories(db)
    }
    // hard wait for data to be cleared
    runBlocking { job.join() }
  }

  private fun getPanicApp(packageName: String?): PanicApp? {
    if (packageName == null) return null
    return try {
      pm.getPackageInfo(packageName, 0)?.applicationInfo?.toPanicApp()
    } catch (e: Exception) {
      log.error(e) { "Failed to get package info for $packageName" }
      null
    }
  }

  private fun ApplicationInfo.toPanicApp() =
    PanicApp(packageName = packageName, name = loadLabel(pm).toString())

  private fun ActivityInfo.toPanicApp() =
    PanicApp(packageName = packageName, name = loadLabel(pm).toString())

  // provide a way to set this preference directly when receiving intent from panic app
  fun setTriggerPackageName(triggerPackageName: String) {
    // update the flow so the drop-down shows the correct value selected
    prefsFlow.update {
      it.toMutablePreferences().apply { this[PREF_TRIGGER_PACKAGE_NAME] = triggerPackageName }
    }

    // additionally update the state so that the drop-down preview shows the correct value
    _state.update { it.copy(selectedPanicApp = getPanicApp(triggerPackageName)) }
  }
}
