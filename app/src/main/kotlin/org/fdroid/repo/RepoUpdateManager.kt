package org.fdroid.repo

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.fdroid.CompatibilityChecker
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.NotificationManager
import org.fdroid.R
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.download.DownloaderFactory
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.RepoManager
import org.fdroid.index.RepoUpdater
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager

private const val MIN_UPDATE_INTERVAL_MILLIS = 15_000

@Singleton
class RepoUpdateManager
@VisibleForTesting
internal constructor(
  private val context: Context,
  private val db: FDroidDatabase,
  private val repoManager: RepoManager,
  private val updatesManager: UpdatesManager,
  private val settingsManager: SettingsManager,
  private val downloaderFactory: DownloaderFactory,
  private val notificationManager: NotificationManager,
  // Defaults keep the testing constructor lightweight while still mirroring production wiring.
  private val compatibilityChecker: CompatibilityChecker =
    CompatibilityCheckerImpl(packageManager = context.packageManager, forceTouchApps = false),
  private val repoUpdateListener: RepoUpdateListener =
    RepoUpdateListener(context, notificationManager),
  private val repoUpdater: RepoUpdater =
    RepoUpdater(
      tempDir = context.cacheDir, // FIXME this causes disk I/O
      db = db,
      downloaderFactory = downloaderFactory,
      compatibilityChecker = compatibilityChecker,
      listener = repoUpdateListener,
    ),
) {

  @Inject
  constructor(
    @ApplicationContext context: Context,
    db: FDroidDatabase,
    repositoryManager: RepoManager,
    updatesManager: UpdatesManager,
    settingsManager: SettingsManager,
    downloaderFactory: DownloaderFactory,
    notificationManager: NotificationManager,
  ) : this(
    context = context,
    db = db,
    repoManager = repositoryManager,
    updatesManager = updatesManager,
    settingsManager = settingsManager,
    downloaderFactory = downloaderFactory,
    notificationManager = notificationManager,
  )

  private val log = KotlinLogging.logger {}
  private val _isUpdating = MutableStateFlow(false)
  val isUpdating = _isUpdating.asStateFlow()
  val repoUpdateState = repoUpdateListener.updateState

  /**
   * The time in milliseconds of the (earliest!) next automatic repo update check. This is
   * [Long.MAX_VALUE], if no time is known.
   */
  val nextUpdateFlow =
    RepoUpdateWorker.getAutoUpdateWorkInfo(context).map { workInfo ->
      workInfo?.nextScheduleTimeMillis ?: Long.MAX_VALUE
    }

  /**
   * Updates all enabled repositories.
   *
   * The call is skipped when an update is already running or when the last update happened very
   * recently. When at least one repository is processed, app updates are loaded and notifications
   * are refreshed.
   */
  @WorkerThread
  suspend fun updateRepos() {
    currentCoroutineContext().ensureActive()
    if (shouldSkipUpdate()) return

    _isUpdating.value = true
    try {
      currentCoroutineContext().ensureActive()
      var anyReposProcessed = false
      // Always get repos fresh from DB, because:
      // * when an update is requested early at app start, the repos might not be available yet
      // * when an update is requested when adding a new repo, it might not be in the list yet
      db.getRepositoryDao().getRepositories().forEach { repository ->
        if (!repository.enabled) return@forEach // don't update disabled repos

        currentCoroutineContext().ensureActive()
        val result = updateRepositoryInternal(repository)
        if (result is IndexUpdateResult.Processed) {
          anyReposProcessed = true
        }
      }

      db.getRepositoryDao().walCheckpoint()
      updateLastCheckTimestamp(anyReposProcessed)

      if (anyReposProcessed) {
        checkAndNotifyAppUpdates()
      }
    } finally {
      notificationManager.cancelUpdateRepoNotification()
      _isUpdating.value = false
    }
  }

  /**
   * Updates a single repository by id.
   *
   * Returns [IndexUpdateResult.NotFound] when no repository exists for [repoId].
   */
  @WorkerThread
  fun updateRepo(repoId: Long): IndexUpdateResult {
    if (isUpdating.value) log.warn { "Already updating repositories: updateRepo($repoId)" }

    val repository = repoManager.getRepository(repoId) ?: return IndexUpdateResult.NotFound
    _isUpdating.value = true
    return try {
      val result = updateRepositoryInternal(repository)
      if (result is IndexUpdateResult.Processed) {
        updatesManager.loadUpdates()
      }
      result
    } finally {
      notificationManager.cancelUpdateRepoNotification()
      _isUpdating.value = false
      db.getRepositoryDao().walCheckpoint()
    }
  }

  private fun shouldSkipUpdate(): Boolean {
    if (isUpdating.value) {
      // This is a workaround for what looks like a WorkManager bug.
      // Sometimes it goes through scheduling/cancellation loops
      // and then ends up running the same worker more than once.
      log.warn { "Already updating repositories, skipping duplicate call" }
      return true
    }
    val timeSinceLastCheck = System.currentTimeMillis() - settingsManager.lastRepoUpdate
    if (timeSinceLastCheck < MIN_UPDATE_INTERVAL_MILLIS) {
      // This is a workaround for a similar issue as above.
      // We've seen WorkManager tell our worker to run in what looks like an endless loop.
      log.info { "Not updating, only $timeSinceLastCheck ms since last check." }
      return true
    }
    return false
  }

  private fun updateRepositoryInternal(repository: Repository): IndexUpdateResult {
    repoUpdateListener.onUpdateStarted(repository.repoId)
    showRepoUpdateNotification(repository)

    val result = repoUpdater.update(repository)
    log.info { "Update repo result: $result" }

    repoUpdateListener.onUpdateFinished(repository.repoId, result)

    if (result is IndexUpdateResult.Error) {
      log.error(result.e) { "Error updating repository ${repository.address}" }
    }
    return result
  }

  private fun showRepoUpdateNotification(repository: Repository) {
    val repositoryName = repository.getName(LocaleListCompat.getDefault())
    val notificationMessage =
      context.getString(R.string.notification_repo_update_default, repositoryName)
    notificationManager.showUpdateRepoNotification(notificationMessage, throttle = false)
  }

  private fun updateLastCheckTimestamp(anyReposProcessed: Boolean) {
    // Don't update time on first start when repos failed to update
    if (!settingsManager.isFirstStart || anyReposProcessed) {
      settingsManager.lastRepoUpdate = System.currentTimeMillis()
    }
  }

  private suspend fun checkAndNotifyAppUpdates() {
    updatesManager.loadUpdates().join() // wait here until we have finished loading updates
    val numUpdates = updatesManager.numUpdates.value
    if (numUpdates > 0) {
      val states = updatesManager.notificationStates
      notificationManager.showAppUpdatesAvailableNotification(states)
    }
  }
}
