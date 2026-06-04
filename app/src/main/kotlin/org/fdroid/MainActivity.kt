package org.fdroid

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.Main
import org.fdroid.ui.utils.launchSafe

// Using [AppCompatActivity] and not [ComponentActivity] seems to be needed
// for automatic theme changes when calling AppCompatDelegate.setDefaultNightMode()
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  private val log = KotlinLogging.logger {}

  val requestPermissionLauncher = registerForActivityResult(RequestPermission()) {}

  @Inject lateinit var settingsManager: SettingsManager
  @Inject lateinit var notificationManager: NotificationManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log.info { "onCreate($savedInstanceState)" }
    // observe preventScreenshots setting and react to changes
    lifecycleScope.launch {
      // this flow doesn't change when we are paused, so we keep collecting it
      settingsManager.preventScreenshotsFlow.collect { preventScreenshots ->
        if (preventScreenshots) {
          window?.addFlags(FLAG_SECURE)
        } else {
          window?.clearFlags(FLAG_SECURE)
        }
      }
    }
    enableEdgeToEdge()
    setContent {
      Main {
        // inform OnNewIntentListeners about the initial intent (otherwise would be missed)
        log.info { "Passing initial intent: $intent" }
        if (intent != null) {
          onNewIntent(intent)
          // set intent to null to avoid re-processing on configuration changes
          intent = null
        }
      }
    }
    if (
      SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PERMISSION_GRANTED
    ) {
      requestPermissionLauncher.launchSafe(POST_NOTIFICATIONS)
    }
  }

  override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
    log.info {
      val callerInfo = if (SDK_INT >= 35) caller.`package` else caller.toString()
      "onNewIntent: $intent, caller: $callerInfo"
    }
    // if the app got killed and restarted via an intent,
    // this may run before the IntentListener is attached, so it would miss this intent
    if (SDK_INT >= 35) setIntent(intent, caller) else setIntent(intent)

    // calling super seems to be needed, so the IntentListener gets informed
    super.onNewIntent(intent, caller)
  }

  override fun onResume() {
    super.onResume()
    notificationManager.cancelSelfUpdateNotification()
  }
}
