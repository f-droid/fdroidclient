package org.fdroid.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MY_PACKAGE_REPLACED
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.annotation.RequiresApi
import mu.KotlinLogging

class AppUpdateReceiver : BroadcastReceiver() {

  private val log = KotlinLogging.logger {}

  @RequiresApi(35)
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_MY_PACKAGE_REPLACED) {
      log.warn { "Unknown action: ${intent.action}" }
      return
    }
    log.info { "Intent received, we just updated ourselves!" }
    val intent =
      context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK)
      }
    if (intent != null) {
      try {
        context.startActivity(intent)
      } catch (e: Exception) {
        log.error(e) { "Failed to start activity after update" }
      }
    }
  }
}
