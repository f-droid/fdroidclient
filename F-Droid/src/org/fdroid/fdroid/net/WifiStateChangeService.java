
package org.fdroid.fdroid.net;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.LocalRepoKeyStore;
import org.fdroid.fdroid.localrepo.LocalRepoManager;

import java.security.cert.Certificate;
import java.util.Locale;

public class WifiStateChangeService extends Service {
    public static final String BROADCAST = "org.fdroid.fdroid.action.WIFI_CHANGE";

    private static WaitForWifiAsyncTask asyncTask;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (asyncTask != null)
            asyncTask.cancel(true);
        asyncTask = new WaitForWifiAsyncTask();
        asyncTask.execute();
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
                    if (isCancelled())
                        return null;
                    Log.i(TAG, "waiting for the wifi to be enabled...");
                    Thread.sleep(1000);
                }
                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                while (ipAddress == 0) {
                    if (isCancelled())
                        return null;
                    Log.i(TAG, "waiting for an IP address...");
                    Thread.sleep(1000);
                    ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                }
                if (isCancelled())
                    return null;
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                ipAddress = wifiInfo.getIpAddress();
                FDroidApp.ipAddressString = String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                        (ipAddress & 0xff),
                        (ipAddress >> 8 & 0xff),
                        (ipAddress >> 16 & 0xff),
                        (ipAddress >> 24 & 0xff));

                FDroidApp.ssid = wifiInfo.getSSID().replaceAll("^\"(.*)\"$", "$1");
                FDroidApp.bssid = wifiInfo.getBSSID();

                String scheme;
                if (Preferences.get().isLocalRepoHttpsEnabled())
                    scheme = "https";
                else
                    scheme = "http";
                FDroidApp.repo.name = Preferences.get().getLocalRepoName();
                FDroidApp.repo.address = String.format(Locale.ENGLISH, "%s://%s:%d/fdroid/repo",
                        scheme, FDroidApp.ipAddressString, FDroidApp.port);

                if (isCancelled())
                    return null;

                Context context = WifiStateChangeService.this.getApplicationContext();
                LocalRepoManager lrm = LocalRepoManager.get(context);
                lrm.setUriString(FDroidApp.repo.address);
                lrm.writeIndexPage(Utils.getSharingUri(context, FDroidApp.repo).toString());

                if (isCancelled())
                    return null;

                try {
                    LocalRepoKeyStore localRepoKeyStore = LocalRepoKeyStore.get(context);
                    Certificate localCert = localRepoKeyStore.getCertificate();
                    FDroidApp.repo.fingerprint = Utils.calcFingerprint(localCert);

                    /*
                     * Once the IP address is known we need to generate a self
                     * signed certificate to use for HTTPS that has a CN field set
                     * to the ipAddressString. This must be run in the background
                     * because if this is the first time the singleton is run, it
                     * can take a while to instantiate.
                     */
                    if (Preferences.get().isLocalRepoHttpsEnabled())
                        localRepoKeyStore.setupHTTPSCertificate();

                } catch (LocalRepoKeyStore.InitException e) {
                    Log.e(TAG, "Unable to configure a fingerprint or HTTPS for the local repo: " + e.getMessage());
                    Log.e(TAG, Log.getStackTraceString(e));
                }

            } catch (InterruptedException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Intent intent = new Intent(BROADCAST);
            LocalBroadcastManager.getInstance(WifiStateChangeService.this).sendBroadcast(intent);
            WifiStateChangeService.this.stopSelf();
            FDroidApp.restartLocalRepoService();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
