package org.fdroid

import android.Manifest.permission.POST_NOTIFICATIONS
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
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.Main

// Using [AppCompatActivity] and not [ComponentActivity] seems to be needed
// for automatic theme changes when calling AppCompatDelegate.setDefaultNightMode()
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  val requestPermissionLauncher = registerForActivityResult(RequestPermission()) {}

  @Inject lateinit var settingsManager: SettingsManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
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
      requestPermissionLauncher.launch(POST_NOTIFICATIONS)
    }
  }
}
