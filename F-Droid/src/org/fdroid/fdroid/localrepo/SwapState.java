package org.fdroid.fdroid.localrepo;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SwapState {

    private static final String SHARED_PREFERENCES = "swap-state";

    public static final int STEP_INTRO       = 1;
    public static final int STEP_SELECT_APPS = 2;
    public static final int STEP_JOIN_WIFI   = 3;
    public static final int STEP_SHOW_NFC    = 4;
    public static final int STEP_WIFI_QR     = 5;


    @NonNull
    private final Context context;

    @NonNull
    private Set<String> appsToSwap;

    private int step;

    private SwapState(@NonNull Context context, @SwapStep int step, @NonNull Set<String> appsToSwap) {
        this.context = context;
        this.step = step;
        this.appsToSwap = appsToSwap;
    }

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

    public Set<String> getAppsToSwap() {
        return appsToSwap;
    }

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

    private SharedPreferences persistence() {
        return context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_APPEND);
    }

    private void persistStep() {
        persistence().edit().putInt(KEY_STEP, step).commit();
    }

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

    /**
     * Ensure that we don't get put into an incorrect state, by forcing people to pass valid
     * states to setStep. Ideally this would be done by requiring an enum or something to
     * be passed rather than in integer, however that is harder to persist on disk than an int.
     * This is the same as, e.g. {@link Context#getSystemService(String)}
     */
    @IntDef({STEP_INTRO, STEP_SELECT_APPS, STEP_JOIN_WIFI, STEP_SHOW_NFC, STEP_WIFI_QR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SwapStep {}
}
