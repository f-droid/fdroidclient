package org.fdroid.fdroid.compat;

import android.os.Build;

import java.util.Locale;

/**
 * @see <a href="https://developer.android.com/about/versions/oreo/android-8.0-changes#lai">use default DISPLAY category Locale</a>
 */
public class LocaleCompat {
    public static Locale getDefault() {
        if (Build.VERSION.SDK_INT >= 24) {
            return Locale.getDefault(java.util.Locale.Category.DISPLAY);
        } else {
            return Locale.getDefault();
        }
    }
}
