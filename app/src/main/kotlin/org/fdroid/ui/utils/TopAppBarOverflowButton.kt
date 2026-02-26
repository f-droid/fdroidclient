package org.fdroid.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.fdroid.R

@Composable
fun TopAppBarOverflowButton(
    menuItems: @Composable (ColumnScope.(onDismissRequest: () -> Unit) -> Unit),
) {
    Box {
        var menuExpanded by remember { mutableStateOf(false) }
        TopAppBarButton(
            Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.more),
            onClick = { menuExpanded = !menuExpanded },
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            menuItems { menuExpanded = false }
        }
    }
}
