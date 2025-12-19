package org.fdroid.ui.discover

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import org.fdroid.ui.navigation.NavDestinations
import org.fdroid.ui.navigation.moreMenuItems

@Composable
fun DiscoverOverFlowMenu(
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
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = null,
                        modifier = Modifier.semantics { hideFromAccessibility() },
                    )
                }
            )
        }
    }
}
