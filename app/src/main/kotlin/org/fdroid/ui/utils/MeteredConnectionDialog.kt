package org.fdroid.ui.utils

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R

@Composable
fun MeteredConnectionDialog(
  numBytes: Long?,
  onConfirm: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var dontShowAgain by remember { mutableStateOf(false) }
  AlertDialog(
    title = { Text(text = stringResource(R.string.dialog_metered_title)) },
    text = {
      val s =
        if (numBytes == null) {
          stringResource(R.string.dialog_metered_text_no_size)
        } else {
          val sizeStr = Formatter.formatFileSize(LocalContext.current, numBytes)
          stringResource(R.string.dialog_metered_text, sizeStr)
        }
      Column {
        Text(text = s)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 16.dp),
        ) {
          Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
          Text(
            text = stringResource(R.string.dialog_metered_dont_show),
            modifier = Modifier.padding(start = 8.dp),
          )
        }
      }
    },
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          onDismiss()
          onConfirm(dontShowAgain)
        },
      ) {
        Text(
          text = stringResource(R.string.dialog_metered_button),
          color = MaterialTheme.colorScheme.error,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    },
  )
}

@Preview
@Composable
private fun Preview() {
  MeteredConnectionDialog(9_999_999, {}, {})
}
