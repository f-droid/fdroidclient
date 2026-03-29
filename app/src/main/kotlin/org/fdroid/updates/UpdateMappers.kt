package org.fdroid.updates

import androidx.core.os.LocaleListCompat
import io.ktor.client.engine.ProxyConfig
import org.fdroid.database.AvailableAppWithIssue
import org.fdroid.database.UnavailableAppWithIssue
import org.fdroid.database.UpdatableApp
import org.fdroid.download.DownloadRequest
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.ui.apps.AppUpdateItem
import org.fdroid.ui.apps.AppWithIssueItem

private const val UNKNOWN_APP_NAME = "Unknown app"
private const val UNAVAILABLE_APP_LAST_UPDATED = -1L

/** Transforms a [UpdatableApp] from the database into an [AppUpdateItem] for the UI layer. */
internal fun UpdatableApp.toAppUpdateItem(
  localeList: LocaleListCompat,
  proxyConfig: ProxyConfig?,
  repoManager: RepoManager,
): AppUpdateItem {
  val iconDownloadRequest =
    repoManager.getRepository(repoId)?.let { repo ->
      getIcon(localeList)?.getImageModel(repo, proxyConfig)
    } as? DownloadRequest

  return AppUpdateItem(
    repoId = repoId,
    packageName = packageName,
    name = name ?: UNKNOWN_APP_NAME,
    installedVersionName = installedVersionName,
    update = update,
    whatsNew = update.getWhatsNew(localeList)?.trim(),
    iconModel = PackageName(packageName, iconDownloadRequest),
  )
}

/** Transforms an [AvailableAppWithIssue] into an [AppWithIssueItem] for the UI layer. */
internal fun AvailableAppWithIssue.toAppWithIssueItem(
  localeList: LocaleListCompat,
  proxyConfig: ProxyConfig?,
  repoManager: RepoManager,
): AppWithIssueItem {
  val iconDownloadRequest =
    repoManager.getRepository(app.repoId)?.let { repo ->
      app.getIcon(localeList)?.getImageModel(repo, proxyConfig)
    } as? DownloadRequest

  return AppWithIssueItem(
    packageName = app.packageName,
    name = app.getName(localeList) ?: UNKNOWN_APP_NAME,
    installedVersionName = installVersionName,
    installedVersionCode = installVersionCode,
    issue = issue,
    lastUpdated = app.lastUpdated,
    iconModel = PackageName(app.packageName, iconDownloadRequest),
  )
}

/** Transforms an [UnavailableAppWithIssue] into an [AppWithIssueItem] for the UI layer. */
internal fun UnavailableAppWithIssue.toAppWithIssueItem(): AppWithIssueItem {
  return AppWithIssueItem(
    packageName = packageName,
    name = name.toString(),
    installedVersionName = installVersionName,
    installedVersionCode = installVersionCode,
    issue = issue,
    lastUpdated = UNAVAILABLE_APP_LAST_UPDATED,
    iconModel = PackageName(packageName, null),
  )
}

/** Transforms an [AppUpdateItem] into an [AppUpdate] for notification purposes. */
internal fun AppUpdateItem.toAppUpdate(): AppUpdate {
  return AppUpdate(
    packageName = packageName,
    name = name,
    currentVersionName = installedVersionName,
    updateVersionName = update.versionName,
  )
}
