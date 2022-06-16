package org.fdroid.fdroid.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;
import androidx.core.net.ConnectivityManagerCompat;

import org.fdroid.fdroid.FDroidApp;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * An {@link JobIntentService} subclass for tracking whether there is metered or
 * unmetered internet available, based on
 * {@link android.net.ConnectivityManager#CONNECTIVITY_ACTION}
 * <p>
 * Android 7.0 removed {@link android.net.ConnectivityManager#CONNECTIVITY_ACTION} so this will
 * need to be totally changed to support that.
 *
 * @see <a href="https://developer.android.com/topic/performance/background-optimization">Background Optimizations</a>
 */
public class ConnectivityMonitorService extends JobIntentService {
    public static final String TAG = "ConnectivityMonitorServ";

    public static final int FLAG_NET_UNAVAILABLE = 0;
    public static final int FLAG_NET_METERED = 1;
    public static final int FLAG_NET_NO_LIMIT = 2;
    public static final int FLAG_NET_DEVICE_AP_WITHOUT_INTERNET = 3;

    private static final String ACTION_START = "org.fdroid.fdroid.net.action.CONNECTIVITY_MONITOR";

    private static final BroadcastReceiver CONNECTIVITY_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            start(context);
        }
    };

    /**
     * Register the {@link BroadcastReceiver} which also starts this
     * {@code Service} since it is a sticky broadcast. This cannot be
     * registered in the manifest, since Android 7.0 makes that not work.
     */
    public static void registerAndStart(Context context) {
        context.registerReceiver(CONNECTIVITY_RECEIVER, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, ConnectivityMonitorService.class);
        intent.setAction(ACTION_START);
        enqueueWork(context, ConnectivityMonitorService.class, 0x982ae7b, intent);
    }

    /**
     * Gets the state of internet availability, whether there is no connection at all,
     * whether the connection has no usage limit (like most WiFi), or whether this is
     * a metered connection like most cellular plans or hotspot WiFi connections. This
     * also detects whether the device has a hotspot AP enabled but the mobile
     * connection does not provide internet.  That is a special case that is useful
     * for nearby swapping, but nothing else.
     * <p>
     * {@link NullPointerException}s are ignored in the hotspot detection since that
     * detection should not affect normal usage at all, and there are often weird
     * cases when looking through the network devices, especially on bad ROMs.
     */
    public static int getNetworkState(Context context) {
        ConnectivityManager cm = ContextCompat.getSystemService(context, ConnectivityManager.class);
        if (cm == null) {
            return FLAG_NET_UNAVAILABLE;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork == null && cm.getAllNetworks().length == 0) {
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface netIf = networkInterfaces.nextElement();
                    if (netIf.getDisplayName().contains("wlan0")
                            || netIf.getDisplayName().contains("eth0")
                            || netIf.getDisplayName().contains("ap0")) {
                        for (Enumeration<InetAddress> addr = netIf.getInetAddresses(); addr.hasMoreElements();) {
                            InetAddress inetAddress = addr.nextElement();
                            if (inetAddress.isLoopbackAddress() || inetAddress instanceof Inet6Address) {
                                continue;
                            }
                            Log.i(TAG, "FLAG_NET_DEVICE_AP_WITHOUT_INTERNET: " + netIf.getDisplayName()
                                    + " " + inetAddress);
                            return FLAG_NET_DEVICE_AP_WITHOUT_INTERNET; // NOPMD
                        }
                    }
                }
            } catch (SocketException | NullPointerException e) { // NOPMD
                // ignored
            }
        }

        if (activeNetwork == null || !activeNetwork.isConnected()) {
            return FLAG_NET_UNAVAILABLE;
        }

        int networkType = activeNetwork.getType();
        switch (networkType) {
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_WIFI:
                if (ConnectivityManagerCompat.isActiveNetworkMetered(cm)) {
                    return FLAG_NET_METERED;
                } else {
                    return FLAG_NET_NO_LIMIT;
                }
            default:
                return FLAG_NET_METERED;
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_START.equals(intent.getAction())) {
            FDroidApp.networkState = getNetworkState(this);
        }
    }
}
