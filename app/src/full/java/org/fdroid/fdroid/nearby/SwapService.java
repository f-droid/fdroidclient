package org.fdroid.fdroid.nearby;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.database.Repository;
import org.fdroid.download.Downloader;
import org.fdroid.download.NotFoundException;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NotificationHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.nearby.peers.Peer;
import org.fdroid.fdroid.net.DownloaderFactory;
import org.fdroid.fdroid.net.DownloaderService;
import org.fdroid.index.IndexParser;
import org.fdroid.index.IndexParserKt;
import org.fdroid.index.SigningException;
import org.fdroid.index.v1.IndexV1;
import org.fdroid.index.v1.IndexV1UpdaterKt;
import org.fdroid.index.v1.IndexV1Verifier;
import org.fdroid.index.v2.FileV2;

import java.io.File;
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

import cc.mvdan.accesspoint.WifiApControl;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Central service which manages all of the different moving parts of swap
 * which are required to enable p2p swapping of apps.  This is the background
 * operations for {@link SwapWorkflowActivity}.
 */
public class SwapService extends Service {
    private static final String TAG = "SwapService";

    private static final String SHARED_PREFERENCES = "swap-state";
    private static final String KEY_APPS_TO_SWAP = "appsToSwap";
    private static final String KEY_BLUETOOTH_ENABLED = "bluetoothEnabled";
    private static final String KEY_WIFI_ENABLED = "wifiEnabled";
    private static final String KEY_HOTSPOT_ACTIVATED = "hotspotEnabled";
    private static final String KEY_BLUETOOTH_ENABLED_BEFORE_SWAP = "bluetoothEnabledBeforeSwap";
    private static final String KEY_BLUETOOTH_NAME_BEFORE_SWAP = "bluetoothNameBeforeSwap";
    private static final String KEY_WIFI_ENABLED_BEFORE_SWAP = "wifiEnabledBeforeSwap";
    private static final String KEY_HOTSPOT_ACTIVATED_BEFORE_SWAP = "hotspotEnabledBeforeSwap";

    @NonNull
    private final Set<String> appsToSwap = new HashSet<>();
    private final Set<Peer> activePeers = new HashSet<>();
    private final MutableLiveData<IndexV1> index = new MutableLiveData<>();
    private final MutableLiveData<Exception> indexError = new MutableLiveData<>();

    private static LocalBroadcastManager localBroadcastManager;
    private static SharedPreferences swapPreferences;
    private static BluetoothAdapter bluetoothAdapter;
    private static WifiManager wifiManager;
    private static Timer pollConnectedSwapRepoTimer;

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

    private void connectTo(@NonNull Peer peer) {
        if (peer != this.peer) {
            Log.e(TAG, "Oops, got a different peer to swap with than initially planned.");
        }
        peerRepo = FDroidApp.createSwapRepo(peer.getRepoAddress(), null);
        try {
            updateRepo(peer, peerRepo);
        } catch (Exception e) {
            Log.e(TAG, "Error updating repo.", e);
            indexError.postValue(e);
        }
    }

    /**
     * {@code swapJarFile} is a path where the downloaded data will be written
     * to, but this method will not delete it afterwards.
     */
    public static IndexV1 getVerifiedRepoIndex(Repository repo, String expectedSigningFingerprint, File swapJarFile)
            throws SigningException, IOException, NotFoundException, InterruptedException {
        Uri uri = Uri.parse(repo.getAddress())
                .buildUpon()
                .appendPath(IndexV1UpdaterKt.SIGNED_FILE_NAME)
                .build();
        FileV2 indexFile = FileV2.fromPath("/" + IndexV1UpdaterKt.SIGNED_FILE_NAME);
        Downloader downloader =
                DownloaderFactory.INSTANCE.createWithTryFirstMirror(repo, uri, indexFile, swapJarFile);
        downloader.download();
        IndexV1Verifier verifier = new IndexV1Verifier(swapJarFile, null, expectedSigningFingerprint);
        return verifier.getStreamAndVerify(inputStream ->
                IndexParserKt.parseV1(IndexParser.INSTANCE, inputStream)
        ).getSecond();
    }

    private void updateRepo(@NonNull Peer peer, Repository repo)
            throws IOException, InterruptedException, SigningException, NotFoundException {
        File swapJarFile =
                File.createTempFile("swap", "", getApplicationContext().getCacheDir());
        try {
            index.postValue(getVerifiedRepoIndex(repo, peer.getFingerprint(), swapJarFile));
            startPollingConnectedSwapRepo();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            swapJarFile.delete();
        }
    }

    @Nullable
    public Repository getPeerRepo() {
        return peerRepo;
    }

    public LiveData<IndexV1> getIndex() {
        return index;
    }

    public LiveData<Exception> getIndexError() {
        return indexError;
    }

    // =================================================
    //    Have selected a specific peer to swap with
    //  (Rather than showing a generic QR code to scan)
    // =================================================

    @Nullable
    private Peer peer;

    @Nullable
    private Repository peerRepo;

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

    public static boolean getHotspotActivatedUserPreference() {
        return swapPreferences.getBoolean(SwapService.KEY_HOTSPOT_ACTIVATED, false);
    }

    public static void putHotspotActivatedUserPreference(boolean visible) {
        swapPreferences.edit().putBoolean(SwapService.KEY_HOTSPOT_ACTIVATED, visible).apply();
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

    public static boolean wasHotspotEnabledBeforeSwap() {
        return swapPreferences.getBoolean(SwapService.KEY_HOTSPOT_ACTIVATED_BEFORE_SWAP, false);
    }

    public static void putHotspotEnabledBeforeSwap(boolean visible) {
        swapPreferences.edit().putBoolean(SwapService.KEY_HOTSPOT_ACTIVATED_BEFORE_SWAP, visible).apply();
    }

    private static final int NOTIFICATION = 1;

    private final Binder binder = new Binder();

    private static final int TIMEOUT = 15 * 60 * 1000; // 15 mins

    /**
     * Used to automatically turn of swapping after a defined amount of time (15 mins).
     */
    @Nullable
    private Timer timer;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public class Binder extends android.os.Binder {
        public SwapService getService() {
            return SwapService.this;
        }
    }

    @Override
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

        wifiManager = ContextCompat.getSystemService(getApplicationContext(), WifiManager.class);
        if (wifiManager != null) {
            SwapService.putWifiEnabledBeforeSwap(wifiManager.isWifiEnabled());
        }

        appsToSwap.addAll(deserializePackages(swapPreferences.getString(KEY_APPS_TO_SWAP, "")));

        Preferences.get().registerLocalRepoHttpsListeners(httpsEnabledListener);

        localBroadcastManager.registerReceiver(onWifiChange, new IntentFilter(WifiStateChangeService.BROADCAST));
        localBroadcastManager.registerReceiver(bluetoothPeerFound, new IntentFilter(BluetoothManager.ACTION_FOUND));
        localBroadcastManager.registerReceiver(bonjourPeerFound, new IntentFilter(BonjourManager.ACTION_FOUND));
        localBroadcastManager.registerReceiver(bonjourPeerRemoved, new IntentFilter(BonjourManager.ACTION_REMOVED));

        if (getHotspotActivatedUserPreference()) {
            WifiApControl wifiApControl = WifiApControl.getInstance(this);
            if (wifiApControl != null) {
                wifiApControl.enable();
            }
        } else if (getWifiVisibleUserPreference()) {
            if (wifiManager != null) {
                wifiManager.setWifiEnabled(true);
            }
        }

        BonjourManager.start(this);
        BonjourManager.setVisible(this, getWifiVisibleUserPreference() || getHotspotActivatedUserPreference());
    }

    private void askServerToSwapWithUs(final Repository repo) {
        compositeDisposable.add(
                Completable.fromAction(() -> {
                            String swapBackUri = Utils.getLocalRepoUri(FDroidApp.repo).toString();
                            HttpURLConnection conn = null;
                            try {
                                URL url = new URL(repo.getAddress().replace("/fdroid/repo", "/request-swap"));
                                conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setDoInput(true);
                                conn.setDoOutput(true);

                                try (OutputStream outputStream = conn.getOutputStream();
                                     OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                                    writer.write("repo=" + swapBackUri);
                                    writer.flush();
                                }

                                int responseCode = conn.getResponseCode();
                                Utils.debugLog(TAG, "Asking server at " + repo.getAddress() + " to swap with us in return (by " +
                                        "POSTing to \"/request-swap\" with repo \"" + swapBackUri + "\"): " + responseCode);
                            } finally {
                                if (conn != null) {
                                    conn.disconnect();
                                }
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorComplete(e -> {
                            Intent intent = new Intent(DownloaderService.ACTION_INTERRUPTED);
                            intent.setData(Uri.parse(repo.getAddress()));
                            intent.putExtra(DownloaderService.EXTRA_ERROR_MESSAGE, e.getLocalizedMessage());
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                            return true;
                        })
                        .subscribe()
        );
    }

    /**
     * This is for setting things up for when the {@code SwapService} was
     * started by the user clicking on the initial start button. The things
     * that must be run always on start-up go in {@link #onCreate()}.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent startUiIntent = new Intent(this, SwapWorkflowActivity.class);
        startUiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startUiIntent);
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
        compositeDisposable.dispose();

        Utils.debugLog(TAG, "Destroying service, will disable swapping if required, and unregister listeners.");
        Preferences.get().unregisterLocalRepoHttpsListeners(httpsEnabledListener);
        localBroadcastManager.unregisterReceiver(onWifiChange);
        localBroadcastManager.unregisterReceiver(bluetoothPeerFound);
        localBroadcastManager.unregisterReceiver(bonjourPeerFound);
        localBroadcastManager.unregisterReceiver(bonjourPeerRemoved);

        if (bluetoothAdapter != null) {
            unregisterReceiver(bluetoothScanModeChanged);
        }

        BluetoothManager.stop(this);

        BonjourManager.stop(this);
        LocalHTTPDManager.stop(this);
        if (wifiManager != null && !wasWifiEnabledBeforeSwap()) {
            wifiManager.setWifiEnabled(false);
        }

        WifiApControl ap = WifiApControl.getInstance(this);
        if (ap != null) {
            try {
                if (wasHotspotEnabledBeforeSwap()) {
                    ap.enable();
                } else {
                    ap.disable();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        stopPollingConnectedSwapRepo();

        if (timer != null) {
            timer.cancel();
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);

        super.onDestroy();
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, SwapWorkflowActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SWAPS)
                .setContentTitle(getText(R.string.local_repo_running))
                .setContentText(getText(R.string.touch_to_configure_local_repo))
                .setSmallIcon(R.drawable.ic_nearby)
                .setContentIntent(contentIntent)
                .build();
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
     * Sets or resets the idle timer for {@link #TIMEOUT}ms, once the timer
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
            BonjourManager.setVisible(this, getWifiVisibleUserPreference() || getHotspotActivatedUserPreference());
        } else {
            BonjourManager.stop(this);
            LocalHTTPDManager.stop(this);
        }
    }

    private final Preferences.ChangeListener httpsEnabledListener = this::restartWiFiServices;

    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            restartWiFiServices();
        }
    };

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
