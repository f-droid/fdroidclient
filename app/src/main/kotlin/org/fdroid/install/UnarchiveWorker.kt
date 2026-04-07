package org.fdroid.install

import android.content.Context
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ALL_USERS
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ID
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asFlow
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import mu.KotlinLogging
import org.fdroid.database.DbUpdateChecker
import org.fdroid.database.FDroidDatabase
import org.fdroid.index.RepoManager

@HiltWorker
@RequiresApi(35)
class UnarchiveWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val db: FDroidDatabase,
  private val repoManager: RepoManager,
  private val dbUpdateChecker: DbUpdateChecker,
  private val appInstallManager: AppInstallManager,
) : CoroutineWorker(appContext, workerParams) {

  private val log = KotlinLogging.logger {}

  companion object {
    @UiThread
    fun updateNow(context: Context, packageName: String, unarchiveId: Int, allUsers: Boolean) {
      val data =
        Data.Builder()
          .putString(EXTRA_UNARCHIVE_PACKAGE_NAME, packageName)
          .putInt(EXTRA_UNARCHIVE_ID, unarchiveId)
          .putBoolean(EXTRA_UNARCHIVE_ALL_USERS, allUsers)
          .build()
      val request =
        OneTimeWorkRequestBuilder<UnarchiveWorker>()
          .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
          .setInputData(data)
          .build()
      WorkManager.getInstance(context).enqueue(request)
    }
  }

  override suspend fun doWork(): Result {
    val packageName = inputData.getString(EXTRA_UNARCHIVE_PACKAGE_NAME) ?: error("No packageName")
    // TODO actually use in the two values below
    val unarchiveId = inputData.getInt(EXTRA_UNARCHIVE_ID, -1)
    val allUsers = inputData.getBoolean(EXTRA_UNARCHIVE_ALL_USERS, false)

    log.info { "Unarchiving $packageName with unarchiveId $unarchiveId for allUsers=$allUsers" }

    // find suggested version for that app
    val appPrefs = db.getAppPrefsDao().getAppPrefs(packageName).asFlow().first()
    val version =
      dbUpdateChecker.getSuggestedVersion(
        packageName = packageName,
        // TODO we could try to get the old signer (if still available) and search for the same
        preferredSigner = null,
        releaseChannels = appPrefs.releaseChannels,
        onlyFromPreferredRepo = true,
      )

    // install version, if available
    return if (version == null) {
      log.error { "Could not find a version to unarchive for $packageName" }
      Result.failure()
    } else {
      // install app
      // TODO we could do better error handling, e.g. when metadata or repo are null
      //  or install state is an error, also maybe show a Toast to user on error
      appInstallManager.install(
          packageName = packageName,
          appMetadata = db.getAppDao().getApp(version.repoId, packageName)?.metadata,
          version = version,
          currentVersionName = null,
          repo = repoManager.getRepository(version.repoId),
          iconModel = null,
          canAskPreApprovalNow = true,
      )
      Result.success()
    }
  }
}
