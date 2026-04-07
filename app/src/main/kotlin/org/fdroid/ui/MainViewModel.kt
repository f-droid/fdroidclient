package org.fdroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit.DAYS
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.database.FDroidDatabase
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher

@HiltViewModel
class MainViewModel
@Inject
constructor(
  private val db: FDroidDatabase,
  settingsManager: SettingsManager,
  updatesManager: UpdatesManager,
  @param:IoDispatcher val coroutineScope: CoroutineScope,
) : ViewModel() {

  private val log = KotlinLogging.logger {}
  private val moleculeScope =
    CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

  val mainModel: StateFlow<MainModel> by
    lazy(LazyThreadSafetyMode.NONE) {
      moleculeScope.launchMolecule(mode = ContextClock) {
        MainPresenter(
          dynamicColorsFlow = settingsManager.dynamicColorFlow,
          smallBottomBarFlow = settingsManager.smallBottomBarFlow,
          numUpdatesFlow = updatesManager.numUpdates,
          appsWithIssuesFlow = updatesManager.appsWithIssues,
        )
      }
    }

  init {
    // only check for Fts integrity once a day, because it is an expensive operation
    if (System.currentTimeMillis() - settingsManager.lastDbRepairCheck > DAYS.toMillis(1)) {
      // check Fts integrity on worker thread after startup to avoid blocking all DB access
      coroutineScope.launch {
        delay(5000) // give the app some time to start up before doing this
        try {
          db.repairFtsIfNeeded()
          settingsManager.lastDbRepairCheck = System.currentTimeMillis()
        } catch (e: Exception) {
          log.error(e) { "Error running Fts repair or check: " }
        }
      }
    }
  }
}
