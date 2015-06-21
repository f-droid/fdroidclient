package org.fdroid.fdroid.localrepo;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SwapManager {

    private static final String TAG = "SwapState";
    private static final String SHARED_PREFERENCES = "swap-state";
    private static final String KEY_APPS_TO_SWAP = "appsToSwap";

    private static SwapManager instance;

    @NonNull
    public static SwapManager load(@NonNull Context context) {
        if (instance == null) {
            SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            Set<String> appsToSwap = deserializePackages(preferences.getString(KEY_APPS_TO_SWAP, ""));
            instance = new SwapManager(context, appsToSwap);
        }

        return instance;
    }

    @NonNull
    private final Context context;

    @NonNull
    private Set<String> appsToSwap;

    private SwapManager(@NonNull Context context, @NonNull Set<String> appsToSwap) {
        this.context = context.getApplicationContext();
        this.appsToSwap = appsToSwap;

        setupService();
    }

    /**
     * Where relevant, the state of the swap process will be saved to disk using preferences.
     * Note that this is not always useful, for example saving the "current wifi network" is
     * bound to cause trouble when the user opens the swap process again and is connected to
     * a different network.
     */
    private SharedPreferences persistence() {
        return context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_APPEND);
    }

    // ==========================================================
    //                 Manage the current step
    // ("Step" refers to the current view being shown in the UI)
    // ==========================================================

    public static final int STEP_INTRO       = 1;
    public static final int STEP_SELECT_APPS = 2;
    public static final int STEP_JOIN_WIFI   = 3;
    public static final int STEP_SHOW_NFC    = 4;
    public static final int STEP_WIFI_QR     = 5;

    private @SwapStep int step = STEP_INTRO;

    /**
     * Current screen that the swap process is up to.
     * Will be one of the SwapState.STEP_* values.
     */
    @SwapStep
    public int getStep() {
        return step;
    }

    public SwapManager setStep(@SwapStep int step) {
        this.step = step;
        return this;
    }

    public @NonNull Set<String> getAppsToSwap() {
        return appsToSwap;
    }

    /**
     * Ensure that we don't get put into an incorrect state, by forcing people to pass valid
     * states to setStep. Ideally this would be done by requiring an enum or something to
     * be passed rather than in integer, however that is harder to persist on disk than an int.
     * This is the same as, e.g. {@link Context#getSystemService(String)}
     */
    @IntDef({STEP_INTRO, STEP_SELECT_APPS, STEP_JOIN_WIFI, STEP_SHOW_NFC, STEP_WIFI_QR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SwapStep {}


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
     * @see SwapManager#deserializePackages(String)
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
     * @see SwapManager#deserializePackages(String)
     */
    private static Set<String> deserializePackages(String packages) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, packages.split(","));
        return set;
    }

    public void ensureFDroidSelected() {
        String fdroid = context.getPackageName();
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


    // ==========================================
    //   Local repo stop/start/restart handling
    // ==========================================

    @Nullable
    private SwapService service = null;

    private void setupService() {

        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder binder) {
                Log.d(TAG, "Swap service connected, enabling SwapManager to communicate with SwapService.");
                service = ((SwapService.Binder)binder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Log.d(TAG, "Swap service disconnected");
                service = null;
            }
        };

        // The server should not be doing anything or occupying any (noticable) resources
        // until we actually ask it to enable swapping. Therefore, we will start it nice and
        // early so we don't have to wait until it is connected later.
        Intent service = new Intent(context, SwapService.class);
        if (context.bindService(service, serviceConnection, Context.BIND_AUTO_CREATE)) {
            context.startService(service);
        }

    }

    public void enableSwapping() {
        if (service != null) {
            service.enableSwapping();
        } else {
            Log.e(TAG, "Couldn't enable swap, because service was not running.");
        }
    }

    public void disableSwapping() {
        if (service != null) {
            service.disableSwapping();
        } else {
            Log.e(TAG, "Couldn't disable swap, because service was not running.");
        }
        setStep(STEP_INTRO);
    }

    /**
     * Handles checking if the {@link SwapService} is running, and only restarts it if it was running.
     */
    public void restartIfEnabled() {
        if (service != null) {
            service.restartIfEnabled();
        }
    }

    public boolean isEnabled() {
        return service != null && service.isEnabled();
    }

    // ==========================================
    //    Interacting with Bluetooth adapter
    // ==========================================

    @Nullable /* Emulators tend not to have bluetooth adapters. */
    private final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

    public boolean isBluetoothDiscoverable() {
        return bluetooth != null &&
                bluetooth.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
    }

    public void ensureBluetoothDiscoverable() {

        if (bluetooth == null) {
            return;
        }

        if (!bluetooth.isEnabled()) {
            if (!bluetooth.enable()) {

            }
        }

        if (bluetooth.isEnabled()) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            context.startActivity(discoverableIntent);
        }

    }

}
