package org.fdroid.fdroid.compat;

import android.os.Build;

public abstract class Compatibility {

    // like minSdkVersion
    protected static boolean hasApi(int apiLevel) {
        return getApi() >= apiLevel;
    }

    // like maxSdkVersion
    protected static boolean upToApi(int apiLevel) {
        return (apiLevel < 1 || getApi() <= apiLevel);
    }

    protected static int getApi() {
        return Build.VERSION.SDK_INT;
    }

}
