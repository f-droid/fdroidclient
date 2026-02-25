package org.fdroid.repo

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
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
import kotlinx.coroutines.flow.update
import mu.KotlinLogging
import org.fdroid.CompatibilityChecker
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.NotificationManager
import org.fdroid.R
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.download.DownloaderFactory
import org.fdroid.index.IndexUpdateListener
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.RepoManager
import org.fdroid.index.RepoUpdater
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.utils.addressForUi
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

  @WorkerThread
  suspend fun updateRepos() {
    currentCoroutineContext().ensureActive()
    if (isUpdating.value) {
      // This is a workaround for what looks like a WorkManager bug.
      // Sometimes it goes through scheduling/cancellation loops
      // and then ends up running the same worker more than once.
      log.warn { "Already updating repositories in updateRepos() not doing it again." }
      return
    }
    val timeSinceLastCheck = System.currentTimeMillis() - settingsManager.lastRepoUpdate
    if (timeSinceLastCheck < MIN_UPDATE_INTERVAL_MILLIS) {
      // This is a workaround for a similar issue as above.
      // We've seen WorkManager tell our worker to run in what looks like an endless loop.
      log.info { "Not updating, only $timeSinceLastCheck ms since last check." }
      return
    }
    _isUpdating.value = true
    try {
      currentCoroutineContext().ensureActive()
      var reposUpdated = false
      // always get repos fresh from DB, because
      // * when an update is requested early at app start,
      //   the repos above might not be available, yet
      // * when an update is requested when adding a new repo,
      //   it might not be in the FDroidApp list, yet
      db.getRepositoryDao().getRepositories().forEach { repo ->
        if (!repo.enabled) return@forEach
        currentCoroutineContext().ensureActive()

        repoUpdateListener.onUpdateStarted(repo.repoId)
        // show notification
        val repoName = repo.getName(LocaleListCompat.getDefault())
        val msg = context.getString(R.string.notification_repo_update_default, repoName)
        notificationManager.showUpdateRepoNotification(msg, throttle = false)
        // update repo
        val result = repoUpdater.update(repo)
        log.info { "Update repo result: $result" }
        repoUpdateListener.onUpdateFinished(repo.repoId, result)
        if (result is IndexUpdateResult.Processed) reposUpdated = true
        else if (result is IndexUpdateResult.Error) {
          log.error(result.e) { "Error updating repository ${repo.address} " }
        }
      }
      db.getRepositoryDao().walCheckpoint()
      // don't update time on first start when repos failed to update
      if (!settingsManager.isFirstStart || reposUpdated) {
        settingsManager.lastRepoUpdate = System.currentTimeMillis()
      }
      if (reposUpdated) {
        updatesManager.loadUpdates().join()
        val numUpdates = updatesManager.numUpdates.value
        if (numUpdates > 0) {
          val states = updatesManager.notificationStates
          notificationManager.showAppUpdatesAvailableNotification(states)
        }
      }
    } finally {
      notificationManager.cancelUpdateRepoNotification()
      _isUpdating.value = false
    }
  }

  @WorkerThread
  fun updateRepo(repoId: Long): IndexUpdateResult {
    if (isUpdating.value) log.warn { "Already updating repositories: updateRepo($repoId)" }

    val repo = repoManager.getRepository(repoId) ?: return IndexUpdateResult.NotFound
    _isUpdating.value = true
    return try {
      repoUpdateListener.onUpdateStarted(repo.repoId)
      // show notification
      val repoName = repo.getName(LocaleListCompat.getDefault())
      val msg = context.getString(R.string.notification_repo_update_default, repoName)
      notificationManager.showUpdateRepoNotification(msg, throttle = false)
      // update repo
      val result = repoUpdater.update(repo)
      log.info { "Update repo result: $result" }
      repoUpdateListener.onUpdateFinished(repo.repoId, result)
      if (result is IndexUpdateResult.Processed) {
        updatesManager.loadUpdates()
      } else if (result is IndexUpdateResult.Error) {
        log.error(result.e) { "Error updating ${repo.address}: " }
      }
      result
    } finally {
      notificationManager.cancelUpdateRepoNotification()
      _isUpdating.value = false
      db.getRepositoryDao().walCheckpoint()
    }
  }
}

@VisibleForTesting
internal class RepoUpdateListener(
  private val context: Context,
  private val notificationManager: NotificationManager,
) : IndexUpdateListener {

  private val log = KotlinLogging.logger {}
  private val _updateState = MutableStateFlow<RepoUpdateState?>(null)
  val updateState = _updateState.asStateFlow()
  private var lastUpdateProgress = 0L

  fun onUpdateStarted(repoId: Long) {
    _updateState.update { RepoUpdateProgress(repoId, true, 0) }
  }

  override fun onDownloadProgress(repo: Repository, bytesRead: Long, totalBytes: Long) {
    log.debug { "Downloading ${repo.address} ($bytesRead/$totalBytes)" }

    val percent = getPercent(bytesRead, totalBytes)
    val size = Formatter.formatFileSize(context, bytesRead)
    notificationManager.showUpdateRepoNotification(
      msg =
        context.getString(R.string.notification_repo_update_downloading, size, repo.addressForUi),
      throttle = bytesRead != totalBytes,
      progress = percent,
    )
    _updateState.update { RepoUpdateProgress(repo.repoId, true, percent) }
  }

  /**
   * If an updater is unable to know how many apps it has to process (i.e. it is streaming apps to
   * the database or performing a large database query which touches all apps, but is unable to
   * report progress), then it call this listener with [totalApps] = 0. Doing so will result in a
   * message of "Saving app details" sent to the user. If you know how many apps you have processed,
   * then a message of "Saving app details (x/total)" is displayed.
   */
  override fun onUpdateProgress(repo: Repository, appsProcessed: Int, totalApps: Int) {
    // Don't update progress, if we already have updated once within the last second
    if (System.currentTimeMillis() - lastUpdateProgress < 1000 && appsProcessed != totalApps) {
      return
    }
    log.debug { "Committing ${repo.address} ($appsProcessed/$totalApps)" }

    val repoName = repo.getName(LocaleListCompat.getDefault())
    val msg =
      context.resources.getQuantityString(
        R.plurals.notification_repo_update_saving,
        appsProcessed,
        appsProcessed,
        repoName,
      )
    if (totalApps > 0) {
      val percent = getPercent(appsProcessed.toLong(), totalApps.toLong())
      notificationManager.showUpdateRepoNotification(
        msg = msg,
        throttle = appsProcessed != totalApps,
        progress = percent,
      )
      _updateState.update { RepoUpdateProgress(repo.repoId, false, percent) }
    } else {
      notificationManager.showUpdateRepoNotification(msg)
      _updateState.update { RepoUpdateProgress(repo.repoId, false, 0f) }
    }
    lastUpdateProgress = System.currentTimeMillis()
  }

  fun onUpdateFinished(repoId: Long, result: IndexUpdateResult) {
    _updateState.update { RepoUpdateFinished(repoId, result) }
  }

  private fun getPercent(current: Long, total: Long): Int {
    if (total <= 0) return 0
    return (100L * current / total).toInt()
  }
}

sealed interface RepoUpdateState {
  val repoId: Long
}

/**
 * There's two types of progress. First, there's the download, so [isDownloading] is true. Then
 * there's inserting the repo data into the DB, there [isDownloading] is false. The [stepProgress]
 * gets re-used for both.
 *
 * An external unified view on that is given as [progress].
 */
data class RepoUpdateProgress(
  override val repoId: Long,
  private val isDownloading: Boolean,
  @param:FloatRange(from = 0.0, to = 1.0) private val stepProgress: Float,
) : RepoUpdateState {
  constructor(
    repoId: Long,
    isDownloading: Boolean,
    @IntRange(from = 0, to = 100) percent: Int,
  ) : this(repoId = repoId, isDownloading = isDownloading, stepProgress = percent.toFloat() / 100)

  val progress: Float = if (isDownloading) stepProgress / 2 else 0.5f + stepProgress / 2
}

data class RepoUpdateFinished(override val repoId: Long, val result: IndexUpdateResult) :
  RepoUpdateState
