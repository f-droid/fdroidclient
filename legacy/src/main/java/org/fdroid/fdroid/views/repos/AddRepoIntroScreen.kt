package org.fdroid.fdroid.views.repos

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.zxing.client.android.Intents.Scan.MIXED_SCAN
import com.google.zxing.client.android.Intents.Scan.SCAN_TYPE
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanOptions.QR_CODE
import kotlinx.coroutines.launch
import org.fdroid.fdroid.R
import org.fdroid.fdroid.compose.ComposeUtils.FDroidButton
import org.fdroid.fdroid.compose.ComposeUtils.FDroidOutlineButton
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.AddRepoState
import org.fdroid.repo.Added
import org.fdroid.repo.Adding
import org.fdroid.repo.FetchResult
import org.fdroid.repo.Fetching
import org.fdroid.repo.None

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepoIntroScreen(
    state: AddRepoState,
    onFetchRepo: (String) -> Unit,
    onAddRepo: () -> Unit,
    onBackClicked: () -> Unit,
) {
    val appBarTitle = if (state is Fetching) {
        when (state.fetchResult) {
            is FetchResult.IsNewMirror,
            is FetchResult.IsExistingMirror -> stringResource(R.string.repo_add_mirror)

            else -> stringResource(R.string.repo_add_new_title)
        }
    } else {
        stringResource(R.string.repo_add_new_title)
    }

    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            title = {
                Text(
                    text = appBarTitle,
                )
            },
        )
    }) { paddingValues ->
        when (state) {
            None -> AddRepoIntroContent(paddingValues, onFetchRepo)
            is Fetching -> {
                if (state.receivedRepo == null) {
                    RepoProgressScreen(paddingValues, stringResource(R.string.repo_state_fetching))
                } else {
                    RepoPreviewScreen(
                        state = state,
                        modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
                        onAddRepo = onAddRepo,
                    )
                }
            }

            Adding -> RepoProgressScreen(paddingValues, stringResource(R.string.repo_state_adding))
            is Added -> Box(modifier = Modifier.padding(paddingValues)) // empty UI
            is AddRepoError -> AddRepoErrorScreen(paddingValues, state)
        }
    }
}

@Composable
fun AddRepoIntroContent(paddingValues: PaddingValues, onFetchRepo: (String) -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        verticalArrangement = spacedBy(16.dp),
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .imePadding()
            .padding(paddingValues),
    ) {
        Text(
            text = stringResource(R.string.repo_intro),
            style = MaterialTheme.typography.bodyLarge,
        )
        val startForResult = rememberLauncherForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                onFetchRepo(result.contents)
            }
        }
        FDroidButton(
            stringResource(R.string.repo_scan_qr_code),
            imageVector = Icons.Filled.QrCode,
            onClick = {
                startForResult.launch(ScanOptions().apply {
                    setPrompt("")
                    setBeepEnabled(true)
                    setOrientationLocked(false)
                    setDesiredBarcodeFormats(QR_CODE)
                    addExtra(SCAN_TYPE, MIXED_SCAN)
                })
            },
        )
        val isPreview = LocalInspectionMode.current
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
fun AddRepoIntroScreenPreview() {
    FDroidContent(pureBlack = true) {
        AddRepoIntroScreen(None, {}, {}) {}
    }
}

@Composable
@Preview(uiMode = UI_MODE_NIGHT_YES, widthDp = 720, heightDp = 360)
fun AddRepoIntroScreenPreviewNight() {
    FDroidContent(pureBlack = true) {
        AddRepoIntroScreen(None, {}, {}) {}
    }
}
