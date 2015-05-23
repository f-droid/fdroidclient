package org.fdroid.fdroid.localrepo;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SwapState {

    private static final String SHARED_PREFERENCES = "swap-state";

    public static final int STEP_INTRO       = 1;
    public static final int STEP_SELECT_APPS = 2;
    public static final int STEP_JOIN_WIFI   = 3;
    public static final int STEP_SHOW_NFC    = 4;
    public static final int STEP_WIFI_QR     = 5;

    private int step;

    @NonNull
    private final Context context;

    private SwapState(@NonNull Context context) {
        this.context = context;
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
        persist();
        return this;
    }

    private static final String KEY_STEP = "step";

    @NonNull
    public static SwapState load(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);

        @SwapStep int step = preferences.getInt(KEY_STEP, STEP_INTRO);

        return new SwapState(context)
                .setStep(step);
    }

    private void persist() {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_APPEND);
        preferences.edit()
                .putInt(KEY_STEP, step)
                .commit();
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
