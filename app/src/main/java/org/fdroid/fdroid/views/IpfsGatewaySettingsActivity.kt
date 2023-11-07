package org.fdroid.fdroid.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.IPreferencesIpfs
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.R
import org.fdroid.fdroid.compose.ComposeUtils.FDroidContent

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
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            DefaultGateWaysSettings(prefs = prefs)
        }
    }
}

@Composable
fun DefaultGateWaysSettings(
    prefs: IPreferencesIpfs,
) {

    var ipfsEnabled by remember { mutableStateOf(prefs.isIpfsEnabled) }
    var disabledDefaultGateways by remember { mutableStateOf(prefs.ipfsGwDisabledDefaults) }

    Column() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.ipfsgw_explainer),
                modifier = Modifier.weight(1f)
            )
            Switch(checked = ipfsEnabled, onCheckedChange = { checked ->
                ipfsEnabled = checked
                prefs.isIpfsEnabled = checked
            })
        }
        Preferences.DEFAULT_IPFS_GATEWAYS.forEach() { gatewayUrl ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(64.dp, 4.dp, 16.dp, 4.dp)
            ) {
                Text(text = gatewayUrl, modifier = Modifier.weight(1f))
                Switch(
                    checked = !disabledDefaultGateways.contains(gatewayUrl),
                    onCheckedChange = { checked ->
                        val newSet = disabledDefaultGateways.toMutableSet()
                        if (!checked) {
                            newSet.add(gatewayUrl)
                        } else {
                            newSet.remove(gatewayUrl)
                        }
                        disabledDefaultGateways = newSet
                        prefs.ipfsGwDisabledDefaults = disabledDefaultGateways
                    },
                    enabled = ipfsEnabled
                )
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
        override fun getIpfsGwUserList(): Set<String> = throw NotImplementedError()
        override fun setIpfsGwUserList(selectedSet: Set<String>?) = throw NotImplementedError()
        override fun getIpfsGwDisabledDefaults(): Set<String> {
            return setOf("https://4everland.io/ipfs/")
        }

        override fun setIpfsGwDisabledDefaults(selectedSet: Set<String>?) =
            throw NotImplementedError()
    }

    FDroidContent {
        IpfsGatewaySettingsScreen(
            prefs = prefs,
            onBackClicked = {},
        )
    }
}