package org.fdroid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import org.fdroid.R

@Composable
fun BottomBar(
    numUpdates: Int,
    hasIssues: Boolean,
    currentNavKey: NavKey,
    onNav: (NavigationKey) -> Unit,
) {
    NavigationBar {
        BottomNavDestinations.entries.forEach { dest ->
            NavigationBarItem(
                icon = { NavIcon(dest, numUpdates, hasIssues) },
                label = { Text(stringResource(dest.label)) },
                selected = dest.key == currentNavKey,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = contentColorFor(MaterialTheme.colorScheme.primary),
                ),
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
    hasIssues: Boolean,
    currentNavKey: NavKey,
    onNav: (NavigationKey) -> Unit,
    modifier: Modifier,
) {
    NavigationRail(modifier) {
        BottomNavDestinations.entries.forEach { dest ->
            NavigationRailItem(
                icon = { NavIcon(dest, numUpdates, hasIssues) },
                label = { Text(stringResource(dest.label)) },
                selected = dest.key == currentNavKey,
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = contentColorFor(MaterialTheme.colorScheme.primary),
                ),
                onClick = {
                    if (dest.key != currentNavKey) onNav(dest.key)
                },
            )
        }
    }
}

@Composable
private fun NavIcon(dest: BottomNavDestinations, numUpdates: Int, hasIssues: Boolean) {
    BadgedBox(
        badge = {
            if (dest == BottomNavDestinations.MY_APPS && numUpdates > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                    Text(text = numUpdates.toString())
                }
            } else if (dest == BottomNavDestinations.MY_APPS && hasIssues) {
                Icon(
                    imageVector = Icons.Default.Error,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = stringResource(R.string.my_apps_header_apps_with_issue)
                )
            }
        }
    ) {
        Icon(
            dest.icon,
            contentDescription = stringResource(dest.label)
        )
    }
}
