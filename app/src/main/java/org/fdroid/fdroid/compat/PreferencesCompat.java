package org.fdroid.fdroid.compat;

import android.content.SharedPreferences;
import android.os.Build;

public class PreferencesCompat {

    public static void apply(SharedPreferences.Editor e) {
        if (Build.VERSION.SDK_INT < 9) {
            e.commit();
        } else {
            e.apply();
        }
    }
}
