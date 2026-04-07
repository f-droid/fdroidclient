package org.fdroid.ui.navigation

import android.content.Context
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.ui.nearby.NearbyStart

fun getMoreMenuItems(context: Context): List<NavDestinations> =
  listOf(
    NavDestinations.AllApps(context.getString(R.string.app_list_all)),
    NavDestinations.Nearby,
    NavDestinations.About,
  )

fun EntryProviderScope<NavKey>.extraNavigationEntries(navigator: Navigator) {
  entry(NavigationKey.Nearby) { NearbyStart { navigator.goBack() } }
}
