package org.fdroid.fdroid.localrepo.type;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.SwapService;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Sends a {@link SwapService#BONJOUR_STATE_CHANGE} broadcasts when starting, started or stopped.
 */
public class BonjourBroadcast extends SwapType {

    private static final String TAG = "BonjourBroadcast";

    private JmDNS jmdns;
    private ServiceInfo pairService;

    public BonjourBroadcast(Context context) {
        super(context);
    }

    @Override
    public void start() {
        Utils.debugLog(TAG, "Preparing to start Bonjour service.");
        sendBroadcast(SwapService.EXTRA_STARTING);

        InetAddress address = getDeviceAddress();
        if (address == null) {
            Log.e(TAG, "Starting Bonjour service, but couldn't ascertain IP address."
                    + "  Seems we are not connected to a network.");
            return;
        }

        /*
         * a ServiceInfo can only be registered with a single instance
         * of JmDNS, and there is only ever a single LocalHTTPD port to
         * advertise anyway.
         */
        if (pairService != null || jmdns != null) {
            clearCurrentMDNSService();
        }

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
            Utils.debugLog(TAG, "Starting bonjour service...");
            pairService = ServiceInfo.create(type, repoName, FDroidApp.port, 0, 0, values);
            jmdns = JmDNS.create(address);
            jmdns.registerService(pairService);
            setConnected(true);
            Utils.debugLog(TAG, "... Bounjour service started.");
        } catch (IOException e) {
            Log.e(TAG, "Error while registering jmdns service", e);
            setConnected(false);
        }
    }

    @Override
    public void stop() {
        Utils.debugLog(TAG, "Unregistering MDNS service...");
        clearCurrentMDNSService();
        setConnected(false);
    }

    private void clearCurrentMDNSService() {
        if (jmdns != null) {
            jmdns.unregisterAllServices();
            Utils.closeQuietly(jmdns);
            pairService = null;
            jmdns = null;
        }
    }

    @Override
    public String getBroadcastAction() {
        return SwapService.BONJOUR_STATE_CHANGE;
    }

    @Nullable
    private InetAddress getDeviceAddress() {
        if (FDroidApp.ipAddressString != null) {
            try {
                return InetAddress.getByName(FDroidApp.ipAddressString);
            } catch (UnknownHostException ignored) {
            }
        }

        return null;
    }

}
