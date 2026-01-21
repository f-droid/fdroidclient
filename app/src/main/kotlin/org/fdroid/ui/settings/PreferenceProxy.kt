package org.fdroid.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.textFieldPreference
import org.fdroid.R
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_PROXY
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.fdroid.ui.FDroidContent
import java.net.InetSocketAddress

fun LazyListScope.preferenceProxy(
    proxyState: MutableState<String>,
    showError: MutableState<Boolean>,
) {
    textFieldPreference(
        key = PREF_KEY_PROXY,
        defaultValue = PREF_DEFAULT_PROXY,
        rememberState = { proxyState },
        icon = {
            Icon(
                imageVector = Icons.Default.VpnLock,
                contentDescription = null,
                modifier = Modifier.semantics { hideFromAccessibility() },
            )
        },
        title = {
            Text(stringResource(R.string.pref_proxy_title))
        },
        summary = {
            val value = proxyState.value
            val s = if (value.isBlank()) {
                stringResource(R.string.pref_proxy_disabled)
            } else {
                stringResource(R.string.pref_proxy_enabled, value)
            }
            Text(s)
        },
        textToValue = {
            if (it.isBlank() || isProxyValid(it)) {
                showError.value = false
                it
            } else {
                showError.value = true
                // null is currently treated as an error and won't cause an update
                null
            }
        },
        textField = { value, onValueChange, onOk ->
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardActions = KeyboardActions { onOk() },
                singleLine = true,
                trailingIcon = {
                    if (value.text.isNotBlank()) {
                        IconButton(onClick = { onValueChange(TextFieldValue("")) }) {
                            Icon(Icons.Default.Clear, stringResource(R.string.clear))
                        }
                    }
                },
                isError = showError.value,
                supportingText = {
                    val s = if (showError.value) {
                        stringResource(R.string.pref_proxy_error)
                    } else {
                        stringResource(R.string.pref_proxy_hint)
                    }
                    Text(s)
                },
            )
        },
    )
}

private fun isProxyValid(proxyStr: String): Boolean = try {
    val (host, port) = proxyStr.split(':')
    InetSocketAddress.createUnresolved(host, port.toInt())
    true
} catch (_: Exception) {
    false
}

@Preview
@Composable
private fun PreviewDefault() {
    FDroidContent {
        ProvidePreferenceLocals {
            val showProxyError = remember { mutableStateOf(false) }
            val proxyState = remember { mutableStateOf(PREF_DEFAULT_PROXY) }
            LazyColumn {
                preferenceProxy(proxyState, showProxyError)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewProxySet() {
    FDroidContent {
        ProvidePreferenceLocals {
            val showProxyError = remember { mutableStateOf(false) }
            val proxyState = remember { mutableStateOf("127.0.0.1:8000") }
            LazyColumn {
                preferenceProxy(proxyState, showProxyError)
            }
        }
    }
}
