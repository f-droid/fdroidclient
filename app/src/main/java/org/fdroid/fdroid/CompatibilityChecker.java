package org.fdroid.fdroid;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.Nullable;

import org.fdroid.fdroid.data.Apk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Call getIncompatibleReasons(apk) on an instance of this class to
// find reasons why an apk may be incompatible with the user's device.
public class CompatibilityChecker {

    public static final String TAG = "Compatibility";

    private final Context context;
    private final Set<String> features;
    private final String[] cpuAbis;
    private final boolean forceTouchApps;

    public CompatibilityChecker(Context ctx) {

        context = ctx.getApplicationContext();

        forceTouchApps = Preferences.get().forceTouchApps();

        PackageManager pm = ctx.getPackageManager();

        features = new HashSet<>();
        if (pm != null) {
            final FeatureInfo[] featureArray = pm.getSystemAvailableFeatures();
            if (featureArray != null) {
                for (FeatureInfo fi : pm.getSystemAvailableFeatures()) {
                    features.add(fi.name);
                }
            }
        }

        cpuAbis = Build.SUPPORTED_ABIS;
    }

    private boolean compatibleApi(@Nullable String[] nativecode) {
        if (nativecode == null) {
            return true;
        }

        for (final String cpuAbi : cpuAbis) {
            for (String code : nativecode) {
                if (code.equals(cpuAbi)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getIncompatibleReasons(final Apk apk) {

        List<String> incompatibleReasons = new ArrayList<>();

        if (Build.VERSION.SDK_INT < apk.minSdkVersion) {
            incompatibleReasons.add(context.getString(
                    R.string.minsdk_or_later,
                    Utils.getAndroidVersionName(apk.minSdkVersion)));
        } else if (Build.VERSION.SDK_INT > apk.maxSdkVersion) {
            incompatibleReasons.add(context.getString(
                    R.string.up_to_maxsdk,
                    Utils.getAndroidVersionName(apk.maxSdkVersion)));
        }

        if (apk.features != null) {
            for (final String feat : apk.features) {
                if (forceTouchApps && "android.hardware.touchscreen".equals(feat)) {
                    continue;
                }
                if (!features.contains(feat)) {
                    Collections.addAll(incompatibleReasons, feat.split(","));
                }
            }
        }
        if (!compatibleApi(apk.nativecode)) {
            Collections.addAll(incompatibleReasons, apk.nativecode);
        }

        return incompatibleReasons;
    }
}
