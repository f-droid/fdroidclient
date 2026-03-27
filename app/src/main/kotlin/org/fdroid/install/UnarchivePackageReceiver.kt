package org.fdroid.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_UNARCHIVE_PACKAGE
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ALL_USERS
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ID
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import androidx.annotation.RequiresApi
import mu.KotlinLogging

class UnarchivePackageReceiver : BroadcastReceiver() {

  private val log = KotlinLogging.logger {}

  @RequiresApi(35)
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_UNARCHIVE_PACKAGE) {
      log.warn { "Unknown action: ${intent.action}" }
      return
    }
    val packageName = intent.getStringExtra(EXTRA_UNARCHIVE_PACKAGE_NAME) ?: error("")
    val unarchiveId = intent.getIntExtra(EXTRA_UNARCHIVE_ID, -1)
    val allUsers = intent.getBooleanExtra(EXTRA_UNARCHIVE_ALL_USERS, false)

    log.info { "Intent received, un-archiving $packageName..." }

    UnarchiveWorker.updateNow(context, packageName, unarchiveId, allUsers)
  }
}
