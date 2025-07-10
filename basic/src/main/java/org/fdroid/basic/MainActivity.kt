package org.fdroid.basic

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
import org.fdroid.basic.ui.main.Main

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, POST_NOTIFICATIONS
            ) != PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS)
        }
        setContent {
            Main()
        }
    }
}
