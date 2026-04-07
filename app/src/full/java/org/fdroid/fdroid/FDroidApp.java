package org.fdroid.fdroid;

import android.content.Context;

import org.apache.commons.net.util.SubnetUtils;
import org.fdroid.database.Repository;
import org.fdroid.index.IndexFormatVersion;

import javax.annotation.Nullable;

/**
 * Holds global state used by the swap/nearby feature.
 * In the new app, org.fdroid.App is the Application class;
 * this class provides only the static fields and helpers needed by swap.
 */
public class FDroidApp {

    // for the local repo on this device
    public static volatile int port = 8888;
    public static volatile boolean generateNewPort;
    @Nullable
    public static volatile String ipAddressString;
    @Nullable
    public static volatile SubnetUtils.SubnetInfo subnetInfo;
    @Nullable
    public static volatile String ssid;
    @Nullable
    public static volatile String bssid;
    @Nullable
    public static volatile Repository repo;

    @SuppressWarnings("unused")
    public static volatile String queryString;

    public static final SubnetUtils.SubnetInfo UNSET_SUBNET_INFO =
            new SubnetUtils("0.0.0.0/32").getInfo();

    public static void initWifiSettings() {
        port = generateNewPort ? (int) (Math.random() * 10000 + 8080) : port == 0 ? 8888 : port;
        generateNewPort = false;
        ipAddressString = null;
        subnetInfo = UNSET_SUBNET_INFO;
        ssid = null;
        bssid = null;
    }

    public static Repository createSwapRepo(String address, String certificate) {
        long now = System.currentTimeMillis();
        if (certificate == null) certificate = "d0ef";
        return new Repository(42L, address, now, IndexFormatVersion.ONE, certificate, 20001L, 42, now);
    }

    private static Context appContext;

    public static void setContext(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Context getInstance() {
        return appContext;
    }
}
