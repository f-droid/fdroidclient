package org.fdroid.ui.history

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.DownloadRequest
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.history.HistoryManager
import org.fdroid.index.RepoManager
import org.fdroid.settings.SettingsManager
import org.fdroid.utils.IoDispatcher

@HiltViewModel
class HistoryViewModel
@Inject
constructor(
  app: Application,
  private val db: FDroidDatabase,
  private val repoManager: RepoManager,
  private val historyManager: HistoryManager,
  private val settingsManager: SettingsManager,
  @param:IoDispatcher private val scope: CoroutineScope,
) : AndroidViewModel(app) {

  private val _items = MutableStateFlow<List<HistoryItem>?>(null)
  val items = _items.asStateFlow()
  val useInstallHistory = settingsManager.useInstallHistoryFlow

  init {
    scope.launch { load() }
  }

  @WorkerThread
  private suspend fun load() {
    val packageNames = mutableSetOf<String>()
    val items =
      historyManager
        .getEvents()
        .map { event ->
          packageNames.add(event.packageName)
          HistoryItem(event = event, iconModel = PackageName(event.packageName, null))
        }
        .sortedByDescending { it.event.time }
    _items.value = items
    // second pass to also load icons
    if (packageNames.isNotEmpty()) {
      val proxyConfig = settingsManager.proxyConfig
      val locales = LocaleListCompat.getDefault()
      val apps = db.getAppDao().getApps(packageNames.toList()).associateBy { it.packageName }
      val items =
        historyManager
          .getEvents()
          .map { event ->
            val iconRequest = run {
              val app = apps[event.packageName] ?: return@run null
              val repository = repoManager.getRepository(app.repoId) ?: return@run null
              val icon = app.getIcon(locales)
              icon?.getImageModel(repository, proxyConfig) as? DownloadRequest
            }
            HistoryItem(event = event, iconModel = PackageName(event.packageName, iconRequest))
          }
          .sortedByDescending { it.event.time }
      _items.value = items
    }
  }

  fun useInstallHistory(use: Boolean) {
    settingsManager.useInstallHistory = use
  }

  fun deleteHistory() {
    scope.launch {
      historyManager.clearAll()
      load()
    }
  }
}
