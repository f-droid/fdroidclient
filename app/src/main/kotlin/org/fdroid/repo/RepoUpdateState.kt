package org.fdroid.repo

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import org.fdroid.index.IndexUpdateResult

private const val DOWNLOAD_PHASE_WEIGHT = 0.5f
private const val DB_PHASE_START = 0.5f

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

  // Progress is split in two phases: download (0-50%) then DB save (50-100%).
  val progress: Float
    get() =
      if (isDownloading) {
        stepProgress * DOWNLOAD_PHASE_WEIGHT
      } else {
        DB_PHASE_START + stepProgress * DOWNLOAD_PHASE_WEIGHT
      }
}

data class RepoUpdateFinished(override val repoId: Long, val result: IndexUpdateResult) :
  RepoUpdateState
