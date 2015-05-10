package org.fdroid.fdroid.net;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.LocalRepoKeyStore;
import org.fdroid.fdroid.localrepo.LocalRepoManager;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Locale;

public class WifiStateChangeService extends Service {
    private static final String TAG = "WifiStateChangeService";

    public static final String BROADCAST = "org.fdroid.fdroid.action.WIFI_CHANGE";

    private WifiManager wifiManager;
    private static WaitForWifiAsyncTask asyncTask;
    private int wifiState;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FDroidApp.initWifiSettings();
        NetworkInfo ni = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiState = wifiManager.getWifiState();
        if (ni == null || ni.isConnected()) {
            /* started on app start or from WifiStateChangeReceiver,
               NetworkInfo is only passed via WifiStateChangeReceiver */
            Log.i(TAG, "ni == " + ni + "  wifiState == " + printWifiState(wifiState));
            if (wifiState == WifiManager.WIFI_STATE_ENABLED
                    || wifiState == WifiManager.WIFI_STATE_DISABLING   // might be switching to hotspot
                    || wifiState == WifiManager.WIFI_STATE_DISABLED   // might be hotspot
                    || wifiState == WifiManager.WIFI_STATE_UNKNOWN) { // might be hotspot
                if (asyncTask != null) {
                    asyncTask.cancel(true);
                }
                asyncTask = new WaitForWifiAsyncTask();
                asyncTask.execute();
            }
        }
        return START_NOT_STICKY;
    }

    public class WaitForWifiAsyncTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "WaitForWifiAsyncTask";

        @Override
        protected Void doInBackground(Void... params) {
            try {
                WifiInfo wifiInfo = null;

                wifiState = wifiManager.getWifiState();

                while (FDroidApp.ipAddressString == null) {
                    if (isCancelled())  // can be canceled by a change via WifiStateChangeReceiver
                        return null;
                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        wifiInfo = wifiManager.getConnectionInfo();
                        FDroidApp.ipAddressString = formatIpAddress(wifiInfo.getIpAddress());
                    } else if (wifiState == WifiManager.WIFI_STATE_DISABLED
                            || wifiState == WifiManager.WIFI_STATE_DISABLING) {
                        // try once to see if its a hotspot
                        FDroidApp.ipAddressString = getIpAddressFromNetworkInterface();
                        if (FDroidApp.ipAddressString == null)
                            return null;
                    } else {  // a hotspot can be active during WIFI_STATE_UNKNOWN
                        FDroidApp.ipAddressString = getIpAddressFromNetworkInterface();
                    }
                    Thread.sleep(1000);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "waiting for an IP address...");
                    }
                }
                if (isCancelled())  // can be canceled by a change via WifiStateChangeReceiver
                    return null;

                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    if (ssid != null) {
                        FDroidApp.ssid = ssid.replaceAll("^\"(.*)\"$", "$1");
                    }
                    String bssid = wifiInfo.getBSSID();
                    if (bssid != null) {
                        FDroidApp.bssid = bssid;
                    }
                }

                String scheme;
                if (Preferences.get().isLocalRepoHttpsEnabled())
                    scheme = "https";
                else
                    scheme = "http";
                FDroidApp.repo.name = Preferences.get().getLocalRepoName();
                FDroidApp.repo.address = String.format(Locale.ENGLISH, "%s://%s:%d/fdroid/repo",
                        scheme, FDroidApp.ipAddressString, FDroidApp.port);

                if (isCancelled())  // can be canceled by a change via WifiStateChangeReceiver
                    return null;

                Context context = WifiStateChangeService.this.getApplicationContext();
                LocalRepoManager lrm = LocalRepoManager.get(context);
                lrm.writeIndexPage(Utils.getSharingUri(FDroidApp.repo).toString());

                if (isCancelled())  // can be canceled by a change via WifiStateChangeReceiver
                    return null;

                // the fingerprint for the local repo's signing key
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
            FDroidApp.restartLocalRepoServiceIfRunning();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getIpAddressFromNetworkInterface() {
        try {
            for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
                NetworkInterface netIf = networkInterfaces.nextElement();

                for (Enumeration<InetAddress> inetAddresses = netIf.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isLoopbackAddress() || inetAddress instanceof Inet6Address) {
                        continue;
                    } else if (netIf.getDisplayName().contains("wlan0")
                            || netIf.getDisplayName().contains("eth0")
                            || netIf.getDisplayName().contains("ap0")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private String formatIpAddress(int ipAddress) {
        if (ipAddress == 0) {
            return null;
        } else {
            return String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));
        }
    }

    private String printWifiState(int wifiState) {
        switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLED:
                return "WIFI_STATE_DISABLED";
            case WifiManager.WIFI_STATE_DISABLING:
                return "WIFI_STATE_DISABLING";
            case WifiManager.WIFI_STATE_ENABLING:
                return "WIFI_STATE_ENABLING";
            case WifiManager.WIFI_STATE_ENABLED:
                return "WIFI_STATE_ENABLED";
            case WifiManager.WIFI_STATE_UNKNOWN:
                return "WIFI_STATE_UNKNOWN";
        }
        return null;
    }
}
