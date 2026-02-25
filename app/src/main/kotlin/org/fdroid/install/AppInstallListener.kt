package org.fdroid.install

import android.app.PendingIntent

interface AppInstallListener {
  fun onStartInstall(sessionId: Int)

  fun onUserConfirmationNeeded(sessionId: Int, intent: PendingIntent)

  fun onInstalled()

  fun onInstallError(msg: String?)
}
