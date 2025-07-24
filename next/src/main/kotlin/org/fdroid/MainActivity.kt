package org.fdroid

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.fdroid.ui.Main

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Main {
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
