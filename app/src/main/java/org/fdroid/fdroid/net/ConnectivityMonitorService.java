package org.fdroid.fdroid.net;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;

/**
 * An {@link IntentService} subclass for tracking whether there is metered or
 * unmetered internet available, based on
 * {@link android.net.ConnectivityManager#CONNECTIVITY_ACTION}
 */
public class ConnectivityMonitorService extends IntentService {
    public static final String TAG = "ConnectivityMonitorServ";

    public static final int FLAG_NET_UNAVAILABLE = 0;
    public static final int FLAG_NET_METERED = 1;
    public static final int FLAG_NET_NO_LIMIT = 2;

    private static final String ACTION_START = "org.fdroid.fdroid.net.action.CONNECTIVITY_MONITOR";

    private static final BroadcastReceiver CONNECTIVITY_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            start(context);
        }
    };

    public ConnectivityMonitorService() {
        super("ConnectivityMonitorService");
    }

    public static void registerAndStart(Context context) {
        context.registerReceiver(CONNECTIVITY_RECEIVER, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, ConnectivityMonitorService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    /**
     * Gets the state of internet availability, whether there is no connection at all,
     * whether the connection has no usage limit (like most WiFi), or whether this is
     * a metered connection like most cellular plans or hotspot WiFi connections.
     */
    public static int getNetworkState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) {
            return FLAG_NET_UNAVAILABLE;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            return FLAG_NET_UNAVAILABLE;
        }

        int networkType = activeNetwork.getType();
        switch (networkType) {
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_WIFI:
                if (Build.VERSION.SDK_INT >= 16 && cm.isActiveNetworkMetered()) {
                    return FLAG_NET_METERED;
                } else {
                    return FLAG_NET_NO_LIMIT;
                }
            default:
                return FLAG_NET_METERED;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            FDroidApp.networkState = getNetworkState(this);
            ImageLoader.getInstance().denyNetworkDownloads(!Preferences.get().isBackgroundDownloadAllowed());
        }
    }
}
