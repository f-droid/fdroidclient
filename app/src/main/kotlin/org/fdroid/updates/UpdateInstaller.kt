package org.fdroid.updates

import android.content.Context
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.AppVersion
import org.fdroid.database.FDroidDatabase
import org.fdroid.index.RepoManager
import org.fdroid.install.AppInstallManager
import org.fdroid.ui.apps.AppUpdateItem
import org.fdroid.utils.IoDispatcher

/**
 * Encapsulates installation orchestration for app updates.
 *
 * Responsibilities:
 * * decide pre-approval behavior for batch updates
 * * update other apps in parallel with bounded concurrency
 * * defer updating the client app itself until the end
 */
@Singleton
class UpdateInstaller
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val db: FDroidDatabase,
  private val repoManager: RepoManager,
  private val appInstallManager: AppInstallManager,
  @param:IoDispatcher private val coroutineScope: CoroutineScope,
) {
  private val log = KotlinLogging.logger {}

  companion object {
    private const val UNKNOWN_APP_NAME = "Unknown"
    private const val MAX_CONCURRENT_UPDATES = 8
  }

  /**
   * Applies all provided updates.
   *
   * @param appsToUpdate the update items to install
   * @param canAskPreApprovalNow whether pre-approval may be requested now
   */
  suspend fun updateAll(appsToUpdate: List<AppUpdateItem>, canAskPreApprovalNow: Boolean) {
    if (appsToUpdate.isEmpty()) return
    val canRequestPreApproval = canAskPreApprovalNow && appsToUpdate.size == 1

    // separate the update for our own app (if present) from the rest,
    // so we can defer it until the end and do all other updates in parallel first
    val (ownAppList, otherApps) = appsToUpdate.partition { it.packageName == context.packageName }
    // set our own app to "waiting for install" state, so the UI can reflect that immediately
    val ownApp = ownAppList.firstOrNull()
    if (ownApp != null) setOwnAppWaitingState(ownApp)

    // Update all non-self apps first, then our own package at the end.
    updateAppsInParallel(otherApps, canRequestPreApproval)

    ownApp?.let { update -> updateApp(update, canRequestPreApproval) }
  }

  private suspend fun updateAppsInParallel(
    apps: List<AppUpdateItem>,
    canRequestPreApproval: Boolean,
  ) {
    val concurrencyLimit = min(Runtime.getRuntime().availableProcessors(), MAX_CONCURRENT_UPDATES)
    val semaphore = Semaphore(concurrencyLimit)
    var num = 0
    log.info { "Updating ${apps.size} apps with concurrency limit $concurrencyLimit" }
    apps
      .map { update ->
        coroutineScope.launch {
          // acquire a permit for the update, so we don't run too many in parallel
          // FIXME a job is considered completed when we need to ask for user approval,
          //  so as the user approves updates one by one, more updates will start,
          //  which may lead to too many concurrent updates.
          //  In practice, no adverse effects were observed even with ~50 updates,
          //  so maybe not high priority.
          semaphore.withPermit {
            num += 1
            log.info { "Starting update for ${update.packageName} (${num}/${apps.size})" }
            currentCoroutineContext().ensureActive()
            updateApp(update, canRequestPreApproval)
          }
        }
      }
      .joinAll()

    currentCoroutineContext().ensureActive()
  }

  private fun setOwnAppWaitingState(update: AppUpdateItem) {
    val app = db.getAppDao().getApp(update.repoId, update.packageName)
    appInstallManager.setWaitingState(
      packageName = update.packageName,
      name = app?.metadata?.name.getBestLocale(LocaleListCompat.getDefault()) ?: UNKNOWN_APP_NAME,
      versionName = update.update.versionName,
      currentVersionName = update.installedVersionName,
      lastUpdated = update.update.added,
    )
  }

  private suspend fun updateApp(update: AppUpdateItem, canAskPreApprovalNow: Boolean) {
    val app = db.getAppDao().getApp(update.repoId, update.packageName)
    appInstallManager.install(
        packageName = update.packageName,
        appMetadata = app?.metadata,
        version = update.update as AppVersion,
        currentVersionName = update.installedVersionName,
        repo = repoManager.getRepository(update.repoId),
        iconModel = update.iconModel,
        canAskPreApprovalNow = canAskPreApprovalNow,
    )
  }
}
