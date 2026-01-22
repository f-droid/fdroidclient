package org.fdroid.ui.crash

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.utils.getLogName

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Crash(
    isOldCrash: Boolean,
    onCancel: () -> Unit,
    onSend: (String, String) -> Unit,
    onSave: (Uri, String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val res = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val textFieldState = rememberTextFieldState()
    val snackbarHostState = remember { SnackbarHostState() }
    val launcher = rememberLauncherForActivityResult(CreateDocument("application/json")) {
        val success = it != null && onSave(it, textFieldState.text.toString())
        val msg = if (success) res.getString(R.string.crash_report_saved)
        else res.getString(R.string.crash_report_error_saving)
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = { launcher.launch("${getLogName(context)}.json") }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.crash_report_save),
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        CrashContent(isOldCrash, onCancel, onSend, textFieldState, Modifier.padding(paddingValues))
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        Crash(false, {}, { _, _ -> }, { _, _ -> true })
    }
}
