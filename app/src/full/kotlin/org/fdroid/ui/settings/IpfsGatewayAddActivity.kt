package org.fdroid.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.settings.IpfsManager.Companion.DEFAULT_GATEWAYS
import org.fdroid.ui.utils.FDroidButton
import org.fdroid.ui.utils.FDroidOutlineButton
import javax.inject.Inject

@AndroidEntryPoint
class IpfsGatewayAddActivity : AppCompatActivity() {

    @Inject
    lateinit var manager: IpfsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FDroidContent {
                IpfsGatewayAddScreen(
                    onBackClicked = { onBackPressedDispatcher.onBackPressed() },
                    onAddUserGateway = { url ->
                        // don't allow adding default gateways to the user gateways list
                        if (!DEFAULT_GATEWAYS.contains(url)) {
                            val updatedUserGwList = manager.ipfsGwUserList.toMutableList()
                            // don't allow double adding gateways
                            if (!updatedUserGwList.contains(url)) {
                                updatedUserGwList.add(url)
                                manager.ipfsGwUserList = updatedUserGwList
                            }
                        }
                        finish()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpfsGatewayAddScreen(
    onBackClicked: () -> Unit,
    onAddUserGateway: (url: String) -> Unit,
) {
    val textState = remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.ipfsgw_add_title),
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Enter IPFS gateway URL",
                style = MaterialTheme.typography.bodyLarge,
            )
            Column {
                TextField(
                    value = textState.value,
                    minLines = 2,
                    onValueChange = { textState.value = it },
                    isError = errorMsg.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned {
                            focusRequester.requestFocus()
                        },
                )
                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val clipboardManager = LocalClipboardManager.current
                FDroidOutlineButton(
                    stringResource(R.string.paste),
                    imageVector = Icons.Default.ContentPaste,
                    onClick = {
                        if (clipboardManager.hasText()) {
                            textState.value = TextFieldValue(clipboardManager.getText()?.text ?: "")
                        }
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
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
                    },
                )
            }
        }
    }
}

@Composable
@Preview
fun IpfsGatewayAddScreenPreview() {
    FDroidContent {
        IpfsGatewayAddScreen(
            onBackClicked = {},
            onAddUserGateway = {},
        )
    }
}
