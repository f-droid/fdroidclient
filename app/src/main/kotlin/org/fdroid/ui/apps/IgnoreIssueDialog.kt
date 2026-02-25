package org.fdroid.ui.apps

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.fdroid.R

@Composable
fun IgnoreIssueDialog(appName: String, onIgnore: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    title = { Text(text = stringResource(R.string.my_apps_ignore_dialog_title)) },
    text = { Text(text = stringResource(R.string.my_apps_ignore_dialog_text, appName)) },
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(onClick = onIgnore) {
        Text(
          text = stringResource(R.string.my_apps_ignore_dialog_button),
          color = MaterialTheme.colorScheme.error,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    },
  )
}
