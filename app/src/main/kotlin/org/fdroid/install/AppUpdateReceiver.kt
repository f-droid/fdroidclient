package org.fdroid.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MY_PACKAGE_REPLACED
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build.VERSION.SDK_INT
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import mu.KotlinLogging
import org.fdroid.NotificationManager

@AndroidEntryPoint
class AppUpdateReceiver : BroadcastReceiver() {

  private val log = KotlinLogging.logger {}

  @Inject lateinit var notificationManager: NotificationManager

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
    if (intent == null) {
      log.error { "Could not get launch intent for ourselves" }
    } else {
      try {
        context.startActivity(intent)
      } catch (e: Exception) {
        log.error(e) { "Failed to start activity after update" }
      }
    }
    // show notification on Android 10+, because we aren't allowed to start activity from background
    // see: https://developer.android.com/guide/components/activities/background-starts
    if (SDK_INT >= 29) notificationManager.showSelfUpdateNotification()
  }
}
