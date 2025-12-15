package org.fdroid.ui.apps

import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.startActivitySafe

@Composable
fun NotAvailableDialog(packageName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.app_issue_not_available_title)) },
        text = {
            Column(verticalArrangement = spacedBy(8.dp)) {
                Text(text = stringResource(R.string.app_issue_not_available_text))
                OutlinedButton(
                    onClick = {
                        val intent: Intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            setData(Uri.fromParts("package", packageName, null))
                        }
                        context.startActivitySafe(intent)
                    },
                    modifier = Modifier.align(CenterHorizontally)
                ) {
                    Text(stringResource(R.string.app_issue_not_available_button))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) { Text(stringResource(R.string.ok)) }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        Box(modifier = Modifier.fillMaxSize()) {
            NotAvailableDialog("foo.bar") {}
        }
    }
}
