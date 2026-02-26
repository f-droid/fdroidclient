package org.fdroid.ui.discover

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.fdroid.ui.details.NoAppSelected
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.Navigator

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.discoverEntry(navigator: Navigator) {
  entry<NavigationKey.Discover>(
    metadata = ListDetailSceneStrategy.listPane("appdetails") { NoAppSelected() }
  ) {
    val viewModel = hiltViewModel<DiscoverViewModel>()
    Discover(
      discoverModel = viewModel.discoverModel.collectAsStateWithLifecycle().value,
      onListTap = { navigator.navigate(NavigationKey.AppList(it)) },
      onAppTap = {
        val new = NavigationKey.AppDetails(it.packageName)
        if (navigator.last is NavigationKey.AppDetails) {
          navigator.replaceLast(new)
        } else {
          navigator.navigate(new)
        }
      },
      onNav = { navKey -> navigator.navigate(navKey) },
    )
  }
}
