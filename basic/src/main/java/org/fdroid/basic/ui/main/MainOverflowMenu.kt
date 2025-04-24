package org.fdroid.basic.ui.main

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun MainOverFlowMenu(
    menuExpanded: Boolean,
    onItemClicked: (NavDestinations) -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = onDismissRequest
    ) {
        moreMenuItems.forEach { dest ->
            DropdownMenuItem(
                text = { Text(stringResource(dest.label)) },
                onClick = { onItemClicked(dest) },
                leadingIcon = {
                    Icon(dest.icon, contentDescription = null)
                }
            )
        }
    }
}
