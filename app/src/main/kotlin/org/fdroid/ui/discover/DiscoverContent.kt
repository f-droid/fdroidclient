package org.fdroid.ui.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.ui.categories.CategoryList
import org.fdroid.ui.lists.AppListType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverContent(
  discoverModel: LoadedDiscoverModel,
  onListTap: (AppListType) -> Unit,
  onAppTap: (AppDiscoverItem) -> Unit,
  onNav: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    AnimatedVisibility(discoverModel.newApps.isNotEmpty()) {
      val listNew = AppListType.New(stringResource(R.string.app_list_new))
      AppCarousel(
        title = listNew.title,
        apps = discoverModel.newApps,
        onTitleTap = { onListTap(listNew) },
        onAppTap = onAppTap,
      )
    }
    AnimatedVisibility(!discoverModel.recentlyUpdatedApps.isEmpty()) {
      val listRecentlyUpdated =
        AppListType.RecentlyUpdated(stringResource(R.string.app_list_recently_updated))
      AppCarousel(
        title = listRecentlyUpdated.title,
        apps = discoverModel.recentlyUpdatedApps,
        onTitleTap = { onListTap(listRecentlyUpdated) },
        onAppTap = onAppTap,
      )
    }
    AnimatedVisibility(!discoverModel.mostDownloadedApps.isNullOrEmpty()) {
      val listMostDownloaded =
        AppListType.MostDownloaded(stringResource(R.string.app_list_most_downloaded))
      if (discoverModel.mostDownloadedApps != null)
        AppCarousel(
          title = listMostDownloaded.title,
          apps = discoverModel.mostDownloadedApps,
          onTitleTap = { onListTap(listMostDownloaded) },
          onAppTap = onAppTap,
        )
    }
    CategoryList(
      categoryMap = discoverModel.categories,
      onNav = onNav,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
