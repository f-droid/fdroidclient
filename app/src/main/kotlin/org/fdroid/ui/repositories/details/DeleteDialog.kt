package org.fdroid.ui.repositories.details

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.fdroid.R

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun DeleteDialog(onDismissDialog: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        title = {
            Text(text = stringResource(R.string.repo_confirm_delete_title))
        },
        text = {
            Text(text = stringResource(R.string.repo_confirm_delete_body))
        },
        onDismissRequest = onDismissDialog,
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissDialog) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
