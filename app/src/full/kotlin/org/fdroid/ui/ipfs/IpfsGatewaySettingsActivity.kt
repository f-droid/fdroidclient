package org.fdroid.ui.ipfs

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.ipfs.IpfsManager.Companion.DEFAULT_GATEWAYS
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
                    prefs = manager,
                    onAddUserGateway = { url ->
                        // don't allow adding default gateways to the user gateways list
                        if (!DEFAULT_GATEWAYS.contains(url)) {
                            val updatedUserGwList = manager.ipfsGwUserList.toMutableList()
                            // don't allow double adding gateways
                            if (!updatedUserGwList.contains(url)) {
                                updatedUserGwList.add(url)
                                manager.ipfsGwUserList = updatedUserGwList
                            }
                        }
                    },
                    onBackClicked = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }
}
