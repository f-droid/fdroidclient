package org.fdroid.ui.apps

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.NoCompatibleSigner
import org.fdroid.download.NetworkState
import org.fdroid.install.InstallState
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.utils.getMyAppsInfo
import org.fdroid.ui.utils.getPreviewVersion

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun MyAppsOnlyInstalledAppsAndNoInternet() {
  ScreenshotTest(numUpdates = 0, hasAppIssues = false) {
    val model =
      MyAppsModel(
        appUpdates = emptyList(),
        installingApps = emptyList(),
        appsWithIssue = null,
        installedApps = getInstalledApps(),
        showAppIssueHint = false,
        sortOrder = AppListSortOrder.NAME,
        networkState = NetworkState(isOnline = true, isMetered = false),
      )
    MyApps(
      myAppsInfo = getMyAppsInfo(model),
      currentPackageName = null,
      onAppItemClick = {},
      onNav = {},
    )
  }
}

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun MyAppsInstalledAndIssues() {
  ScreenshotTest(numUpdates = 0, hasAppIssues = true) {
    val model =
      MyAppsModel(
        appUpdates = emptyList(),
        installingApps = emptyList(),
        appsWithIssue = getAppIssues(),
        installedApps = getInstalledApps(),
        showAppIssueHint = true,
        sortOrder = AppListSortOrder.NAME,
        networkState = NetworkState(isOnline = false, isMetered = false),
      )
    MyApps(
      myAppsInfo = getMyAppsInfo(model),
      currentPackageName = null,
      onAppItemClick = {},
      onNav = {},
    )
  }
}

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun MyAppsUpdatesOnlyAndNoInternet() {
  ScreenshotTest(numUpdates = getAppUpdates().size) {
    val model =
      MyAppsModel(
        appUpdates = getAppUpdates(),
        installingApps = emptyList(),
        appsWithIssue = null,
        installedApps = emptyList(),
        showAppIssueHint = false,
        sortOrder = AppListSortOrder.LAST_UPDATED,
        networkState = NetworkState(isOnline = false, isMetered = false),
      )
    MyApps(
      myAppsInfo = getMyAppsInfo(model),
      currentPackageName = null,
      onAppItemClick = {},
      onNav = {},
    )
  }
}

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun MyAppsFullList() {
  val installingApps =
    listOf(
      InstallingAppItem(
        packageName = "com.kunzisoft.keepass.libre",
        installState =
          InstallState.Installing(
            name = "KeePassDX Passkey Vault",
            versionName = "4.3.2",
            currentVersionName = null,
            lastUpdated = 1771544205000L,
            iconModel = null,
          ),
      ),
      InstallingAppItem(
        packageName = "com.inspiredandroid.linuxcommandbibliotheca",
        installState =
          InstallState.Installing(
            name = "Linux Command Library",
            versionName = "3.5.13",
            currentVersionName = "3.5.10",
            lastUpdated = 1772699310000L,
            iconModel = null,
          ),
      ),
    )
  ScreenshotTest(numUpdates = 1) {
    val model =
      MyAppsModel(
        appUpdates = getAppUpdates().take(1),
        installingApps = installingApps,
        appsWithIssue = getAppIssues(),
        installedApps = getInstalledApps(),
        showAppIssueHint = false,
        sortOrder = AppListSortOrder.NAME,
        networkState = NetworkState(isOnline = true, isMetered = false),
      )
    MyApps(
      myAppsInfo = getMyAppsInfo(model),
      currentPackageName = null,
      onAppItemClick = {},
      onNav = {},
    )
  }
}

private fun getAppUpdates() =
  listOf(
    AppUpdateItem(
      repoId = 1L,
      packageName = "app.organicmaps",
      name = "Organic Maps・Offline Map & GPS",
      installedVersionName = "2026.02.18-4-FDroid",
      update = getPreviewVersion(versionName = "2026.02.18-5-FDroid", size = 70355961L),
      whatsNew = "foo bar",
    ),
    AppUpdateItem(
      repoId = 1L,
      packageName = "at.bitfire.davdroid",
      name = "DAVx⁵",
      installedVersionName = "4.5.8-ose",
      update = getPreviewVersion(versionName = "4.5.9-ose", size = 15974669L),
      whatsNew = null,
    ),
  )

private fun getAppIssues() =
  listOf(
    AppWithIssueItem(
      packageName = "com.example.app1",
      name = "App with Issues and a very long name that may wrap to another line",
      installedVersionName = "1.0.0",
      installedVersionCode = 1,
      issue = NoCompatibleSigner(),
      lastUpdated = 1770000000L,
    )
  )

private fun getInstalledApps() =
  listOf(
    InstalledAppItem(
      packageName = "com.aurora.store",
      name = "Aurora Store",
      installedVersionName = "4.8.1",
      installedVersionCode = 1,
      lastUpdated = 1771544205000L,
    ),
    InstalledAppItem(
      packageName = "com.duckduckgo.mobile.android",
      name = "DuckDuckGo Privacy Browser",
      installedVersionName = "5.268.1",
      installedVersionCode = 1,
      lastUpdated = 1772699310000L,
    ),
    InstalledAppItem(
      packageName = "com.foobnix.pro.pdf.reader",
      name = "Librera Reader",
      installedVersionName = "9.3.63-fdroid",
      installedVersionCode = 1,
      lastUpdated = 1772574994000L,
    ),
  )
