package org.fdroid.fdroid.views

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.R
import org.fdroid.fdroid.compose.ComposeUtils
import org.fdroid.fdroid.compose.ComposeUtils.FDroidContent

class IpfsGatewayAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FDroidContent {
                IpfsGatewayAddScreen(onBackClicked = { onBackPressedDispatcher.onBackPressed() },
                    onAddUserGateway = { url ->
                        // don't allow adding default gateways to the user gateways list
                        if (!Preferences.DEFAULT_IPFS_GATEWAYS.contains(url)) {
                            val updatedUserGwList = Preferences.get().ipfsGwUserList.toMutableList()
                            // don't allow double adding gateways
                            if (!updatedUserGwList.contains(url)) {
                                updatedUserGwList.add(url)
                                Preferences.get().ipfsGwUserList = updatedUserGwList
                            }
                        }
                        finish()
                    })
            }
        }
    }
}

@Composable
fun IpfsGatewayAddScreen(
    onBackClicked: () -> Unit,
    onAddUserGateway: (url: String) -> Unit,
) {
    val textState = remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(
            elevation = 4.dp,
            backgroundColor = MaterialTheme.colors.primarySurface,
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.ipfsgw_add_title),
                    modifier = Modifier.alpha(ContentAlpha.high),
                )
            },
        )
    }

    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Enter IPFS gateway URL",
                style = MaterialTheme.typography.body1,
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
                        style = MaterialTheme.typography.body1,
                        color = colorResource(
                            id = R.color.fdroid_error
                        )
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val clipboardManager = LocalClipboardManager.current
                ComposeUtils.FDroidOutlineButton(
                    stringResource(R.string.paste),
                    imageVector = Icons.Default.ContentPaste,
                    onClick = {
                        if (clipboardManager.hasText()) {
                            textState.value = TextFieldValue(clipboardManager.getText()?.text ?: "")
                        }
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                ComposeUtils.FDroidButton(
                    text = stringResource(R.string.ipfsgw_add_add),
                    onClick = l@{
                        errorMsg = ""
                        val inputUri =
                            if (textState.value.text.endsWith("/")) textState.value.text else "${textState.value.text}/"

                        try {
                            val uri = Uri.parse(inputUri)
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