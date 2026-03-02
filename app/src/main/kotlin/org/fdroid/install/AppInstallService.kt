package org.fdroid.install

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
import android.os.Build.VERSION.SDK_INT
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import mu.KotlinLogging
import org.fdroid.NotificationManager
import org.fdroid.NotificationManager.Companion.NOTIFICATION_ID_APP_INSTALLS

@AndroidEntryPoint
class AppInstallService : Service() {

  companion object {
    private val _isServiceRunning = AtomicBoolean(false)
    val isServiceRunning
      get() = _isServiceRunning.get()
  }

  private val log = KotlinLogging.logger {}

  @Inject lateinit var notificationManager: NotificationManager

  override fun onCreate() {
    log.info { "onCreate" }
    _isServiceRunning.set(true)
    super.onCreate() // apparently important for injection
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    log.info { "onStartCommand $intent" }
    val notificationState = InstallNotificationState()
    try {
      ServiceCompat.startForeground(
        this,
        NOTIFICATION_ID_APP_INSTALLS,
        notificationManager.getAppInstallNotification(notificationState).build(),
        if (SDK_INT >= 29) FOREGROUND_SERVICE_TYPE_MANIFEST else 0,
      )
    } catch (e: Exception) {
      log.error(e) { "Error starting foreground service: " }
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onBind(intent: Intent): IBinder? = null

  override fun onTimeout(startId: Int, fgsType: Int) {
    log.info { "onTimeout($startId, $fgsType)" }
    stopSelf()
  }

  override fun onDestroy() {
    log.info { "onDestroy" }
    _isServiceRunning.set(false)
  }
}
