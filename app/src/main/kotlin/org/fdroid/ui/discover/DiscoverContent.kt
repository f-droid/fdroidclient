package org.fdroid.ui.discover

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.ui.categories.CategoryList
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.search.AppsSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverContent(
    discoverModel: LoadedDiscoverModel,
    onListTap: (AppListType) -> Unit,
    onAppTap: (AppDiscoverItem) -> Unit,
    onNav: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    // workaround for https://issuetracker.google.com/issues/445720462)
    Column(modifier = modifier.focusable()) {
        AppsSearch(
            onNav = onNav,
            textFieldState = discoverModel.searchTextFieldState,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 4.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
        )
        if (discoverModel.newApps.isNotEmpty()) {
            val listNew = AppListType.New(stringResource(R.string.app_list_new))
            AppCarousel(
                title = listNew.title,
                apps = discoverModel.newApps,
                onTitleTap = { onListTap(listNew) },
                onAppTap = onAppTap,
            )
        }
        val listRecentlyUpdated = AppListType.RecentlyUpdated(
            stringResource(R.string.app_list_recently_updated),
        )
        AppCarousel(
            title = listRecentlyUpdated.title,
            apps = discoverModel.recentlyUpdatedApps,
            onTitleTap = { onListTap(listRecentlyUpdated) },
            onAppTap = onAppTap,
        )
        if (!discoverModel.mostDownloadedApps.isNullOrEmpty()) {
            val listMostDownloaded = AppListType.MostDownloaded(
                stringResource(R.string.app_list_most_downloaded),
            )
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
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}
