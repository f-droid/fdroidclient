package org.fdroid.install

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

typealias InstallBroadcastListener =
  InstallBroadcastReceiver.(status: Int, confirmIntent: Intent?, msg: String?) -> Unit

@Singleton
class InstallBroadcastReceiverFactory @Inject constructor() {
  fun create(sessionId: Int, listener: InstallBroadcastListener): InstallBroadcastReceiver {
    return InstallBroadcastReceiver(sessionId, listener)
  }
}
