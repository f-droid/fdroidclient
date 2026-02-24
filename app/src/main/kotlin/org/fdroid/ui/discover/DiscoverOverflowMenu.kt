package org.fdroid.ui.discover

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import org.fdroid.ui.navigation.NavDestinations
import org.fdroid.ui.navigation.getMoreMenuItems

@Composable
fun DiscoverOverFlowMenu(
    onItemClicked: (NavDestinations) -> Unit,
) {
    getMoreMenuItems(LocalContext.current).forEach { dest ->
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
