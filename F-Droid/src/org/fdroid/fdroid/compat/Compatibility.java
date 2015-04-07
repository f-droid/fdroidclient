package org.fdroid.fdroid.compat;

import android.os.Build;

public abstract class Compatibility {

    // like minSdkVersion
    protected static final boolean hasApi(int apiLevel) {
        return getApi() >= apiLevel;
    }

    // like maxSdkVersion
    protected static final boolean upToApi(int apiLevel) {
        return (apiLevel < 1 || getApi() <= apiLevel);
    }

    protected static final int getApi() {
        return Build.VERSION.SDK_INT;
    }

}
