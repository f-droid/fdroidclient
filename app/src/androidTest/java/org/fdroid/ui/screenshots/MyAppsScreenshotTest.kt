package org.fdroid.ui.screenshots

import androidx.core.os.LocaleListCompat
import org.fdroid.download.NetworkState
import org.fdroid.ui.apps.MyApps
import org.fdroid.ui.apps.MyAppsModel
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.getMyAppsInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MyAppsScreenshotTest(localeName: String) : LocalizedScreenshotTest(localeName) {

  companion object {
    @JvmStatic @Parameterized.Parameters(name = "{0}") fun locales() = locales
  }

  @Test
  fun myApps() =
    screenshotTest(
      "3_My_Apps",
      currentNavKey = NavigationKey.MyApps,
      numUpdates = getUpdates(LocaleListCompat.getDefault()).size,
      dark = true,
    ) { localeList ->
      val model =
        MyAppsModel(
          appUpdates = getUpdates(localeList),
          installingApps = emptyList(),
          installedApps = getInstalledApps(localeList),
          showAppIssueHint = false,
          networkState = NetworkState(isOnline = true, isMetered = false),
        )
      val info = getMyAppsInfo(model)
      MyApps(myAppsInfo = info, currentPackageName = null, onAppItemClick = {}, onNav = {})
    }
}
