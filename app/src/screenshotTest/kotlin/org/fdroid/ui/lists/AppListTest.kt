package org.fdroid.ui.lists

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.utils.appListItems
import org.fdroid.ui.utils.getAppListInfo

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun AppListLoadingTest() =
  ScreenshotTest(showBottomBar = false) {
    AppList(
      appListInfo =
        getTestAppListInfo(
          apps = null,
          showFilterBadge = false,
          sortBy = AppListSortOrder.NAME,
          list = AppListType.New("Loading apps forever"),
        ),
      currentPackageName = appListItems[0].packageName,
      onBackClicked = {},
      onItemClick = {},
    )
  }

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun AppListTest() =
  ScreenshotTest(showBottomBar = false) {
    AppList(
      appListInfo =
        getTestAppListInfo(
          apps = appListItems,
          showFilterBadge = true,
          sortBy = AppListSortOrder.NAME,
          list = AppListType.New("New"),
        ),
      currentPackageName = appListItems[0].packageName,
      onBackClicked = {},
      onItemClick = {},
    )
  }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
fun AppListNightTest() =
  ScreenshotTest(showBottomBar = false) {
    AppList(
      appListInfo =
        getTestAppListInfo(
          apps = appListItems,
          showFilterBadge = false,
          sortBy = AppListSortOrder.LAST_UPDATED,
          list = AppListType.New("All apps"),
        ),
      currentPackageName = appListItems[2].packageName,
      onBackClicked = {},
      onItemClick = {},
    )
  }

private fun getTestAppListInfo(
  list: AppListType,
  apps: List<AppListItem>?,
  showFilterBadge: Boolean,
  sortBy: AppListSortOrder,
) =
  getAppListInfo(
    model =
      AppListModel(
        apps = apps,
        showFilterBadge = showFilterBadge,
        sortBy = sortBy,
        filterIncompatible = false,
        categories = null,
        filteredCategoryIds = emptySet(),
        antiFeatures = null,
        filteredAntiFeatureIds = emptySet(),
        repositories = emptyList(),
        filteredRepositoryIds = emptySet(),
      ),
    list = list,
  )
