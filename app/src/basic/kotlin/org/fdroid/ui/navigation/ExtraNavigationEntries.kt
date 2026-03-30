package org.fdroid.ui.navigation

import android.content.Context
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.fdroid.R

fun getMoreMenuItems(context: Context): List<NavDestinations> =
  listOf(NavDestinations.AllApps(context.getString(R.string.app_list_all)), NavDestinations.About)

fun EntryProviderScope<NavKey>.extraNavigationEntries(navigator: Navigator) {}
