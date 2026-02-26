package org.fdroid.ui.ipfs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.FDroidButton

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddGatewaysDialog(
    onAddUserGateway: (url: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val textState = remember { mutableStateOf(TextFieldValue()) }
    var errorMsg by remember { mutableStateOf("") }
    AlertDialog(
        title = {
            Text(text = stringResource(R.string.ipfsgw_add_title))
        },
        text = {
            Column {
                TextField(
                    value = textState.value,
                    minLines = 2,
                    onValueChange = { textState.value = it },
                    isError = errorMsg.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            FDroidButton(
                text = stringResource(R.string.ipfsgw_add_add),
                onClick = l@{
                    errorMsg = ""
                    val inputUri = if (textState.value.text.endsWith("/")) {
                        textState.value.text
                    } else {
                        "${textState.value.text}/"
                    }
                    try {
                        val uri = inputUri.toUri()
                        if (!setOf("http", "https").contains(uri.scheme)) {
                            errorMsg = "IPFS gateway URL should start with `https://`"
                            return@l
                        }
                    } catch (e: Exception) {
                        errorMsg = "could not parse uri ($e)"
                        return@l
                    }
                    // no errors -> proceed to add the url
                    onAddUserGateway(inputUri)
                    onDismissRequest()
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
@Preview
fun AddGatewaysScreenPreview() {
    FDroidContent {
        AddGatewaysDialog(
            onAddUserGateway = {},
            onDismissRequest = {},
        )
    }
}
