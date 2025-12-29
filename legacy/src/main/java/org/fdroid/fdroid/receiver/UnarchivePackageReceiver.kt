package org.fdroid.fdroid.receiver

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_UNARCHIVE_PACKAGE
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ALL_USERS
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ID
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import android.util.Log
import org.fdroid.fdroid.work.UnarchiveWorker

private val TAG = UnarchivePackageReceiver::class.java.simpleName

@TargetApi(35)
class UnarchivePackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UNARCHIVE_PACKAGE) {
            Log.w(TAG, "Unknown action: ${intent.action}")
            return
        }
        val packageName = intent.getStringExtra(EXTRA_UNARCHIVE_PACKAGE_NAME) ?: error("")
        val unarchiveId = intent.getIntExtra(EXTRA_UNARCHIVE_ID, -1)
        val allUsers = intent.getBooleanExtra(EXTRA_UNARCHIVE_ALL_USERS, false)

        Log.i(TAG, "Intent received, un-archiving $packageName...")

        UnarchiveWorker.updateNow(context, packageName, unarchiveId, allUsers)
    }
}
