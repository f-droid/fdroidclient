package org.fdroid.repo

import android.content.Context
import android.text.format.Formatter
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mu.KotlinLogging
import org.fdroid.NotificationManager
import org.fdroid.R
import org.fdroid.database.Repository
import org.fdroid.index.IndexUpdateListener
import org.fdroid.index.IndexUpdateResult
import org.fdroid.ui.utils.addressForUi

private const val PROGRESS_THROTTLE_MILLIS = 1_000L

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
    val notificationMessage =
      context.getString(R.string.notification_repo_update_downloading, size, repo.addressForUi)
    notificationManager.showUpdateRepoNotification(
      msg = notificationMessage,
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
    if (shouldThrottleProgress(appsProcessed, totalApps)) return

    log.debug { "Committing ${repo.address} ($appsProcessed/$totalApps)" }

    val repositoryName = repo.getName(LocaleListCompat.getDefault())
    val notificationMessage =
      context.resources.getQuantityString(
        R.plurals.notification_repo_update_saving,
        appsProcessed,
        appsProcessed,
        repositoryName,
      )

    if (totalApps > 0) {
      showProgressWithTotal(repo, notificationMessage, appsProcessed, totalApps)
    } else {
      showProgressWithoutTotal(repo, notificationMessage)
    }
    lastUpdateProgress = System.currentTimeMillis()
  }

  fun onUpdateFinished(repoId: Long, result: IndexUpdateResult) {
    _updateState.update { RepoUpdateFinished(repoId, result) }
  }

  private fun shouldThrottleProgress(appsProcessed: Int, totalApps: Int): Boolean {
    val timeSinceLastProgress = System.currentTimeMillis() - lastUpdateProgress
    return timeSinceLastProgress < PROGRESS_THROTTLE_MILLIS && appsProcessed != totalApps
  }

  private fun showProgressWithTotal(
    repository: Repository,
    notificationMessage: String,
    appsProcessed: Int,
    totalApps: Int,
  ) {
    val percent = getPercent(appsProcessed.toLong(), totalApps.toLong())
    notificationManager.showUpdateRepoNotification(
      msg = notificationMessage,
      throttle = appsProcessed != totalApps,
      progress = percent,
    )
    _updateState.update { RepoUpdateProgress(repository.repoId, false, percent) }
  }

  private fun showProgressWithoutTotal(repository: Repository, notificationMessage: String) {
    notificationManager.showUpdateRepoNotification(notificationMessage)
    _updateState.update { RepoUpdateProgress(repository.repoId, false, 0f) }
  }

  private fun getPercent(current: Long, total: Long): Int {
    if (total <= 0) return 0
    return (100L * current / total).toInt()
  }
}
