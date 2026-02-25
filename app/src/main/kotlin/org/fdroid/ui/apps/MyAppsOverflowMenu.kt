package org.fdroid.ui.apps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import org.fdroid.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyAppsOverFlowMenu(
  onInstallHistory: () -> Unit,
  onExportInstalledApps: () -> Unit,
  onDismissRequest: () -> Unit,
) {
  DropdownMenuItem(
    text = { Text(stringResource(R.string.install_history)) },
    onClick = {
      onInstallHistory()
      onDismissRequest()
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.History,
        contentDescription = null,
        modifier = Modifier.semantics { hideFromAccessibility() },
      )
    },
  )
  DropdownMenuItem(
    text = { Text(stringResource(R.string.my_apps_export_installed_apps)) },
    onClick = {
      onExportInstalledApps()
      onDismissRequest()
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Filled.UploadFile,
        contentDescription = null,
        modifier = Modifier.semantics { hideFromAccessibility() },
      )
    },
  )
}
