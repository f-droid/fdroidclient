package org.fdroid.ui.utils

import android.text.format.Formatter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.fdroid.R

@Composable
fun MeteredConnectionDialog(numBytes: Long?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        title = {
            Text(text = stringResource(R.string.dialog_metered_title))
        },
        text = {
            val s = if (numBytes == null) {
                stringResource(R.string.dialog_metered_text_no_size)
            } else {
                val sizeStr = Formatter.formatFileSize(LocalContext.current, numBytes)
                stringResource(R.string.dialog_metered_text, sizeStr)
            }
            Text(text = s)
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) {
                Text(
                    text = stringResource(R.string.dialog_metered_button),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    MeteredConnectionDialog(9_999_999, {}, {})
}
