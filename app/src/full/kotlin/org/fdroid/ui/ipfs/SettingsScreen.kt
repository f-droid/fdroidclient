package org.fdroid.ui.ipfs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.ipfs.IpfsManager.Companion.DEFAULT_GATEWAYS

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(prefs: IpfsPreferences, actions: IpfsActions, onBackClicked: () -> Unit) {
  var showAddDialog by remember { mutableStateOf(false) }
  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBackClicked) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
          }
        },
        title = { Text(text = stringResource(R.string.ipfsgw_title)) },
      )
    },
    floatingActionButton = {
      // it doesn't seam to be supported to disable FABs, so just hide it for now.
      if (prefs.isIpfsEnabled) {
        FloatingActionButton(onClick = { showAddDialog = true }) {
          Icon(Icons.Filled.Add, stringResource(id = R.string.ipfsgw_add_add))
        }
      }
    },
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).verticalScroll(rememberScrollState())) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = stringResource(id = R.string.ipfsgw_explainer),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
          )
          Switch(checked = prefs.isIpfsEnabled, onCheckedChange = actions::setIpfsEnabled)
        }
        DefaultGatewaysSettings(prefs, actions)
        UserGatewaysSettings(prefs, actions)
        // make sure FAB doesn't occlude the delete button of the last user gateway
        Spacer(modifier = Modifier.height(64.dp))
      }
    }
    if (showAddDialog) AddGatewaysDialog(actions::addUserGateway) { showAddDialog = false }
  }
}

@Composable
fun DefaultGatewaysSettings(prefs: IpfsPreferences, actions: IpfsActions) {
  Column {
    Text(
      text = stringResource(id = R.string.ipfsgw_caption_official_gateways),
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 4.dp),
    )
    for (gatewayUrl in DEFAULT_GATEWAYS) {
      Row(modifier = Modifier.fillMaxWidth().padding(48.dp, 4.dp, 0.dp, 4.dp)) {
        Text(
          text = gatewayUrl,
          style = MaterialTheme.typography.bodyLarge,
          modifier =
            Modifier.weight(1f)
              .align(Alignment.CenterVertically)
              .alpha(if (prefs.isIpfsEnabled) 1f else 0.5f),
        )
        Switch(
          checked = !prefs.disabledDefaultGateways.contains(gatewayUrl),
          onCheckedChange = { checked -> actions.setDefaultGatewayEnabled(gatewayUrl, checked) },
          enabled = prefs.isIpfsEnabled,
          modifier = Modifier.align(Alignment.CenterVertically),
        )
      }
    }
  }
}

@Composable
fun UserGatewaysSettings(prefs: IpfsPreferences, actions: IpfsActions) {
  Column {
    if (prefs.userGateways.isNotEmpty()) {
      Text(
        text = stringResource(id = R.string.ipfsgw_caption_custom_gateways),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 4.dp),
      )
    }
    for (gatewayUrl in prefs.userGateways) {
      Row(modifier = Modifier.fillMaxWidth().padding(48.dp, 4.dp, 0.dp, 4.dp)) {
        Text(
          text = gatewayUrl,
          style = MaterialTheme.typography.bodyLarge,
          modifier =
            Modifier.weight(1f)
              .align(Alignment.CenterVertically)
              .alpha(if (prefs.isIpfsEnabled) 1f else 0.5f),
        )
        IconButton(
          onClick = { actions.removeUserGateway(gatewayUrl) },
          enabled = prefs.isIpfsEnabled,
          modifier = Modifier.align(Alignment.CenterVertically),
        ) {
          Icon(Icons.Default.DeleteForever, contentDescription = "Localized description")
        }
      }
    }
  }
}

@Composable
@Preview
fun SettingsScreenPreview() {
  FDroidContent {
    SettingsScreen(
      prefs =
        IpfsPreferences(
          isIpfsEnabled = true,
          disabledDefaultGateways = listOf("https://4everland.io/ipfs/"),
          userGateways = listOf("https://my.imaginary.gateway/ifps/"),
        ),
      actions =
        object : IpfsActions {
          override fun setIpfsEnabled(enabled: Boolean) {}

          override fun setDefaultGatewayEnabled(url: String, enabled: Boolean) {}

          override fun addUserGateway(url: String) {}

          override fun removeUserGateway(url: String) {}
        },
      onBackClicked = {},
    )
  }
}
