package org.fdroid.fdroid.shadows;

import android.annotation.SuppressLint;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Workaround for the large amount of "WARNING: unknown service appops" messages which clogged up our CI output.
 */
@Implements(className = org.robolectric.shadows.ShadowContextImpl.CLASS_NAME)
@SuppressLint("Unused")
public class ShadowContextImpl extends org.robolectric.shadows.ShadowContextImpl {
    @Override
    @Implementation
    public Object getSystemService(String name) {
        if ("appops".equals(name)) {
            return null;
        }

        return super.getSystemService(name);
    }
}
