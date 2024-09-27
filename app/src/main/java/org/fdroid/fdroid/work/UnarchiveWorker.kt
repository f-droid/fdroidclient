package org.fdroid.fdroid.work

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ALL_USERS
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ID
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import android.content.pm.PackageManager.MATCH_ARCHIVED_PACKAGES
import android.content.pm.PackageManager.PackageInfoFlags
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.asFlow
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.fdroid.database.DbUpdateChecker
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.data.Apk
import org.fdroid.fdroid.data.App
import org.fdroid.fdroid.data.DBHelper
import org.fdroid.fdroid.installer.InstallManagerService

private val TAG = UnarchiveWorker::class.java.simpleName

@TargetApi(35)
class UnarchiveWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        @UiThread
        fun updateNow(context: Context, packageName: String, unarchiveId: Int, allUsers: Boolean) {
            val data = Data.Builder()
                .putString(EXTRA_UNARCHIVE_PACKAGE_NAME, packageName)
                .putInt(EXTRA_UNARCHIVE_ID, unarchiveId)
                .putBoolean(EXTRA_UNARCHIVE_ALL_USERS, allUsers)
                .build()
            val request = OneTimeWorkRequestBuilder<UnarchiveWorker>()
                .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context)
                .enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        val packageName = inputData.getString(EXTRA_UNARCHIVE_PACKAGE_NAME)
            ?: error("No packageName")
        val unarchiveId = inputData.getInt(EXTRA_UNARCHIVE_ID, -1)
        val allUsers = inputData.getBoolean(EXTRA_UNARCHIVE_ALL_USERS, false)

        Log.i(TAG, "$packageName $unarchiveId $allUsers")

        // get archived PackageInfo
        val pm = applicationContext.packageManager
        @SuppressLint("WrongConstant") // not sure why MATCH_ARCHIVED_PACKAGES is considered wrong
        val packageInfo =
            pm.getPackageInfo(packageName, PackageInfoFlags.of(MATCH_ARCHIVED_PACKAGES))

        // find suggested version for that app
        val db = DBHelper.getDb(applicationContext)
        val updateChecker = DbUpdateChecker(db, pm)
        val appPrefs = db.getAppPrefsDao().getAppPrefs(packageName).asFlow().first()
        val version = updateChecker.getSuggestedVersion(
            packageName = packageName,
            // TODO we could try to get the old signer (if still available) and search for the same
            preferredSigner = null,
            releaseChannels = appPrefs.releaseChannels,
            onlyFromPreferredRepo = true,
        )
        // install version, if available
        return if (version == null) {
            Log.e(TAG, "Could not find a version to unarchive for $packageName")
            Result.failure()
        } else {
            // get all the objects our InstallManagerService requires
            val repoManager = FDroidApp.getRepoManager(applicationContext)
            // repos may not have loaded yet, so we use the flow and wait for repos to be ready
            val repo = repoManager.repositoriesState.first().find { it.repoId == version.repoId }
            val dbApp = db.getAppDao().getApp(version.repoId, packageName)
            val app = App(dbApp, packageInfo)
            val apk = Apk(version, repo)
            // fire off installation, should happen automatically from here on
            InstallManagerService.queue(applicationContext, app, apk)
            Result.success()
        }
    }

}
