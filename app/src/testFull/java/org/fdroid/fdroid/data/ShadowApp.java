package org.fdroid.fdroid.data;

import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(App.class)
public class ShadowApp extends ValueObject {

    @Implementation
    protected static int[] getMinTargetMaxSdkVersions(Context context, String packageName) {
        return new int[]{10, 23, Apk.SDK_VERSION_MAX_VALUE};
    }
}
