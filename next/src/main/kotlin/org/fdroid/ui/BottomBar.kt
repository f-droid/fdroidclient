package org.fdroid.ui

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey

@Composable
fun BottomBar(numUpdates: Int, currentNavKey: NavKey, onNav: (NavigationKey) -> Unit) {
    NavigationBar {
        BottomNavDestinations.entries.forEach { dest ->
            NavigationBarItem(
                icon = { NavIcon(dest, numUpdates) },
                label = { Text(stringResource(dest.label)) },
                selected = dest.key == currentNavKey,
                onClick = {
                    if (dest.key != currentNavKey) onNav(dest.key)
                },
            )
        }
    }
}

@Composable
fun NavigationRail(
    numUpdates: Int,
    currentNavKey: NavKey,
    onNav: (NavigationKey) -> Unit,
    modifier: Modifier,
) {
    NavigationRail(modifier) {
        BottomNavDestinations.entries.forEach { dest ->
            NavigationRailItem(
                icon = { NavIcon(dest, numUpdates) },
                label = { Text(stringResource(dest.label)) },
                selected = dest.key == currentNavKey,
                onClick = {
                    if (dest.key != currentNavKey) onNav(dest.key)
                },
            )
        }
    }
}

@Composable
private fun NavIcon(dest: BottomNavDestinations, numUpdates: Int) {
    BadgedBox(
        badge = {
            if (dest == BottomNavDestinations.MY_APPS && numUpdates > 0) {
                Badge {
                    Text(text = numUpdates.toString())
                }
            }
        }
    ) {
        Icon(
            dest.icon,
            contentDescription = stringResource(dest.label)
        )
    }
}
