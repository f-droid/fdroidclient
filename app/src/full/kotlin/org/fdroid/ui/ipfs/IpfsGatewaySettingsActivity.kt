package org.fdroid.ui.ipfs

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.fdroid.ui.FDroidContent
import javax.inject.Inject

@AndroidEntryPoint
class IpfsGatewaySettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var manager: IpfsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FDroidContent {
                SettingsScreen(
                    prefs = manager.preferences.collectAsStateWithLifecycle().value,
                    actions = manager,
                    onBackClicked = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }
}
