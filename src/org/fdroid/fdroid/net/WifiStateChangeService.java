
package org.fdroid.fdroid.net;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;

import java.util.Locale;

public class WifiStateChangeService extends Service {
    public static final String BROADCAST = "org.fdroid.fdroid.action.WIFI_CHANGE";

    private Context context;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;
        new WaitForWifiAsyncTask().execute();
        return START_NOT_STICKY;
    }

    public class WaitForWifiAsyncTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "WaitForWifiAsyncTask";
        private WifiManager wifiManager;

        @Override
        protected Void doInBackground(Void... params) {
            wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            try {
                while (!wifiManager.isWifiEnabled()) {
                    Log.i(TAG, "waiting for the wifi to be enabled...");
                    Thread.sleep(3000);
                }
                FDroidApp.ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                while (FDroidApp.ipAddress == 0) {
                    Log.i(TAG, "waiting for an IP address...");
                    Thread.sleep(3000);
                    FDroidApp.ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                }
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                FDroidApp.ipAddress = wifiInfo.getIpAddress();
                FDroidApp.ipAddressString = String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                        (FDroidApp.ipAddress & 0xff),
                        (FDroidApp.ipAddress >> 8 & 0xff),
                        (FDroidApp.ipAddress >> 16 & 0xff),
                        (FDroidApp.ipAddress >> 24 & 0xff));

                FDroidApp.ssid = wifiInfo.getSSID().replaceAll("^\"(.*)\"$", "$1");
                FDroidApp.bssid = wifiInfo.getBSSID();

                String scheme;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean("use_https", false))
                    scheme = "https";
                else
                    scheme = "http";
                FDroidApp.repo.address = String.format(Locale.ENGLISH, "%s://%s:%d/fdroid/repo",
                        scheme, FDroidApp.ipAddressString, FDroidApp.port);
                FDroidApp.localRepo.setUriString(FDroidApp.repo.address);
                FDroidApp.localRepo.writeIndexPage(
                        Utils.getSharingUri(context, FDroidApp.repo).toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Intent intent = new Intent(BROADCAST);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            WifiStateChangeService.this.stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
