package org.fdroid.ui.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun History(
    items: List<HistoryItem>?,
    enabled: Boolean?,
    onEnabled: (Boolean) -> Unit,
    onDeleteAll: () -> Unit,
    onBackClicked: (() -> Unit)?,
) {
    var deleteAllDialogShown by remember { mutableStateOf(false) }
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBackClicked != null) IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = {
                    Text(stringResource(R.string.install_history))
                },
                actions = {
                    if (!items.isNullOrEmpty()) IconButton(
                        onClick = { deleteAllDialogShown = true },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription =
                            stringResource(R.string.install_history_delete_ally),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        if (items == null) BigLoadingIndicator(modifier = Modifier.padding(paddingValues))
        else HistoryList(items, enabled, onEnabled, paddingValues)
        val onDismiss = { deleteAllDialogShown = false }
        if (deleteAllDialogShown) AlertDialog(
            title = {
                Text(text = stringResource(R.string.install_history_delete_text))
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAll()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
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
}

@Preview
@Composable
private fun PreviewLoading() {
    FDroidContent {
        History(null, true, {}, {}) { }
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    FDroidContent {
        History(emptyList(), true, {}, {}) { }
    }
}

@Preview
@Composable
private fun PreviewEmptyDisabled() {
    FDroidContent {
        History(emptyList(), false, {}, {}) { }
    }
}
