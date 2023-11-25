package org.fdroid.fdroid.views

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import org.fdroid.fdroid.IPreferencesIpfs
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.R
import org.fdroid.fdroid.compose.ComposeUtils
import org.fdroid.fdroid.compose.ComposeUtils.FDroidContent
import org.fdroid.fdroid.compose.ComposeUtils.LifecycleEventListener

class IpfsGatewaySettingsActivity : ComponentActivity() {

    lateinit var prefs: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = Preferences.get()

        setContent {
            FDroidContent {
                IpfsGatewaySettingsScreen(prefs = prefs,
                    onBackClicked = { onBackPressedDispatcher.onBackPressed() })
            }
        }
    }
}

@Composable
fun IpfsGatewaySettingsScreen(
    onBackClicked: () -> Unit,
    prefs: IPreferencesIpfs,
) {
    val context = LocalContext.current
    var ipfsEnabled by remember { mutableStateOf(prefs.isIpfsEnabled) }

    Scaffold(topBar = {
        TopAppBar(
            elevation = 4.dp,
            backgroundColor = MaterialTheme.colors.primarySurface,
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.ipfsgw_title),
                    modifier = Modifier.alpha(ContentAlpha.high),
                )
            },
        )
    }, floatingActionButton = {
        // it doesn't seam to be supported to disable FABs, so just hide it for now.
        if (ipfsEnabled) {
            FloatingActionButton(onClick = {
                context.startActivity(Intent(context, IpfsGatewayAddActivity::class.java))
            }) {
                Icon(Icons.Filled.Add, stringResource(id = R.string.ipfsgw_add_add))
            }
        }
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.ipfsgw_explainer),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = ipfsEnabled, onCheckedChange = { checked ->
                        ipfsEnabled = checked
                        prefs.isIpfsEnabled = checked
                    })
                }
                DefaultGatewaysSettings(prefs = prefs, ipfsEnabled = ipfsEnabled)
                UserGatewaysSettings(prefs = prefs, ipfsEnabled = ipfsEnabled)
                // make sure FAB doesn't occlude the delete button of the last user gateway
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun DefaultGatewaysSettings(
    prefs: IPreferencesIpfs,
    ipfsEnabled: Boolean,
) {
    var disabledDefaultGateways by remember { mutableStateOf(prefs.ipfsGwDisabledDefaults) }

    Column {
        ComposeUtils.CaptionText(text = stringResource(id = R.string.ipfsgw_caption_official_gateways))
        for (gatewayUrl in Preferences.DEFAULT_IPFS_GATEWAYS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp, 4.dp, 0.dp, 4.dp)
            ) {
                Text(
                    text = gatewayUrl,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .alpha(if (ipfsEnabled) 1f else 0.5f)
                )
                Switch(
                    checked = !disabledDefaultGateways.contains(gatewayUrl),
                    onCheckedChange = { checked ->
                        val newList = disabledDefaultGateways.toMutableList()
                        if (!checked) {
                            newList.add(gatewayUrl)
                        } else {
                            newList.remove(gatewayUrl)
                        }
                        disabledDefaultGateways = newList
                        prefs.ipfsGwDisabledDefaults = newList
                    },
                    enabled = ipfsEnabled,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun UserGatewaysSettings(
    prefs: IPreferencesIpfs,
    ipfsEnabled: Boolean,
) {
    var userGateways by remember { mutableStateOf(prefs.ipfsGwUserList) }

    // Make sure list get updated when user returns from IpfsGatewayAddActivity
    LifecycleEventListener { _, event ->
        if (Lifecycle.Event.ON_RESUME == event) {
            userGateways = prefs.ipfsGwUserList
        }
    }

    Column {
        if (userGateways.isNotEmpty()) {
            ComposeUtils.CaptionText(text = stringResource(id = R.string.ipfsgw_caption_custom_gateways))
        }
        for (gatewayUrl in userGateways) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp, 4.dp, 0.dp, 4.dp)

            ) {
                Text(
                    text = gatewayUrl,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .alpha(if (ipfsEnabled) 1f else 0.5f)
                )
                IconButton(
                    onClick = {
                        val newGateways = userGateways.toMutableList()
                        newGateways.remove(gatewayUrl)

                        userGateways = newGateways
                        prefs.ipfsGwUserList = newGateways
                    }, enabled = ipfsEnabled, modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Localized description",
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun IpfsGatewaySettingsScreenPreview() {

    val prefs = object : IPreferencesIpfs {
        override fun isIpfsEnabled(): Boolean = true
        override fun setIpfsEnabled(enabled: Boolean) = throw NotImplementedError()
        override fun getIpfsGwUserList(): List<String> =
            listOf("https://my.imaginary.gateway/ifps/")

        override fun setIpfsGwUserList(selectedSet: List<String>?) = throw NotImplementedError()
        override fun getIpfsGwDisabledDefaults() = listOf("https://4everland.io/ipfs/")
        override fun setIpfsGwDisabledDefaults(selectedSet: List<String>?) =
            throw NotImplementedError()
    }

    FDroidContent {
        IpfsGatewaySettingsScreen(
            prefs = prefs,
            onBackClicked = {},
        )
    }
}