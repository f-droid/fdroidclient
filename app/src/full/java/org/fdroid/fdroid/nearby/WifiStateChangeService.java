package org.fdroid.fdroid.nearby;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.commons.net.util.SubnetUtils;
import org.fdroid.database.Repository;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Locale;

import cc.mvdan.accesspoint.WifiApControl;

/**
 * Handle state changes to the device's wifi, storing the required bits.
 * The {@link Intent} that starts it either has no extras included,
 * which is how it can be triggered by code, or it came in from the system
 * via {@link WifiStateChangeReceiver}, in
 * which case an instance of {@link NetworkInfo} is included.
 * <p>
 * The work is done in a {@link Thread} so that new incoming {@code Intents}
 * are not blocked by processing. A new {@code Intent} immediately nullifies
 * the current state because it means that something about the wifi has
 * changed.  Having the {@code Thread} also makes it easy to kill work
 * that is in progress.
 * <p>
 * This also schedules an update to encourage updates happening on
 * unmetered networks like typical WiFi rather than networks that can
 * cost money or have caps.  The logic for checking the state of the
 * internet connection is in {@link org.fdroid.fdroid.UpdateService#onHandleWork(Intent)}
 * <p>
 * Some devices send multiple copies of given events, like a Moto G often
 * sends three {@code CONNECTED} events.  So they have to be debounced to
 * keep the {@link #BROADCAST} useful.
 */
@SuppressWarnings("LineLength")
public class WifiStateChangeService extends Worker {
    private static final String TAG = "WifiStateChangeService";

    public static final String BROADCAST = "org.fdroid.fdroid.action.WIFI_CHANGE";
    public static final String EXTRA_STATUS = "wifiStateChangeStatus";

    private WifiManager wifiManager;
    private static WifiInfoThread wifiInfoThread;
    private static int previousWifiState = Integer.MIN_VALUE;
    private volatile static int wifiState;
    private static final int NETWORK_INFO_STATE_NOT_SET = -1;

    public WifiStateChangeService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void registerReceiver(Context context) {
        ContextCompat.registerReceiver(
            context,
            new WifiStateChangeReceiver(),
            new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    public static void start(Context context, @Nullable Intent intent) {
        int networkInfoStateInt = NETWORK_INFO_STATE_NOT_SET;
        if (intent != null) {
            NetworkInfo ni = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            networkInfoStateInt = ni.getState().ordinal();
        }

        WorkRequest workRequest = new OneTimeWorkRequest.Builder(WifiStateChangeService.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setInputData(new Data.Builder()
                        .putInt(WifiManager.EXTRA_NETWORK_INFO, networkInfoStateInt)
                        .build()
                )
                .build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
        int networkInfoStateInt = getInputData().getInt(WifiManager.EXTRA_NETWORK_INFO, NETWORK_INFO_STATE_NOT_SET);
        NetworkInfo.State networkInfoState = null;
        if (networkInfoStateInt != NETWORK_INFO_STATE_NOT_SET) {
            networkInfoState = NetworkInfo.State.values()[networkInfoStateInt];
        }
        Utils.debugLog(TAG, "WiFi change service started.");
        wifiManager = ContextCompat.getSystemService(getApplicationContext(), WifiManager.class);
        if (wifiManager == null) {
            return Result.failure();
        }
        wifiState = wifiManager.getWifiState();
        Utils.debugLog(TAG, "networkInfoStateInt == " + networkInfoStateInt
                + "  wifiState == " + printWifiState(wifiState));
        if (networkInfoState == null
                || networkInfoState == NetworkInfo.State.CONNECTED
                || networkInfoState == NetworkInfo.State.DISCONNECTED) {
            if (previousWifiState != wifiState &&
                    (wifiState == WifiManager.WIFI_STATE_ENABLED
                            || wifiState == WifiManager.WIFI_STATE_DISABLING  // might be switching to hotspot
                            || wifiState == WifiManager.WIFI_STATE_DISABLED   // might be hotspot
                            || wifiState == WifiManager.WIFI_STATE_UNKNOWN)) { // might be hotspot
                if (wifiInfoThread != null) {
                    wifiInfoThread.interrupt();
                }
                wifiInfoThread = new WifiInfoThread();
                wifiInfoThread.start();
            }
        }
        return Result.success();
    }

    public class WifiInfoThread extends Thread {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
            try {
                FDroidApp.initWifiSettings();
                Utils.debugLog(TAG, "Checking wifi state (in background thread).");
                WifiInfo wifiInfo = null;

                int wifiState = wifiManager.getWifiState();
                int retryCount = 0;
                while (FDroidApp.ipAddressString == null) {
                    if (isInterrupted()) { // can be canceled by a change via WifiStateChangeReceiver
                        return;
                    }
                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        wifiInfo = wifiManager.getConnectionInfo();
                        FDroidApp.ipAddressString = formatIpAddress(wifiInfo.getIpAddress());
                        setSsid(wifiInfo);
                        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                        if (dhcpInfo != null) {
                            String netmask = formatIpAddress(dhcpInfo.netmask);
                            if (!TextUtils.isEmpty(FDroidApp.ipAddressString) && netmask != null) {
                                try {
                                    FDroidApp.subnetInfo = new SubnetUtils(FDroidApp.ipAddressString, netmask).getInfo();
                                } catch (IllegalArgumentException e) {
                                    // catch mystery: "java.lang.IllegalArgumentException: Could not parse [null/24]"
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (FDroidApp.ipAddressString == null
                                || FDroidApp.subnetInfo == FDroidApp.UNSET_SUBNET_INFO) {
                            setIpInfoFromNetworkInterface();
                        }
                    } else if (wifiState == WifiManager.WIFI_STATE_DISABLED
                            || wifiState == WifiManager.WIFI_STATE_DISABLING
                            || wifiState == WifiManager.WIFI_STATE_UNKNOWN) {
                        // try once to see if its a hotspot
                        setIpInfoFromNetworkInterface();
                        if (FDroidApp.ipAddressString == null) {
                            return;
                        }
                    }

                    if (retryCount > 120) {
                        return;
                    }
                    retryCount++;

                    if (FDroidApp.ipAddressString == null) {
                        Thread.sleep(1000);
                        Utils.debugLog(TAG, "waiting for an IP address...");
                    }
                }
                if (isInterrupted()) { // can be canceled by a change via WifiStateChangeReceiver
                    return;
                }

                setSsid(wifiInfo);

                String scheme;
                if (Preferences.get().isLocalRepoHttpsEnabled()) {
                    scheme = "https";
                } else {
                    scheme = "http";
                }
                Context context = WifiStateChangeService.this.getApplicationContext();
                String address = String.format(Locale.ENGLISH, "%s://%s:%d/fdroid/repo",
                        scheme, FDroidApp.ipAddressString, FDroidApp.port);
                // the fingerprint for the local repo's signing key
                LocalRepoKeyStore localRepoKeyStore = LocalRepoKeyStore.get(context);
                Certificate localCert = localRepoKeyStore.getCertificate();
                String cert = localCert == null ?
                        null : Hasher.hex(localCert).toLowerCase(Locale.US);
                Repository repo = FDroidApp.createSwapRepo(address, cert);

                if (isInterrupted()) { // can be canceled by a change via WifiStateChangeReceiver
                    return;
                }

                LocalRepoManager lrm = LocalRepoManager.get(context);
                lrm.writeIndexPage(Utils.getSharingUri(FDroidApp.repo).toString());

                if (isInterrupted()) { // can be canceled by a change via WifiStateChangeReceiver
                    return;
                }

                FDroidApp.repo = repo;

                /*
                 * Once the IP address is known we need to generate a self
                 * signed certificate to use for HTTPS that has a CN field set
                 * to the ipAddressString. This must be run in the background
                 * because if this is the first time the singleton is run, it
                 * can take a while to instantiate.
                 */
                if (Preferences.get().isLocalRepoHttpsEnabled()) {
                    localRepoKeyStore.setupHTTPSCertificate();
                }

            } catch (LocalRepoKeyStore.InitException e) {
                Log.e(TAG, "Unable to configure a fingerprint or HTTPS for the local repo", e);
            } catch (InterruptedException e) {
                Utils.debugLog(TAG, "interrupted");
                return;
            }
            Intent intent = new Intent(BROADCAST);
            intent.putExtra(EXTRA_STATUS, wifiState);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    private void setSsid(WifiInfo wifiInfo) {
        Context context = getApplicationContext();
        if (wifiInfo != null && wifiInfo.getBSSID() != null) {
            String ssid = wifiInfo.getSSID();
            Utils.debugLog(TAG, "Have wifi info, connected to " + ssid);
            if (ssid == null) {
                FDroidApp.ssid = context.getString(R.string.swap_blank_wifi_ssid);
            } else {
                FDroidApp.ssid = ssid.replaceAll("^\"(.*)\"$", "$1");
            }
            FDroidApp.bssid = wifiInfo.getBSSID();
        } else {
            WifiApControl wifiApControl = WifiApControl.getInstance(context);
            Utils.debugLog(TAG, "WifiApControl: " + wifiApControl);
            if (wifiApControl == null && FDroidApp.ipAddressString != null) {
                wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null && wifiInfo.getBSSID() != null) {
                    setSsid(wifiInfo);
                } else {
                    FDroidApp.ssid = context.getString(R.string.swap_active_hotspot, "");
                }
            } else if (wifiApControl != null && wifiApControl.isEnabled()) {
                WifiConfiguration wifiConfiguration = wifiApControl.getConfiguration();
                Utils.debugLog(TAG, "WifiConfiguration: " + wifiConfiguration);
                if (wifiConfiguration == null) {
                    FDroidApp.ssid = context.getString(R.string.swap_active_hotspot, "");
                    FDroidApp.bssid = "";
                    return;
                }

                if (wifiConfiguration.hiddenSSID) {
                    FDroidApp.ssid = context.getString(R.string.swap_hidden_wifi_ssid);
                } else {
                    FDroidApp.ssid = wifiConfiguration.SSID;
                }
                FDroidApp.bssid = wifiConfiguration.BSSID;
            }
        }
    }

    /**
     * Search for known Wi-Fi, Hotspot, and local network interfaces and get
     * the IP Address info from it.  This is necessary because network
     * interfaces in Hotspot/AP mode do not show up in the regular
     * {@link WifiManager} queries, and also on
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP Android 5.0} and newer,
     * {@link WifiManager#getDhcpInfo()} returns an invalid netmask.
     *
     * @see <a href="https://issuetracker.google.com/issues/37015180">netmask of WifiManager.getDhcpInfo() is always zero on Android 5.0</a>
     */
    private void setIpInfoFromNetworkInterface() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces == null) {
                return;
            }
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface netIf = networkInterfaces.nextElement();

                for (Enumeration<InetAddress> inetAddresses = netIf.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isLoopbackAddress() || inetAddress instanceof Inet6Address) {
                        continue;
                    }
                    if (netIf.getDisplayName().contains("wlan0")
                            || netIf.getDisplayName().contains("eth0")
                            || netIf.getDisplayName().contains("ap0")) {
                        FDroidApp.ipAddressString = inetAddress.getHostAddress();
                        for (InterfaceAddress address : netIf.getInterfaceAddresses()) {
                            short networkPrefixLength = address.getNetworkPrefixLength();
                            if (networkPrefixLength > 32) {
                                // something is giving a "/64" netmask, IPv6?
                                // java.lang.IllegalArgumentException: Value [64] not in range [0,32]
                                continue;
                            }
                            try {
                                String cidr = String.format(Locale.ENGLISH, "%s/%d",
                                        FDroidApp.ipAddressString, networkPrefixLength);
                                FDroidApp.subnetInfo = new SubnetUtils(cidr).getInfo();
                                break;
                            } catch (IllegalArgumentException e) {
                                if (BuildConfig.DEBUG) {
                                    e.printStackTrace();
                                } else {
                                    Log.i(TAG, "Getting subnet failed: " + e.getLocalizedMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (NullPointerException | SocketException e) {
            // NetworkInterface.getNetworkInterfaces() can throw a NullPointerException internally
            Log.e(TAG, "Could not get ip address", e);
        }
    }

    static String formatIpAddress(int ipAddress) {
        if (ipAddress == 0) {
            return null;
        }
        return String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                ipAddress & 0xff,
                ipAddress >> 8 & 0xff,
                ipAddress >> 16 & 0xff,
                ipAddress >> 24 & 0xff);
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
            case Integer.MIN_VALUE:
                return "previous value unset";
            default:
                return "~not mapped~";
        }
    }
}
