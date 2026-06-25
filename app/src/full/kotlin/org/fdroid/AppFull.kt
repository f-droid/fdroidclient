package org.fdroid

import android.content.Context;
import org.fdroid.fdroid.nearby.WifiStateChangeService

/** Wrapper class to isolate things only needed in the full flavor. */
class AppFull {
    companion object {
        fun onCreate(context: Context) {
            WifiStateChangeService.registerReceiver(context)
            WifiStateChangeService.start(context, null)
        }
    }
}
