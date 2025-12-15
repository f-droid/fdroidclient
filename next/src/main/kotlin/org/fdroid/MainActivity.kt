package org.fdroid

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.Main
import javax.inject.Inject

// Using [AppCompatActivity] and not [ComponentActivity] seems to be needed
// for automatic theme changes when calling AppCompatDelegate.setDefaultNightMode()
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dynamicColors = settingsManager.dynamicColorFlow.collectAsStateWithLifecycle(
                PREF_DEFAULT_DYNAMIC_COLORS
            ).value
            Main(dynamicColors) {
                // inform OnNewIntentListeners about the initial intent (otherwise would be missed)
                if (savedInstanceState == null && intent != null) {
                    onNewIntent(intent)
                }
            }
        }
        if (SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS)
        }
    }
}
