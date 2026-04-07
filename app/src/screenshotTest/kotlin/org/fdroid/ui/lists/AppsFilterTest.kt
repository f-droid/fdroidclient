package org.fdroid.ui.lists

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.utils.getAppListInfo
import org.fdroid.ui.utils.getAppListModel

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun AppsFilterTest() =
  ScreenshotTest(showBottomBar = false) { AppsFilter(info = getAppListInfo(getAppListModel())) }

@Composable
@PreviewTest
@Preview(
  showBackground = true,
  showSystemUi = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
fun AppsFilterNightTest() =
  ScreenshotTest(showBottomBar = false) {
    AppsFilter(
      info =
        getAppListInfo(
          getAppListModel(
            sortBy = AppListSortOrder.LAST_UPDATED,
            filterIncompatible = false,
            filteredCategoryIds = emptySet(),
            filteredAntiFeatureIds = emptySet(),
            filteredRepositoryIds = emptySet(),
          )
        )
    )
  }
