package org.fdroid.fdroid.localrepo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.localrepo.peers.Peer;
import org.fdroid.fdroid.localrepo.peers.PeerFinder;
import org.fdroid.fdroid.localrepo.type.BluetoothSwap;
import org.fdroid.fdroid.localrepo.type.SwapType;
import org.fdroid.fdroid.localrepo.type.WifiSwap;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

    @NonNull
    private final Set<String> appsToSwap = new HashSet<>();

    /**
     * A cache of parsed APKs from the file system.
     */
    private static final ConcurrentHashMap<String, App> INSTALLED_APPS = new ConcurrentHashMap<>();

    public static void stop(Context context) {
        Intent intent = new Intent(context, SwapService.class);
        context.stopService(intent);
    }

    static App getAppFromCache(String packageName) {
        return INSTALLED_APPS.get(packageName);
    }

    static void putAppInCache(String packageName, App app) {
        INSTALLED_APPS.put(packageName, app);
    }

    /**
     * Where relevant, the state of the swap process will be saved to disk using preferences.
     * Note that this is not always useful, for example saving the "current wifi network" is
     * bound to cause trouble when the user opens the swap process again and is connected to
     * a different network.
     */
    private SharedPreferences persistence() {
        return getSharedPreferences(SHARED_PREFERENCES, MODE_APPEND);
    }

    // ==========================================================
    //                 Search for peers to swap
    // ==========================================================

    private Observable<Peer> peerFinder;

    /**
     * Call {@link Observable#subscribe()} on this in order to be notified of peers
     * which are found. Call {@link Subscription#unsubscribe()} on the resulting
     * subscription when finished and you no longer want to scan for peers.
     *
     * The returned object will scan for peers on a background thread, and emit
     * found peers on the mian thread.
     *
     * Invoking this in multiple places will return the same, cached, peer finder.
     * That is, if in the past it already found some peers, then you subscribe
     * to it in the future, the future subscriber will still receive the peers
     * that were found previously.
     * TODO: What about removing peers that no longer are present?
     */
    public Observable<Peer> scanForPeers() {
        Utils.debugLog(TAG, "Scanning for nearby devices to swap with...");
        if (peerFinder == null) {
            peerFinder = PeerFinder.createObservable(getApplicationContext())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .distinct();
        }
        return peerFinder;
    }

    // ==========================================================
    //                 Manage the current step
    // ("Step" refers to the current view being shown in the UI)
    // ==========================================================

    public static final int STEP_INTRO           = 1;
    public static final int STEP_SELECT_APPS     = 2;
    public static final int STEP_JOIN_WIFI       = 3;
    public static final int STEP_SHOW_NFC        = 4;
    public static final int STEP_WIFI_QR         = 5;
    public static final int STEP_CONNECTING      = 6;
    public static final int STEP_SUCCESS         = 7;
    public static final int STEP_CONFIRM_SWAP    = 8;

    /**
     * Special view, that we don't really want to actually store against the
     * {@link SwapService#step}. Rather, we use it for the purpose of specifying
     * we are in the state waiting for the {@link SwapService} to get started and
     * bound to the {@link SwapWorkflowActivity}.
     */
    public static final int STEP_INITIAL_LOADING = 9;

    @SwapStep private int step = STEP_INTRO;

    /**
     * Current screen that the swap process is up to.
     * Will be one of the SwapState.STEP_* values.
     */
    @SwapStep
    public int getStep() {
        return step;
    }

    public SwapService setStep(@SwapStep int step) {
        this.step = step;
        return this;
    }

    @NonNull public Set<String> getAppsToSwap() {
        return appsToSwap;
    }

    public void refreshSwap() {
        if (peer != null) {
            connectTo(peer, false);
        }
    }

    public void connectToPeer() {
        if (getPeer() == null) {
            throw new IllegalStateException("Cannot connect to peer, no peer has been selected.");
        }
        connectTo(getPeer(), getPeer().shouldPromptForSwapBack());
    }

    public void connectTo(@NonNull Peer peer, boolean requestSwapBack) {
        if (peer != this.peer) {
            Log.e(TAG, "Oops, got a different peer to swap with than initially planned.");
        }

        peerRepo = ensureRepoExists(peer);

        // Only ask server to swap with us, if we are actually running a local repo service.
        // It is possible to have a swap initiated without first starting a swap, in which
        // case swapping back is pointless.
        if (isEnabled() && requestSwapBack) {
            askServerToSwapWithUs(peerRepo);
        }

        UpdateService.updateRepoNow(peer.getRepoAddress(), this);
    }

    private void askServerToSwapWithUs(final Repo repo) {
        askServerToSwapWithUs(repo.address);
    }

    private void askServerToSwapWithUs(final String address) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                Uri repoUri = Uri.parse(address);
                String swapBackUri = Utils.getLocalRepoUri(FDroidApp.repo).toString();

                AndroidHttpClient client = AndroidHttpClient.newInstance("F-Droid", SwapService.this);
                HttpPost request = new HttpPost("/request-swap");
                HttpHost host = new HttpHost(repoUri.getHost(), repoUri.getPort(), repoUri.getScheme());

                try {
                    Utils.debugLog(TAG, "Asking server at " + address + " to swap with us in return (by POSTing to \"/request-swap\" with repo \"" + swapBackUri + "\")...");
                    populatePostParams(swapBackUri, request);
                    client.execute(host, request);
                } catch (IOException e) {
                    notifyOfErrorOnUiThread();
                    Log.e(TAG, "Error while asking server to swap with us", e);
                } finally {
                    client.close();
                }
                return null;
            }

            private void populatePostParams(String swapBackUri, HttpPost request) throws UnsupportedEncodingException {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("repo", swapBackUri));
                UrlEncodedFormEntity encodedParams = new UrlEncodedFormEntity(params);
                request.setEntity(encodedParams);
            }

            private void notifyOfErrorOnUiThread() {
                // TODO: Broadcast error message so that whoever wants to can display a relevant
                // message in the UI. This service doesn't understand the concept of UI.
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(
                                SwapService.this,
                                R.string.swap_reciprocate_failed,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });*/
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
            values.put(Schema.RepoTable.Cols.IN_USE, true);
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

    /**
     * Ensure that we don't get put into an incorrect state, by forcing people to pass valid
     * states to setStep. Ideally this would be done by requiring an enum or something to
     * be passed rather than in integer, however that is harder to persist on disk than an int.
     * This is the same as, e.g. {@link Context#getSystemService(String)}
     */
    @IntDef({STEP_INTRO, STEP_SELECT_APPS, STEP_JOIN_WIFI, STEP_SHOW_NFC, STEP_WIFI_QR,
        STEP_CONNECTING, STEP_SUCCESS, STEP_CONFIRM_SWAP, STEP_INITIAL_LOADING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SwapStep { }

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
        persistence().edit().putString(KEY_APPS_TO_SWAP, serializePackages(appsToSwap)).commit();
    }

    /**
     * Replacement for {@link android.content.SharedPreferences.Editor#putStringSet(String, Set)}
     * which is only available in API >= 11.
     * Package names are reverse-DNS-style, so they should only have alpha numeric values. Thus,
     * this uses a comma as the separator.
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

    // =============================================================
    //   Remember which swap technologies a user used in the past
    // =============================================================

    private final BroadcastReceiver receiveSwapStatusChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.debugLog(TAG, "Remembering that Bluetooth swap " + (bluetoothSwap.isConnected() ? "IS" : "is NOT") +
                    " connected and WiFi swap " + (wifiSwap.isConnected() ? "IS" : "is NOT") + " connected.");
            persistence().edit()
                    .putBoolean(KEY_BLUETOOTH_ENABLED, bluetoothSwap.isConnected())
                    .putBoolean(KEY_WIFI_ENABLED, wifiSwap.isConnected())
                    .commit();
        }
    };

    /*
    private boolean wasBluetoothEnabled() {
        return persistence().getBoolean(KEY_BLUETOOTH_ENABLED, false);
    }
    */

    private boolean wasWifiEnabled() {
        return persistence().getBoolean(KEY_WIFI_ENABLED, false);
    }

    /**
     * Handles checking if the {@link SwapService} is running, and only restarts it if it was running.
     */
    public void stopWifiIfEnabled(final boolean restartAfterStopping) {
        if (wifiSwap.isConnected()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Utils.debugLog(TAG, "Stopping the currently running WiFi swap service (on background thread)");
                    wifiSwap.stop();

                    if (restartAfterStopping) {
                        Utils.debugLog(TAG, "Restarting WiFi swap service after stopping (still on background thread)");
                        wifiSwap.start();
                    }
                    return null;
                }
            }.execute();
        }
    }

    public boolean isEnabled() {
        return bluetoothSwap.isConnected() || wifiSwap.isConnected();
    }

    // ==========================================
    //    Interacting with Bluetooth adapter
    // ==========================================

    public boolean isBluetoothDiscoverable() {
        return bluetoothSwap.isDiscoverable();
    }

    public boolean isBonjourDiscoverable() {
        return wifiSwap.isConnected() && wifiSwap.getBonjour().isConnected();
    }

    public static final String ACTION_PEER_FOUND = "org.fdroid.fdroid.SwapManager.ACTION_PEER_FOUND";
    public static final String EXTRA_PEER = "EXTRA_PEER";

    // ===============================================================
    //        Old SwapService stuff being merged into that.
    // ===============================================================

    public static final String BONJOUR_STATE_CHANGE = "org.fdroid.fdroid.BONJOUR_STATE_CHANGE";
    public static final String BLUETOOTH_STATE_CHANGE = "org.fdroid.fdroid.BLUETOOTH_STATE_CHANGE";
    public static final String WIFI_STATE_CHANGE = "org.fdroid.fdroid.WIFI_STATE_CHANGE";
    public static final String EXTRA_STARTING = "STARTING";
    public static final String EXTRA_STARTED = "STARTED";
    public static final String EXTRA_STOPPING = "STOPPING";
    public static final String EXTRA_STOPPED = "STOPPED";

    private static final int NOTIFICATION = 1;

    private final Binder binder = new Binder();
    private SwapType bluetoothSwap;
    private WifiSwap wifiSwap;

    private static final int TIMEOUT = 15 * 60 * 1000; // 15 mins

    /**
     * Used to automatically turn of swapping after a defined amount of time (15 mins).
     */
    @Nullable
    private Timer timer;

    public SwapType getBluetoothSwap() {
        return bluetoothSwap;
    }

    public WifiSwap getWifiSwap() {
        return wifiSwap;
    }

    public class Binder extends android.os.Binder {
        public SwapService getService() {
            return SwapService.this;
        }
    }

    public void onCreate() {
        super.onCreate();

        Utils.debugLog(TAG, "Creating swap service.");
        startForeground(NOTIFICATION, createNotification());

        CacheSwapAppsService.startCaching(this);

        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);

        appsToSwap.addAll(deserializePackages(preferences.getString(KEY_APPS_TO_SWAP, "")));
        bluetoothSwap = BluetoothSwap.create(this);
        wifiSwap = new WifiSwap(this);

        Preferences.get().registerLocalRepoHttpsListeners(httpsEnabledListener);

        LocalBroadcastManager.getInstance(this).registerReceiver(onWifiChange, new IntentFilter(WifiStateChangeService.BROADCAST));

        IntentFilter filter = new IntentFilter(BLUETOOTH_STATE_CHANGE);
        filter.addAction(WIFI_STATE_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiveSwapStatusChanged, filter);

        /*
        if (wasBluetoothEnabled()) {
            Utils.debugLog(TAG, "Previously the user enabled Bluetooth swap, so enabling again automatically.");
            bluetoothSwap.startInBackground();
        }
        */

        if (wasWifiEnabled()) {
            Utils.debugLog(TAG, "Previously the user enabled WiFi swap, so enabling again automatically.");
            wifiSwap.startInBackground();
        } else {
            Utils.debugLog(TAG, "WiFi was NOT enabled last time user swapped, so starting with WiFi not visible.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onWifiChange);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiveSwapStatusChanged);

        //TODO getBluetoothSwap().stopInBackground();
        getWifiSwap().stopInBackground();

        if (timer != null) {
            timer.cancel();
        }
        stopForeground(true);

        super.onDestroy();
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, SwapWorkflowActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        return new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.local_repo_running))
                .setContentText(getText(R.string.touch_to_configure_local_repo))
                .setSmallIcon(R.drawable.ic_swap)
                .setContentIntent(contentIntent)
                .build();
    }

    private void initTimer() {
        if (timer != null) {
            Utils.debugLog(TAG, "Cancelling existing timeout timer so timeout can be reset.");
            timer.cancel();
        }

        Utils.debugLog(TAG, "Initializing swap timeout to " + TIMEOUT + "ms minutes");
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Utils.debugLog(TAG, "Disabling swap because " + TIMEOUT + "ms passed.");
                stop(SwapService.this);
            }
        }, TIMEOUT);
    }

    @SuppressWarnings("FieldCanBeLocal") // The constructor will get bloated if these are all local...
    private final Preferences.ChangeListener httpsEnabledListener = new Preferences.ChangeListener() {
        @Override
        public void onPreferenceChange() {
            Log.i(TAG, "Swap over HTTPS preference changed.");
            stopWifiIfEnabled(true);
        }
    };

    @SuppressWarnings("FieldCanBeLocal") // The constructor will get bloated if these are all local...
    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            boolean hasIp = FDroidApp.ipAddressString != null;
            stopWifiIfEnabled(hasIp);
        }
    };

}
