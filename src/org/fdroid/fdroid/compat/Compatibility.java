package org.fdroid.fdroid.compat;

import android.os.Build;

import org.fdroid.fdroid.Utils;

public abstract class Compatibility {

    protected static boolean hasApi(int apiLevel) {
        return getApi() >= apiLevel;
    }

    protected static int getApi() {
        return Build.VERSION.SDK_INT;
    }

}
