package org.fdroid.ui.screenshots

import org.fdroid.ui.discover.Discover
import org.fdroid.ui.discover.LoadedDiscoverModel
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DiscoverScreenshotTest(localeName: String) : LocalizedScreenshotTest(localeName) {

  companion object {
    @JvmStatic @Parameterized.Parameters(name = "{0}") fun locales() = locales
  }

  @Test
  fun appDetails() =
    screenshotTest("1_Discover") { localeList ->
      val model =
        LoadedDiscoverModel(
          newApps = getNewApps(localeList),
          recentlyUpdatedApps = getRecentlyUpdatedApps(localeList),
          mostDownloadedApps = getMostDownloadedApps(localeList),
          categories = getCategoryItems(localeList).groupBy { it.group },
          hasRepoIssues = false,
        )
      Discover(discoverModel = model, onListTap = {}, onAppTap = {}, onNav = {})
    }
}
