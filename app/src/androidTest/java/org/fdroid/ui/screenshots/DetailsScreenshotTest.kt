package org.fdroid.ui.screenshots

import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallState
import org.fdroid.ui.details.AppDetails
import org.fdroid.ui.details.AppDetailsItem
import org.fdroid.ui.details.VersionItem
import org.fdroid.ui.utils.getAppDetailsActions
import org.fdroid.ui.utils.testVersion1
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DetailsScreenshotTest(localeName: String) : LocalizedScreenshotTest(localeName) {

  companion object {
    @JvmStatic @Parameterized.Parameters(name = "{0}") fun locales() = locales
  }

  @Test
  fun appDetails() =
    screenshotTest("4_Details", showBottomBar = false, dark = true) { localeList ->
      val item =
        AppDetailsItem(
          app = appMetadata,
          actions = getAppDetailsActions(),
          installState = InstallState.Unknown,
          networkState = NetworkState(isOnline = true, isMetered = false),
          name = appMetadata.name.getBestLocale(localeList) ?: "Unknown name",
          summary = appMetadata.summary.getBestLocale(localeList),
          description = appMetadata.description.getBestLocale(localeList),
          icon = appDetailsIcon,
          featureGraphic = appDetailsFeatureGraphic,
          phoneScreenshots = appDetailsScreenshots.getBestLocale(localeList) ?: emptyList(),
          versions =
            listOf(
              VersionItem(
                testVersion1,
                isInstalled = false,
                isSuggested = true,
                isCompatible = true,
                isSignerCompatible = true,
                showInstallButton = true,
              )
            ),
        )
      AppDetails(item = item, onNav = {}, onBackNav = {})
    }
}
