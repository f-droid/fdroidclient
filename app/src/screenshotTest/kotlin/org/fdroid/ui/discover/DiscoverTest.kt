package org.fdroid.ui.discover

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateProgress
import org.fdroid.ui.ScreenshotTest
import org.fdroid.ui.categories.CategoryGroups
import org.fdroid.ui.categories.CategoryItem

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
fun DiscoverFirstStartTest() = ScreenshotTest {
  Discover(
    discoverModel =
      FirstStartDiscoverModel(
        networkState = NetworkState(isOnline = true, isMetered = false),
        repoUpdateState = RepoUpdateProgress(1, true, 0.25f),
      ),
    onListTap = {},
    onAppTap = {},
    onNav = {},
  )
}

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
private fun DiscoverNoEnabledReposTest() = ScreenshotTest {
  Discover(discoverModel = NoEnabledReposDiscoverModel, onListTap = {}, onAppTap = {}, onNav = {})
}

@Composable
@PreviewTest
@Preview(showBackground = true, showSystemUi = true)
private fun DiscoverTest() {
  ScreenshotTest {
    Discover(discoverModel = getLoadedModel(), onListTap = {}, onAppTap = {}, onNav = {})
  }
}

private fun getLoadedModel(): LoadedDiscoverModel {
  val newApps =
    listOf(
      AppDiscoverItem(
        packageName = "net.thunderbird.android",
        name = "Thunderbird: Free Your Inbox",
        isInstalled = true,
      ),
      AppDiscoverItem(
        packageName = "io.element.android.x",
        name = "Element X - Secure Chat & Call",
        isInstalled = false,
      ),
      AppDiscoverItem(
        packageName = "org.breezyweather",
        name = "Breezy Weather",
        isInstalled = true,
      ),
    )
  val recentlyUpdatedApps =
    listOf(
      AppDiscoverItem(packageName = "helium314.keyboard", name = "HeliBoard", isInstalled = true),
      AppDiscoverItem(
        packageName = "dev.imranr.obtainium.fdroid",
        name = "Obtainium",
        isInstalled = false,
      ),
      AppDiscoverItem(packageName = "com.fsck.k9", name = "K-9 Mail", isInstalled = false),
      AppDiscoverItem(
        packageName = "com.github.andreyasadchy.xtra",
        name = "Xtra",
        isInstalled = false,
      ),
      AppDiscoverItem(packageName = "com.github.libretube", name = "LibreTube", isInstalled = false),
    )
  val mostDownloadedApps =
    listOf(
      AppDiscoverItem(
        packageName = "com.inspiredandroid.linuxcommandbibliotheca",
        name = "Linux Command Library",
        isInstalled = false,
      ),
      AppDiscoverItem(packageName = "com.junkfood.seal", name = "Seal", isInstalled = false),
      AppDiscoverItem(
        packageName = "com.kunzisoft.keepass.libre",
        name = "KeePassDX Passkey Vault",
        isInstalled = false,
      ),
      AppDiscoverItem(
        packageName = "app.organicmaps",
        name = "Organic Maps・Offline Map & GPS",
        isInstalled = false,
      ),
      AppDiscoverItem(packageName = "at.bitfire.davdroid", name = "DAVx⁵", isInstalled = false),
    )
  val categories =
    mapOf(
      CategoryGroups.communication to
        listOf(
          CategoryItem(id = "Contact", name = "Contact"),
          CategoryItem(id = "Email", name = "Email"),
          CategoryItem(id = "Forum", name = "Forum"),
          CategoryItem(id = "Messaging", name = "Messaging"),
          CategoryItem(id = "Phone & SMS", name = "Phone & SMS"),
        )
    )
  return LoadedDiscoverModel(
    newApps = newApps,
    recentlyUpdatedApps = recentlyUpdatedApps,
    mostDownloadedApps = mostDownloadedApps,
    categories = categories,
    hasRepoIssues = true,
  )
}
