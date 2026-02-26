package org.fdroid.ui.lists

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.Navigator

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.appListEntry(navigator: Navigator, isBigScreen: Boolean) {
  entry<NavigationKey.AppList>(metadata = ListDetailSceneStrategy.listPane("appdetails")) {
    val viewModel =
      hiltViewModel<AppListViewModel, AppListViewModel.Factory>(
        creationCallback = { factory -> factory.create(it.type) }
      )
    val appListInfo =
      object : AppListInfo {
        override val model = viewModel.appListModel.collectAsStateWithLifecycle().value
        override val list: AppListType = it.type
        override val actions: AppListActions = viewModel
        override val showFilters: Boolean =
          viewModel.showFilters.collectAsStateWithLifecycle().value
        override val showOnboarding: Boolean =
          viewModel.showOnboarding.collectAsStateWithLifecycle().value
      }
    AppList(
      appListInfo = appListInfo,
      currentPackageName =
        if (isBigScreen) {
          (navigator.last as? NavigationKey.AppDetails)?.packageName
        } else null,
      onBackClicked = { navigator.goBack() },
    ) { packageName ->
      val new = NavigationKey.AppDetails(packageName)
      if (navigator.last is NavigationKey.AppDetails) {
        navigator.replaceLast(new)
      } else {
        navigator.navigate(new)
      }
    }
  }
}
