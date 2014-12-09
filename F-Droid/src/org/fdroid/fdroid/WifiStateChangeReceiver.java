
package org.fdroid.fdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import org.fdroid.fdroid.net.WifiStateChangeService;

public class WifiStateChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkInfo ni = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (ni.isConnected()) {
            context.startService(new Intent(context, WifiStateChangeService.class));
        }
    }
}
