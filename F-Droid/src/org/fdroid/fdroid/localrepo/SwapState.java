package org.fdroid.fdroid.localrepo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SwapState {

    private static final String SHARED_PREFERENCES = "swap-state";

    private static final String KEY_STEP         = "step";
    private static final String KEY_APPS_TO_SWAP = "appsToSwap";

    private static SwapState instance;

    @NonNull
    public static SwapState load(@NonNull Context context) {
        if (instance == null) {
            SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);

            @SwapStep int step = preferences.getInt(KEY_STEP, STEP_INTRO);
            Set<String> appsToSwap = deserializePackages(preferences.getString(KEY_APPS_TO_SWAP, ""));

            instance = new SwapState(context, step, appsToSwap);
        }

        return instance;
    }

    @NonNull
    private final Context context;

    @NonNull
    private Set<String> appsToSwap;

    private SwapState(@NonNull Context context, @SwapStep int step, @NonNull Set<String> appsToSwap) {
        this.context = context.getApplicationContext();
        this.step = step;
        this.appsToSwap = appsToSwap;
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

    private @SwapStep int step;

    /**
     * Current screen that the swap process is up to.
     * Will be one of the SwapState.STEP_* values.
     */
    @SwapStep
    public int getStep() {
        return step;
    }

    public SwapState setStep(@SwapStep int step) {
        this.step = step;
        persistStep();
        return this;
    }

    public @NonNull Set<String> getAppsToSwap() {
        return appsToSwap;
    }

    private void persistStep() {
        persistence().edit().putInt(KEY_STEP, step).commit();
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
     * @see SwapState#deserializePackages(String)
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
     * @see SwapState#deserializePackages(String)
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

    private Messenger localRepoServiceMessenger = null;
    private boolean localRepoServiceIsBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            localRepoServiceMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            localRepoServiceMessenger = null;
        }
    };

    public void startLocalRepoService() {
        if (!localRepoServiceIsBound) {
            Intent service = new Intent(context, LocalRepoService.class);
            localRepoServiceIsBound = context.bindService(service, serviceConnection, Context.BIND_AUTO_CREATE);
            if (localRepoServiceIsBound)
                context.startService(service);
        }
    }

    public void stopLocalRepoService() {
        if (localRepoServiceIsBound) {
            context.unbindService(serviceConnection);
            localRepoServiceIsBound = false;
        }
        context.stopService(new Intent(context, LocalRepoService.class));
    }

    /**
     * Handles checking if the {@link LocalRepoService} is running, and only restarts it if it was running.
     */
    public void restartLocalRepoServiceIfRunning() {
        if (localRepoServiceMessenger != null) {
            try {
                Message msg = Message.obtain(null, LocalRepoService.RESTART, LocalRepoService.RESTART, 0);
                localRepoServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isLocalRepoServiceRunning() {
        return localRepoServiceIsBound;
    }
}
