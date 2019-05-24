package org.fdroid.fdroid.localrepo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.localrepo.peers.Peer;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Central service which manages all of the different moving parts of swap which are required
 * to enable p2p swapping of apps.
 */
public class SwapService extends Service {
    private static final String TAG = "SwapService";

    private static final String SHARED_PREFERENCES = "swap-state";
    private static final String KEY_APPS_TO_SWAP = "appsToSwap";
    private static final String KEY_BLUETOOTH_ENABLED = "bluetoothEnabled";
    private static final String KEY_WIFI_ENABLED = "wifiEnabled";
    private static final String KEY_BLUETOOTH_ENABLED_BEFORE_SWAP = "bluetoothEnabledBeforeSwap";
    private static final String KEY_BLUETOOTH_NAME_BEFORE_SWAP = "bluetoothNameBeforeSwap";
    private static final String KEY_WIFI_ENABLED_BEFORE_SWAP = "wifiEnabledBeforeSwap";

    @NonNull
    private final Set<String> appsToSwap = new HashSet<>();
    private final Set<Peer> activePeers = new HashSet<>();

    private static LocalBroadcastManager localBroadcastManager;
    private static SharedPreferences swapPreferences;
    private static BluetoothAdapter bluetoothAdapter;
    private static WifiManager wifiManager;
    private static Timer pollConnectedSwapRepoTimer;

    public static void start(Context context) {
        Intent intent = new Intent(context, SwapService.class);
        if (Build.VERSION.SDK_INT < 26) {
            context.startService(intent);
        } else {
            context.startForegroundService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, SwapService.class);
        context.stopService(intent);
    }

    @NonNull
    public Set<String> getAppsToSwap() {
        return appsToSwap;
    }

    @NonNull
    public Set<Peer> getActivePeers() {
        return activePeers;
    }

    public void connectToPeer() {
        if (getPeer() == null) {
            throw new IllegalStateException("Cannot connect to peer, no peer has been selected.");
        }
        connectTo(getPeer());
        if (LocalHTTPDManager.isAlive() && getPeer().shouldPromptForSwapBack()) {
            askServerToSwapWithUs(peerRepo);
        }
    }

    public void connectTo(@NonNull Peer peer) {
        if (peer != this.peer) {
            Log.e(TAG, "Oops, got a different peer to swap with than initially planned.");
        }
        peerRepo = ensureRepoExists(peer);
        UpdateService.updateRepoNow(this, peer.getRepoAddress());
    }

    @SuppressLint("StaticFieldLeak")
    private void askServerToSwapWithUs(final Repo repo) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                String swapBackUri = Utils.getLocalRepoUri(FDroidApp.repo).toString();
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(repo.address.replace("/fdroid/repo", "/request-swap"));
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    OutputStream outputStream = conn.getOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                    writer.write("repo=" + swapBackUri);
                    writer.flush();
                    writer.close();
                    outputStream.close();

                    int responseCode = conn.getResponseCode();
                    Utils.debugLog(TAG, "Asking server at " + repo.address + " to swap with us in return (by " +
                            "POSTing to \"/request-swap\" with repo \"" + swapBackUri + "\"): " + responseCode);
                } catch (IOException e) {
                    Log.e(TAG, "Error while asking server to swap with us", e);
                    Intent intent = new Intent(Downloader.ACTION_INTERRUPTED);
                    intent.setData(Uri.parse(repo.address));
                    intent.putExtra(Downloader.EXTRA_ERROR_MESSAGE, e.getLocalizedMessage());
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
                return null;
            }
        }.execute();
    }

    private Repo ensureRepoExists(@NonNull Peer peer) {
        // TODO: newRepoConfig.getParsedUri() will include a fingerprint, which may not match with
        // the repos address in the database. Not sure on best behaviour in this situation.
        Repo repo = RepoProvider.Helper.findByAddress(this, peer.getRepoAddress());
        if (repo == null) {
            ContentValues values = new ContentValues(6);

            // The name/description is not really required, as swap repos are not shown in the
            // "Manage repos" UI on other device. Doesn't hurt to put something there though,
            // on the off chance that somebody is looking through the sqlite database which
            // contains the repos...
            values.put(Schema.RepoTable.Cols.NAME, peer.getName());
            values.put(Schema.RepoTable.Cols.ADDRESS, peer.getRepoAddress());
            values.put(Schema.RepoTable.Cols.DESCRIPTION, "");
            String fingerprint = peer.getFingerprint();
            if (!TextUtils.isEmpty(fingerprint)) {
                values.put(Schema.RepoTable.Cols.FINGERPRINT, peer.getFingerprint());
            }
            values.put(Schema.RepoTable.Cols.IN_USE, 1);
            values.put(Schema.RepoTable.Cols.IS_SWAP, true);
            Uri uri = RepoProvider.Helper.insert(this, values);
            repo = RepoProvider.Helper.get(this, uri);
        }

        return repo;
    }

    @Nullable
    public Repo getPeerRepo() {
        return peerRepo;
    }

    // =================================================
    //    Have selected a specific peer to swap with
    //  (Rather than showing a generic QR code to scan)
    // =================================================

    @Nullable
    private Peer peer;

    @Nullable
    private Repo peerRepo;

    public void swapWith(Peer peer) {
        this.peer = peer;
    }

    public void addCurrentPeerToActive() {
        activePeers.add(peer);
    }

    public void removeCurrentPeerFromActive() {
        activePeers.remove(peer);
    }

    public boolean isConnectingWithPeer() {
        return peer != null;
    }

    @Nullable
    public Peer getPeer() {
        return peer;
    }

    // ==========================================
    //      Remember apps user wants to swap
    // ==========================================

    private void persistAppsToSwap() {
        swapPreferences.edit().putString(KEY_APPS_TO_SWAP, serializePackages(appsToSwap)).apply();
    }

    /**
     * Replacement for {@link android.content.SharedPreferences.Editor#putStringSet(String, Set)}
     * which is only available in API >= 11.
     * Package names are reverse-DNS-style, so they should only have alpha numeric values. Thus,
     * this uses a comma as the separator.
     *
     * @see SwapService#deserializePackages(String)
     */
    private static String serializePackages(Set<String> packages) {
        StringBuilder sb = new StringBuilder();
        for (String pkg : packages) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(pkg);
        }
        return sb.toString();
    }

    /**
     * @see SwapService#deserializePackages(String)
     */
    private static Set<String> deserializePackages(String packages) {
        Set<String> set = new HashSet<>();
        if (!TextUtils.isEmpty(packages)) {
            Collections.addAll(set, packages.split(","));
        }
        return set;
    }

    public void ensureFDroidSelected() {
        String fdroid = getPackageName();
        if (!hasSelectedPackage(fdroid)) {
            selectPackage(fdroid);
        }
    }

    public boolean hasSelectedPackage(String packageName) {
        return appsToSwap.contains(packageName);
    }

    public void selectPackage(String packageName) {
        appsToSwap.add(packageName);
        persistAppsToSwap();
    }

    public void deselectPackage(String packageName) {
        if (appsToSwap.contains(packageName)) {
            appsToSwap.remove(packageName);
        }
        persistAppsToSwap();
    }

    public static boolean getBluetoothVisibleUserPreference() {
        return swapPreferences.getBoolean(SwapService.KEY_BLUETOOTH_ENABLED, false);
    }

    public static void putBluetoothVisibleUserPreference(boolean visible) {
        swapPreferences.edit().putBoolean(SwapService.KEY_BLUETOOTH_ENABLED, visible).apply();
    }

    public static boolean getWifiVisibleUserPreference() {
        return swapPreferences.getBoolean(SwapService.KEY_WIFI_ENABLED, false);
    }

    public static void putWifiVisibleUserPreference(boolean visible) {
        swapPreferences.edit().putBoolean(SwapService.KEY_WIFI_ENABLED, visible).apply();
    }

    public static boolean wasBluetoothEnabledBeforeSwap() {
        return swapPreferences.getBoolean(SwapService.KEY_BLUETOOTH_ENABLED_BEFORE_SWAP, false);
    }

    public static void putBluetoothEnabledBeforeSwap(boolean visible) {
        swapPreferences.edit().putBoolean(SwapService.KEY_BLUETOOTH_ENABLED_BEFORE_SWAP, visible).apply();
    }

    public static String getBluetoothNameBeforeSwap() {
        return swapPreferences.getString(SwapService.KEY_BLUETOOTH_NAME_BEFORE_SWAP, null);
    }

    public static void putBluetoothNameBeforeSwap(String name) {
        swapPreferences.edit().putString(SwapService.KEY_BLUETOOTH_NAME_BEFORE_SWAP, name).apply();
    }

    public static boolean wasWifiEnabledBeforeSwap() {
        return swapPreferences.getBoolean(SwapService.KEY_WIFI_ENABLED_BEFORE_SWAP, false);
    }

    public static void putWifiEnabledBeforeSwap(boolean visible) {
        swapPreferences.edit().putBoolean(SwapService.KEY_WIFI_ENABLED_BEFORE_SWAP, visible).apply();
    }

    private static final int NOTIFICATION = 1;

    private final Binder binder = new Binder();

    private static final int TIMEOUT = 15 * 60 * 1000; // 15 mins

    /**
     * Used to automatically turn of swapping after a defined amount of time (15 mins).
     */
    @Nullable
    private Timer timer;

    public class Binder extends android.os.Binder {
        public SwapService getService() {
            return SwapService.this;
        }
    }

    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION, createNotification());
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        swapPreferences = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);

        LocalHTTPDManager.start(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            SwapService.putBluetoothEnabledBeforeSwap(bluetoothAdapter.isEnabled());
            if (bluetoothAdapter.isEnabled()) {
                BluetoothManager.start(this);
            }
            registerReceiver(bluetoothScanModeChanged,
                    new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        }

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            SwapService.putWifiEnabledBeforeSwap(wifiManager.isWifiEnabled());
        }

        appsToSwap.addAll(deserializePackages(swapPreferences.getString(KEY_APPS_TO_SWAP, "")));

        Preferences.get().registerLocalRepoHttpsListeners(httpsEnabledListener);

        localBroadcastManager.registerReceiver(onWifiChange, new IntentFilter(WifiStateChangeService.BROADCAST));
        localBroadcastManager.registerReceiver(bluetoothStatus, new IntentFilter(BluetoothManager.ACTION_STATUS));
        localBroadcastManager.registerReceiver(bluetoothPeerFound, new IntentFilter(BluetoothManager.ACTION_FOUND));
        localBroadcastManager.registerReceiver(bonjourPeerFound, new IntentFilter(BonjourManager.ACTION_FOUND));
        localBroadcastManager.registerReceiver(bonjourPeerRemoved, new IntentFilter(BonjourManager.ACTION_REMOVED));
        localBroadcastManager.registerReceiver(localRepoStatus, new IntentFilter(LocalRepoService.ACTION_STATUS));

        BonjourManager.start(this);
        BonjourManager.setVisible(this, getWifiVisibleUserPreference());
    }

    /**
     * This is for setting things up for when the {@code SwapService} was
     * started by the user clicking on the initial start button. The things
     * that must be run always on start-up go in {@link #onCreate()}.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        deleteAllSwapRepos();
        startActivity(new Intent(this, SwapWorkflowActivity.class));
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // reset the timer on each new connect, the user has come back
        initTimer();
        return binder;
    }

    @Override
    public void onDestroy() {
        Utils.debugLog(TAG, "Destroying service, will disable swapping if required, and unregister listeners.");
        Preferences.get().unregisterLocalRepoHttpsListeners(httpsEnabledListener);
        localBroadcastManager.unregisterReceiver(onWifiChange);
        localBroadcastManager.unregisterReceiver(bluetoothStatus);
        localBroadcastManager.unregisterReceiver(bluetoothPeerFound);
        localBroadcastManager.unregisterReceiver(bonjourPeerFound);
        localBroadcastManager.unregisterReceiver(bonjourPeerRemoved);

        unregisterReceiver(bluetoothScanModeChanged);

        BluetoothManager.stop(this);

        BonjourManager.stop(this);
        LocalHTTPDManager.stop(this);
        if (wifiManager != null && !wasWifiEnabledBeforeSwap()) {
            wifiManager.setWifiEnabled(false);
        }

        stopPollingConnectedSwapRepo();

        if (timer != null) {
            timer.cancel();
        }
        stopForeground(true);

        deleteAllSwapRepos();

        super.onDestroy();
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, SwapWorkflowActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        return new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.local_repo_running))
                .setContentText(getText(R.string.touch_to_configure_local_repo))
                .setSmallIcon(R.drawable.ic_nearby)
                .setContentIntent(contentIntent)
                .build();
    }

    /**
     * For now, swap repos are only trusted as long as swapping is active.  They
     * should have a long lived trust based on the signing key, but that requires
     * that the repos are stored in the database by fingerprint, not by URL address.
     *
     * @see <a href="https://gitlab.com/fdroid/fdroidclient/issues/295">TOFU in swap</a>
     * @see <a href="https://gitlab.com/fdroid/fdroidclient/issues/703">
     * signing key fingerprint should be sole ID for repos in the database</a>
     */
    private void deleteAllSwapRepos() {
        for (Repo repo : RepoProvider.Helper.all(this)) {
            if (repo.isSwap) {
                Utils.debugLog(TAG, "Removing stale swap repo: " + repo.address + " - " + repo.fingerprint);
                RepoProvider.Helper.remove(this, repo.getId());
            }
        }
    }

    private void startPollingConnectedSwapRepo() {
        stopPollingConnectedSwapRepo();
        pollConnectedSwapRepoTimer = new Timer("pollConnectedSwapRepoTimer", true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (peer != null) {
                    connectTo(peer);
                }
            }
        };
        pollConnectedSwapRepoTimer.schedule(timerTask, 5000);
    }

    public void stopPollingConnectedSwapRepo() {
        if (pollConnectedSwapRepoTimer != null) {
            pollConnectedSwapRepoTimer.cancel();
            pollConnectedSwapRepoTimer = null;
        }
    }

    /**
     * Sets or resets the idel timer for {@link #TIMEOUT}ms, once the timer
     * expires, this service and all things that rely on it will be stopped.
     */
    public void initTimer() {
        if (timer != null) {
            Utils.debugLog(TAG, "Cancelling existing timeout timer so timeout can be reset.");
            timer.cancel();
        }

        Utils.debugLog(TAG, "Initializing swap timeout to " + TIMEOUT + "ms minutes");
        timer = new Timer(TAG, true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Utils.debugLog(TAG, "Disabling swap because " + TIMEOUT + "ms passed.");
                String msg = getString(R.string.swap_toast_closing_nearby_after_timeout);
                Utils.showToastFromService(SwapService.this, msg, android.widget.Toast.LENGTH_LONG);
                stop(SwapService.this);
            }
        }, TIMEOUT);
    }

    private void restartWiFiServices() {
        boolean hasIp = FDroidApp.ipAddressString != null;
        if (hasIp) {
            LocalHTTPDManager.restart(this);
            BonjourManager.restart(this);
            BonjourManager.setVisible(this, getWifiVisibleUserPreference());
        } else {
            BonjourManager.stop(this);
            LocalHTTPDManager.stop(this);
        }
    }

    private final Preferences.ChangeListener httpsEnabledListener = new Preferences.ChangeListener() {
        @Override
        public void onPreferenceChange() {
            restartWiFiServices();
        }
    };

    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            restartWiFiServices();
        }
    };

    private final BroadcastReceiver bluetoothStatus = new SwapStateChangeReceiver();
    private final BroadcastReceiver localRepoStatus = new SwapStateChangeReceiver();

    /**
     * When swapping is setup, then start the index polling.
     */
    private class SwapStateChangeReceiver extends BroadcastReceiver {
        private final BroadcastReceiver pollForUpdatesReceiver = new PollForUpdatesReceiver();

        @Override
        public void onReceive(Context context, Intent intent) {
            int bluetoothStatus = intent.getIntExtra(BluetoothManager.ACTION_STATUS, -1);
            int wifiStatus = intent.getIntExtra(LocalRepoService.EXTRA_STATUS, -1);
            if (bluetoothStatus == BluetoothManager.STATUS_STARTED
                    || wifiStatus == LocalRepoService.STATUS_STARTED) {
                localBroadcastManager.registerReceiver(pollForUpdatesReceiver,
                        new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));
            } else {
                localBroadcastManager.unregisterReceiver(pollForUpdatesReceiver);
            }
        }
    }

    /**
     * Reschedule an index update if the last one was successful.
     */
    private class PollForUpdatesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, -1)) {
                case UpdateService.STATUS_COMPLETE_AND_SAME:
                case UpdateService.STATUS_COMPLETE_WITH_CHANGES:
                    startPollingConnectedSwapRepo();
                    break;
            }
        }
    }

    /**
     * Handle events if the user or system changes the Bluetooth setup outside of F-Droid.
     */
    private final BroadcastReceiver bluetoothScanModeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)) {
                case BluetoothAdapter.SCAN_MODE_NONE:
                    BluetoothManager.stop(SwapService.this);
                    break;

                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                    BluetoothManager.start(SwapService.this);
                    break;
            }
        }
    };

    private final BroadcastReceiver bluetoothPeerFound = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            activePeers.add((Peer) intent.getParcelableExtra(BluetoothManager.EXTRA_PEER));
        }
    };

    private final BroadcastReceiver bonjourPeerFound = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            activePeers.add((Peer) intent.getParcelableExtra(BonjourManager.EXTRA_BONJOUR_PEER));
        }
    };

    private final BroadcastReceiver bonjourPeerRemoved = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            activePeers.remove((Peer) intent.getParcelableExtra(BonjourManager.EXTRA_BONJOUR_PEER));
        }
    };
}