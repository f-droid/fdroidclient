package org.fdroid.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit.DAYS
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.R
import org.fdroid.database.FDroidDatabase
import org.fdroid.settings.OnboardingManager
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.navigation.IntentRouter.Companion.ACTION_MY_APPS
import org.fdroid.ui.navigation.IntentRouter.Companion.ACTION_SEARCH
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher

@HiltViewModel
class MainViewModel
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val db: FDroidDatabase,
  settingsManager: SettingsManager,
  updatesManager: UpdatesManager,
  private val onboardingManager: OnboardingManager,
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
          numUpdatesFlow = updatesManager.numUpdates,
          appsWithIssuesFlow = updatesManager.appsWithIssues,
          showOnboardingFlow = onboardingManager.showMainOnboarding,
        )
      }
    }

  init {
    // only check for Fts integrity once a day, because it is an expensive operation
    if (System.currentTimeMillis() - settingsManager.lastDbRepairCheck > DAYS.toMillis(1)) {
      // check Fts integrity on worker thread after startup to avoid blocking all DB access
      coroutineScope.launch {
        delay(10.seconds) // give the app some time to start up before doing this
        try {
          db.repairFtsIfNeeded()
          settingsManager.lastDbRepairCheck = System.currentTimeMillis()
        } catch (e: Exception) {
          log.error(e) { "Error running Fts repair or check: " }
        }
      }
    }
    // create shortcuts dynamically, to avoid static shortcuts launching with NEW_TASK intent flag
    // https://developer.android.com/develop/ui/compose/system/shortcuts/managing-shortcuts#start-one
    val searchShortcut =
      ShortcutInfoCompat.Builder(context, "search")
        .setShortLabel(context.getString(R.string.menu_search))
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(Intent(ACTION_SEARCH).apply { setPackage(context.packageName) })
        .build()
    val myAppsShortcut =
      ShortcutInfoCompat.Builder(context, "my_apps")
        .setShortLabel(context.getString(R.string.menu_apps_my))
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(Intent(ACTION_MY_APPS).apply { setPackage(context.packageName) })
        .build()
    ShortcutManagerCompat.pushDynamicShortcut(context, searchShortcut)
    ShortcutManagerCompat.pushDynamicShortcut(context, myAppsShortcut)
  }

  fun onOnboardingSeen() = onboardingManager.onMainOnboardingSeen()
}
