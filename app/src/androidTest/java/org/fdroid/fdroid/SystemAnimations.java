package org.fdroid.fdroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * @see <a href="https://artemzin.com/blog/easiest-way-to-give-set_animation_scale-permission-for-your-ui-tests-on-android/>EASIEST WAY TO GIVE SET_ANIMATION_SCALE PERMISSION FOR YOUR UI TESTS ON ANDROID</a>
 * @see <a href="https://gist.github.com/xrigau/11284124>Disable animations for Espresso tests</a>
 */
class SystemAnimations {
    public static final String TAG = "SystemAnimations";

    private static final float DISABLED = 0.0f;
    private static final float DEFAULT = 1.0f;

    static void disableAll(Context context) {
        int permStatus = context.checkCallingOrSelfPermission(Manifest.permission.SET_ANIMATION_SCALE);
        if (permStatus == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Manifest.permission.SET_ANIMATION_SCALE PERMISSION_GRANTED");
            setSystemAnimationsScale(DISABLED);
        } else {
            Log.i(TAG, "Disabling Manifest.permission.SET_ANIMATION_SCALE failed: " + permStatus);
        }
    }

    static void enableAll(Context context) {
        int permStatus = context.checkCallingOrSelfPermission(Manifest.permission.SET_ANIMATION_SCALE);
        if (permStatus == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Manifest.permission.SET_ANIMATION_SCALE PERMISSION_GRANTED");
            setSystemAnimationsScale(DEFAULT);
        } else {
            Log.i(TAG, "Enabling Manifest.permission.SET_ANIMATION_SCALE failed: " + permStatus);
        }
    }

    private static void setSystemAnimationsScale(float animationScale) {
        try {
            Class<?> windowManagerStubClazz = Class.forName("android.view.IWindowManager$Stub");
            Method asInterface = windowManagerStubClazz.getDeclaredMethod("asInterface", IBinder.class);
            Class<?> serviceManagerClazz = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClazz.getDeclaredMethod("getService", String.class);
            Class<?> windowManagerClazz = Class.forName("android.view.IWindowManager");
            Method setAnimationScales = windowManagerClazz.getDeclaredMethod("setAnimationScales", float[].class);
            Method getAnimationScales = windowManagerClazz.getDeclaredMethod("getAnimationScales");

            IBinder windowManagerBinder = (IBinder) getService.invoke(null, "window");
            Object windowManagerObj = asInterface.invoke(null, windowManagerBinder);
            float[] currentScales = (float[]) getAnimationScales.invoke(windowManagerObj);
            for (int i = 0; i < currentScales.length; i++) {
                currentScales[i] = animationScale;
            }
            setAnimationScales.invoke(windowManagerObj, new Object[]{currentScales});
        } catch (Exception e) {
            Log.e(TAG, "Could not change animation scale to " + animationScale + " :'(");
        }
    }
}
