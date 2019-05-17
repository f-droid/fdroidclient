package org.fdroid.fdroid.localrepo.type;

import android.content.Context;
import android.net.wifi.WifiManager;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.localrepo.BonjourManager;
import org.fdroid.fdroid.localrepo.LocalHTTPDManager;
import org.fdroid.fdroid.localrepo.SwapService;

@SuppressWarnings("LineLength")
public class WifiSwap extends SwapType {

    private static final String TAG = "WifiSwap";

    private final WifiManager wifiManager;

    public WifiSwap(Context context, WifiManager wifiManager) {
        super(context);
        this.wifiManager = wifiManager;
    }

    protected String getBroadcastAction() {
        return SwapService.WIFI_STATE_CHANGE;
    }

    @Override
    public void start() {
        sendBroadcast(SwapService.EXTRA_STARTING);
        wifiManager.setWifiEnabled(true);
        LocalHTTPDManager.start(context);
        BonjourManager.start(context);
        BonjourManager.setVisible(context, SwapService.getWifiVisibleUserPreference());

        if (FDroidApp.ipAddressString == null) {
            setConnected(false);
        } else {
            setConnected(true);
        }
    }

    @Override
    public void stop() {
        sendBroadcast(SwapService.EXTRA_STOPPING); // This needs to be per-SwapType
        LocalHTTPDManager.stop(context);
        BonjourManager.stop(context);
        setConnected(false);
    }

}
