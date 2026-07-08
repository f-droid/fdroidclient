package org.fdroid.ui.navigation

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.ui.nearby.NearbyStart

fun getMoreMenuItems(context: Context): List<NavDestinations> =
  // enabling Nearby would require fixing org.apache.commons.io.FileUtils.copyFile
  listOfNotNull(
    NavDestinations.AllApps(context.getString(R.string.app_list_all)),
    if (SDK_INT >= 26) NavDestinations.Nearby else null,
    NavDestinations.About,
  )

fun EntryProviderScope<NavKey>.extraNavigationEntries(navigator: Navigator) {
  entry(NavigationKey.Nearby) { NearbyStart { navigator.goBack() } }
}
