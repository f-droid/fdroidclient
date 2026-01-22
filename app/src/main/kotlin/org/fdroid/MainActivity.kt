package org.fdroid

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.fdroid.ui.Main

// Using [AppCompatActivity] and not [ComponentActivity] seems to be needed
// for automatic theme changes when calling AppCompatDelegate.setDefaultNightMode()
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    val requestPermissionLauncher = registerForActivityResult(RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Main {
                // inform OnNewIntentListeners about the initial intent (otherwise would be missed)
                if (savedInstanceState == null && intent != null) {
                    onNewIntent(intent)
                } else if (savedInstanceState != null && intent != null) {
                    Log.w("MainActivity", "Ignored intent due to savedInstanceState: $intent")
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
