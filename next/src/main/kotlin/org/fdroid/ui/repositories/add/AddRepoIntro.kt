package org.fdroid.ui.repositories.add

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.zxing.client.android.Intents.Scan.MIXED_SCAN
import com.google.zxing.client.android.Intents.Scan.SCAN_TYPE
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanOptions.QR_CODE
import kotlinx.coroutines.launch
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.repo.None
import org.fdroid.ui.utils.FDroidButton
import org.fdroid.ui.utils.FDroidOutlineButton

@Composable
fun AddRepoIntroContent(onFetchRepo: (String) -> Unit, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var showPermissionWarning by remember { mutableStateOf(isPreview) }
    val startForResult = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            onFetchRepo(result.contents)
        }
    }

    fun startScanning() {
        startForResult.launch(ScanOptions().apply {
            setPrompt("")
            setBeepEnabled(true)
            setOrientationLocked(false)
            setDesiredBarcodeFormats(QR_CODE)
            addExtra(SCAN_TYPE, MIXED_SCAN)
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = RequestPermission()
    ) { isGranted: Boolean ->
        showPermissionWarning = !isGranted
        if (isGranted) startScanning()
    }
    Column(
        verticalArrangement = spacedBy(16.dp),
        horizontalAlignment = CenterHorizontally,
        modifier = modifier
            .imePadding()
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.repo_intro),
            style = MaterialTheme.typography.bodyLarge,
        )
        FDroidButton(
            stringResource(R.string.repo_scan_qr_code),
            imageVector = Icons.Filled.QrCode,
            onClick = {
                if (checkSelfPermission(context, CAMERA) == PERMISSION_GRANTED) {
                    startScanning()
                } else {
                    permissionLauncher.launch(CAMERA)
                }
            },
        )
        AnimatedVisibility(showPermissionWarning) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.permission_camera_denied),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        var manualExpanded by rememberSaveable { mutableStateOf(isPreview) }
        Row(
            horizontalArrangement = SpaceBetween,
            verticalAlignment = CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MinHeight)
                .clickable { manualExpanded = !manualExpanded },
        ) {
            Text(
                text = stringResource(R.string.repo_enter_url),
                style = MaterialTheme.typography.bodyMedium,
                // avoid occupying the whole row
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (manualExpanded) {
                    Icons.Default.ArrowDropUp
                } else {
                    Icons.Default.ArrowDropDown
                },
                contentDescription = null,
            )
        }
        val textState = remember { mutableStateOf(TextFieldValue()) }
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()
        AnimatedVisibility(visible = manualExpanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = spacedBy(16.dp),
            ) {
                TextField(
                    value = textState.value,
                    minLines = 2,
                    onValueChange = { textState.value = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            onFetchRepo(textState.value.text)
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned {
                            focusRequester.requestFocus()
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                )
                Row(
                    horizontalArrangement = spacedBy(16.dp),
                    verticalAlignment = CenterVertically,
                ) {
                    val clipboardManager = LocalClipboardManager.current
                    FDroidOutlineButton(
                        stringResource(id = R.string.paste),
                        imageVector = Icons.Default.ContentPaste,
                        onClick = {
                            if (clipboardManager.hasText()) {
                                textState.value =
                                    TextFieldValue(clipboardManager.getText()?.text ?: "")
                            }
                        },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    FDroidButton(
                        text = stringResource(R.string.repo_add_add),
                        onClick = { onFetchRepo(textState.value.text) },
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    FDroidContent {
        AddRepo(None, {}, {}, {}, { _, _ -> }) {}
    }
}

@Composable
@Preview(uiMode = UI_MODE_NIGHT_YES, widthDp = 720, heightDp = 360)
private fun PreviewNight() {
    FDroidContent {
        AddRepo(None, {}, {}, {}, { _, _ -> }) {}
    }
}
