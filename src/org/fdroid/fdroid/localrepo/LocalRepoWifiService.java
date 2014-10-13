package org.fdroid.fdroid.localrepo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.net.WifiStateChangeService;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.util.HashMap;

public class LocalRepoWifiService extends LocalRepoService {

    private static final String TAG = "org.fdroid.fdroid.localrepo.LocalRepoWifiService";
    private JmDNS jmdns;
    private ServiceInfo pairService;

    private BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            stopNetworkServices();
            startNetworkServices();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Preferences.get().registerLocalRepoBonjourListeners(localRepoBonjourChangeListener);
        LocalBroadcastManager.getInstance(this).registerReceiver(onWifiChange,
                new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Preferences.get().unregisterLocalRepoBonjourListeners(localRepoBonjourChangeListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onWifiChange);
    }

    @Override
    protected void onStartNetworkServices() {
        if (Preferences.get().isLocalRepoBonjourEnabled())
            registerMDNSService();
        Preferences.get().registerLocalRepoHttpsListeners(localRepoHttpsChangeListener);
    }

    @Override
    protected void onStopNetworkServices() {
        Log.d(TAG, "Stopping local repo network services");
        Preferences.get().unregisterLocalRepoHttpsListeners(localRepoHttpsChangeListener);

        Log.d(TAG, "Unregistering MDNS service...");
        unregisterMDNSService();
    }

    @Override
    protected boolean useHttps() {
        return Preferences.get().isLocalRepoHttpsEnabled();
    }

    @Override
    protected String getIpAddressToBindTo() {
        return FDroidApp.ipAddressString;
    }

    private Preferences.ChangeListener localRepoBonjourChangeListener = new Preferences.ChangeListener() {
        @Override
        public void onPreferenceChange() {
            if (localHttpd.isAlive())
                if (Preferences.get().isLocalRepoBonjourEnabled())
                    registerMDNSService();
                else
                    unregisterMDNSService();
        }
    };

    private Preferences.ChangeListener localRepoHttpsChangeListener = new Preferences.ChangeListener() {
        @Override
        public void onPreferenceChange() {
            Log.i("localRepoHttpsChangeListener", "onPreferenceChange");
            if (localHttpd.isAlive()) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        stopNetworkServices();
                        startNetworkServices();
                        return null;
                    }
                }.execute();
            }
        }
    };

    private void registerMDNSService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*
                 * a ServiceInfo can only be registered with a single instance
                 * of JmDNS, and there is only ever a single LocalHTTPD port to
                 * advertise anyway.
                 */
                if (pairService != null || jmdns != null)
                    clearCurrentMDNSService();
                String repoName = Preferences.get().getLocalRepoName();
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("path", "/fdroid/repo");
                values.put("name", repoName);
                values.put("fingerprint", FDroidApp.repo.fingerprint);
                String type;
                if (Preferences.get().isLocalRepoHttpsEnabled()) {
                    values.put("type", "fdroidrepos");
                    type = "_https._tcp.local.";
                } else {
                    values.put("type", "fdroidrepo");
                    type = "_http._tcp.local.";
                }
                try {
                    pairService = ServiceInfo.create(type, repoName, FDroidApp.port, 0, 0, values);
                    jmdns = JmDNS.create();
                    jmdns.registerService(pairService);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void unregisterMDNSService() {
        if (localRepoBonjourChangeListener != null) {
            Preferences.get().unregisterLocalRepoBonjourListeners(localRepoBonjourChangeListener);
            localRepoBonjourChangeListener = null;
        }
        clearCurrentMDNSService();
    }

    private void clearCurrentMDNSService() {
        if (jmdns != null) {
            if (pairService != null) {
                jmdns.unregisterService(pairService);
                pairService = null;
            }
            jmdns.unregisterAllServices();
            try {
                jmdns.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            jmdns = null;
        }
    }
}
