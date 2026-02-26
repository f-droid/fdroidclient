package org.fdroid.ui.details

import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UpdateDisabled
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.startActivitySafe
import org.fdroid.ui.utils.testApp

@Composable
fun AppDetailsMenu(item: AppDetailsItem, onDismiss: () -> Unit) {
  val res = LocalResources.current
  val context = LocalContext.current
  val uninstallLauncher =
    rememberLauncherForActivityResult(StartActivityForResult()) {
      item.actions.onUninstallResult(it)
    }
  if (item.appPrefs != null)
    DropdownMenuItem(
      leadingIcon = { Icon(Icons.Default.Preview, null) },
      text = { Text(stringResource(R.string.menu_release_channel_beta)) },
      trailingIcon = {
        Checkbox(
          checked = item.allowsBetaVersions,
          onCheckedChange = null,
          enabled = !item.ignoresAllUpdates,
        )
      },
      enabled = !item.ignoresAllUpdates,
      onClick = {
        item.actions.allowBetaVersions()
        onDismiss()
      },
    )
  if (item.actions.ignoreAllUpdates != null)
    DropdownMenuItem(
      leadingIcon = { Icon(Icons.Default.UpdateDisabled, null) },
      text = { Text(stringResource(R.string.menu_ignore_all)) },
      trailingIcon = { Checkbox(item.ignoresAllUpdates, null) },
      onClick = {
        item.actions.ignoreAllUpdates()
        onDismiss()
      },
    )
  if (item.actions.ignoreThisUpdate != null)
    DropdownMenuItem(
      leadingIcon = { Icon(Icons.Default.UpdateDisabled, null) },
      text = { Text(stringResource(R.string.menu_ignore_this)) },
      trailingIcon = {
        Checkbox(
          checked = item.ignoresCurrentUpdate,
          onCheckedChange = null,
          enabled = !item.ignoresAllUpdates,
        )
      },
      enabled = !item.ignoresAllUpdates,
      onClick = {
        item.actions.ignoreThisUpdate()
        onDismiss()
      },
    )
  if (item.actions.shareApk != null)
    DropdownMenuItem(
      leadingIcon = { Icon(Icons.Default.Share, null) },
      text = { Text(stringResource(R.string.menu_share_apk)) },
      onClick = {
        val s = res.getString(R.string.menu_share_apk)
        val i = Intent.createChooser(item.actions.shareApk, s)
        context.startActivitySafe(i)
        onDismiss()
      },
    )
  if (item.actions.uninstallIntent != null)
    DropdownMenuItem(
      leadingIcon = { Icon(Icons.Default.Delete, null) },
      text = { Text(stringResource(R.string.menu_uninstall)) },
      onClick = {
        uninstallLauncher.launch(item.actions.uninstallIntent)
        onDismiss()
      },
    )
}

@Preview
@Composable
fun AppDetailsMenuPreview() {
  FDroidContent { Column { AppDetailsMenu(testApp) {} } }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun AppDetailsMenuAllIgnoredPreview() {
  val appPrefs = testApp.appPrefs!!.toggleIgnoreAllUpdates()
  FDroidContent { Column { AppDetailsMenu(testApp.copy(appPrefs = appPrefs)) {} } }
}
