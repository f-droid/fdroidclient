package org.fdroid.updates

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import mu.KotlinLogging
import org.fdroid.NotificationManager

@AndroidEntryPoint
class SelfUpdateReceiver : BroadcastReceiver() {

  private val log = KotlinLogging.logger {}

  @Inject lateinit var notificationManager: NotificationManager

  companion object {
    fun enable(context: Context) {
      setEnabledState(context, COMPONENT_ENABLED_STATE_ENABLED)
    }

    fun disable(context: Context) {
      setEnabledState(context, COMPONENT_ENABLED_STATE_DISABLED)
    }

    private fun setEnabledState(context: Context, state: Int) {
      val component = ComponentName(context, SelfUpdateReceiver::class.java)
      context.packageManager.setComponentEnabledSetting(
        component,
        state,
        DONT_KILL_APP,
      )
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
      log.warn { "Unknown action: ${intent.action}" }
      return
    }
    log.info { "Intent received, we just updated ourselves!" }
    val intent =
      context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    if (Build.VERSION.SDK_INT >= 29) notificationManager.showSelfUpdateNotification()
  }
}
