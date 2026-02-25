package org.fdroid.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_INTENT
import android.content.pm.PackageInstaller.EXTRA_PACKAGE_NAME
import android.content.pm.PackageInstaller.EXTRA_SESSION_ID
import android.content.pm.PackageInstaller.EXTRA_STATUS
import android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE
import androidx.core.content.IntentCompat.getParcelableExtra
import mu.KotlinLogging

class InstallBroadcastReceiver(
  private val sessionId: Int,
  private val listener:
    InstallBroadcastReceiver.(status: Int, confirmIntent: Intent?, msg: String?) -> Unit,
) : BroadcastReceiver() {

  private val log = KotlinLogging.logger {}

  override fun onReceive(context: Context, intent: Intent) {
    val receivedSessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
    if (receivedSessionId != sessionId) {
      log.warn { "Received intent for session $receivedSessionId, but expected $sessionId" }
      return
    }
    val confirmIntent = getParcelableExtra(intent, EXTRA_INTENT, Intent::class.java)
    val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
    val status = intent.getIntExtra(EXTRA_STATUS, Int.MIN_VALUE)
    val msg = intent.getStringExtra(EXTRA_STATUS_MESSAGE)
    val warnings = intent.getStringArrayListExtra("android.content.pm.extra.WARNINGS")
    log.info { "Received broadcast for $packageName ($sessionId) $status: $msg" }
    if (!warnings.isNullOrEmpty()) {
      warnings.forEach { log.warn { it } }
    }
    listener(status, confirmIntent, msg)
  }
}
