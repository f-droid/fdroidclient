package org.fdroid.updates

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.engine.ProxyConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.NotificationManager
import org.fdroid.database.AppWithIssue
import org.fdroid.database.AvailableAppWithIssue
import org.fdroid.database.DbAppChecker
import org.fdroid.database.UnavailableAppWithIssue
import org.fdroid.database.UpdatableApp
import org.fdroid.index.RepoManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.apps.AppUpdateItem
import org.fdroid.ui.apps.AppWithIssueItem
import org.fdroid.utils.IoDispatcher

@Singleton
class UpdatesManager
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val dbAppChecker: DbAppChecker,
  private val settingsManager: SettingsManager,
  private val repoManager: RepoManager,
  private val installedAppsCache: InstalledAppsCache,
  private val notificationManager: NotificationManager,
  private val updateInstaller: UpdateInstaller,
  @param:IoDispatcher private val coroutineScope: CoroutineScope,
) {
  private val log = KotlinLogging.logger {}

  private val _updates = MutableStateFlow<List<AppUpdateItem>?>(null)
  val updates = _updates.asStateFlow()
  private val _appsWithIssues = MutableStateFlow<List<AppWithIssueItem>?>(null)
  val appsWithIssues = _appsWithIssues.asStateFlow()
  private val _numUpdates = MutableStateFlow(0)
  val numUpdates = _numUpdates.asStateFlow()

  /**
   * The time in milliseconds of the (earliest!) next repository update run. This is
   * [Long.MAX_VALUE], if no time is known.
   */
  val nextRepoUpdateFlow =
    RepoUpdateWorker.getAutoUpdateWorkInfo(context).map { workInfo ->
      workInfo?.nextScheduleTimeMillis ?: Long.MAX_VALUE
    }

  /**
   * The time in milliseconds of the (earliest!) next automatic app update run. This is
   * [Long.MAX_VALUE], if no time is known.
   */
  val nextAppUpdateFlow =
    AppUpdateWorker.getAutoUpdateWorkInfo(context).map { workInfo ->
      workInfo?.nextScheduleTimeMillis ?: Long.MAX_VALUE
    }

  val notificationStates: UpdateNotificationState
    get() = UpdateNotificationState(updates = updates.value.orEmpty().map { it.toAppUpdate() })

  init {
    coroutineScope.launch {
      // delay initial check for updates a bit, so we don't hammer the DB during start-up
      delay(1500)
      // Auto-refresh updates when installed apps change.
      installedAppsCache.installedApps.collect { loadUpdates(it) }
    }
  }

  /** Loads available updates and app issues for the given [packageInfoMap]. */
  fun loadUpdates(
    packageInfoMap: Map<String, PackageInfo> = installedAppsCache.installedApps.value
  ) = coroutineScope.launch {
    if (packageInfoMap.isEmpty()) return@launch
    val localeList = LocaleListCompat.getDefault()
    try {
      log.info { "Checking for updates (${packageInfoMap.size} apps)..." }
      val proxyConfig = settingsManager.proxyConfig
      val (updateCheckResult, duration) =
        measureTimedValue { dbAppChecker.getApps(packageInfoMap = packageInfoMap) }
      log.debug { "Checking for updates took $duration" }
      processAvailableUpdates(updateCheckResult.updates, localeList, proxyConfig)
      processAppIssues(updateCheckResult.issues, localeList, proxyConfig)
    } catch (e: Exception) {
      log.error(e) { "Error loading updates" }
    }
  }

  private fun processAvailableUpdates(
    updates: List<UpdatableApp>,
    localeList: LocaleListCompat,
    proxyConfig: ProxyConfig?,
  ) {
    val updateItems = updates.map { update ->
      update.toAppUpdateItem(localeList, proxyConfig, repoManager)
    }
    _updates.value = updateItems
    _numUpdates.value = updateItems.size
    updateNotificationIfShowing(updateItems)
  }

  private fun updateNotificationIfShowing(updates: List<AppUpdateItem>) {
    if (!notificationManager.isAppUpdatesAvailableNotificationShowing) return

    when {
      updates.isEmpty() -> notificationManager.cancelAppUpdatesAvailableNotification()
      else -> notificationManager.showAppUpdatesAvailableNotification(notificationStates)
    }
  }

  private fun processAppIssues(
    issues: List<AppWithIssue>,
    localeList: LocaleListCompat,
    proxyConfig: ProxyConfig?,
  ) {
    // Don't flag issues too early when we are still at first start
    if (settingsManager.isFirstStart) return

    _appsWithIssues.value =
      issues
        .filterNot { it.packageName in settingsManager.ignoredAppIssues }
        .map { issue ->
          when (issue) {
            is AvailableAppWithIssue ->
              issue.toAppWithIssueItem(localeList, proxyConfig, repoManager)
            is UnavailableAppWithIssue -> issue.toAppWithIssueItem()
          }
        }
  }

  /**
   * Apply all available updates. If [canAskPreApprovalNow] is true, the user can be asked for
   * pre-approval for the update if needed. This should only be true if the user explicitly
   * triggered the update process, e.g. by clicking "Update all" in the UI. For automatic updates,
   * this should be false, to avoid interrupting the update process with pre-approval dialogs.
   */
  suspend fun updateAll(canAskPreApprovalNow: Boolean) {
    val appsToUpdate = updates.value ?: updates.firstOrNull() ?: return
    updateInstaller.updateAll(appsToUpdate, canAskPreApprovalNow)
  }
}
