package org.fdroid.fdroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.fdroid.fdroid.work.RepoUpdateWorker

private val TAG = RepoUpdateReceiver::class.java.simpleName

/**
 * This receiver allows OS components to trigger a repository update.
 * One known use-case for this is to do an initial repository update during SetupWizard,
 * so app data is available when needed, e.g. for restoring app backups.
 */
class RepoUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "org.fdroid.action.UPDATE_REPOS") {
            Log.w(TAG, "Unknown action: ${intent.action}")
            return
        }
        Log.i(TAG, "Intent received, updating repos...")
        RepoUpdateWorker.updateNow(context)
    }
}
