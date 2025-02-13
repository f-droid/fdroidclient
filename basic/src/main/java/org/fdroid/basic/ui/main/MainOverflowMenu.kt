package org.fdroid.basic.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.fdroid.basic.R
import org.fdroid.basic.ui.icons.PackageVariant

@Composable
fun MainOverFlowMenu(menuExpanded: Boolean, onDismissRequest: () -> Unit) {
    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.app_details_repositories)) },
            onClick = { },
            leadingIcon = {
                Icon(
                    PackageVariant,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_settings)) },
            onClick = { },
            leadingIcon = {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("About") },
            onClick = { },
            leadingIcon = {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null
                )
            }
        )
    }
}
