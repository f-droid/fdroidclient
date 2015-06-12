package org.fdroid.fdroid.localrepo.type;

import android.content.Context;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;

import java.io.IOException;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class BonjourType implements SwapType {

    private static final String TAG = "BonjourBroadcastType";

    private JmDNS jmdns;
    private ServiceInfo pairService;
    private final Context context;

    public BonjourType(Context context) {
        this.context = context;
    }

    @Override
    public void start() {

        if (Preferences.get().isLocalRepoBonjourEnabled())
            return;

        /*
         * a ServiceInfo can only be registered with a single instance
         * of JmDNS, and there is only ever a single LocalHTTPD port to
         * advertise anyway.
         */
        if (pairService != null || jmdns != null)
            clearCurrentMDNSService();
        String repoName = Preferences.get().getLocalRepoName();
        HashMap<String, String> values = new HashMap<>();
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
            Log.d(TAG, "Starting bonjour service...");
            pairService = ServiceInfo.create(type, repoName, FDroidApp.port, 0, 0, values);
            jmdns = JmDNS.create();
            jmdns.registerService(pairService);
            Log.d(TAG, "... Bounjour service started.");
        } catch (IOException e) {
            Log.e(TAG, "Error while registering jmdns service: " + e);
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public void stop() {
        Log.d(TAG, "Unregistering MDNS service...");
        clearCurrentMDNSService();
    }

    private void clearCurrentMDNSService() {
        if (jmdns != null) {
            if (pairService != null) {
                jmdns.unregisterService(pairService);
                pairService = null;
            }
            jmdns.unregisterAllServices();
            Utils.closeQuietly(jmdns);
            jmdns = null;
        }
    }

    @Override
    public void restart() {

    }

}
