package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.os.Build;

public class SupportedArchitectures extends Compatibility {

    private static String[] getAbisDonut() {
        return new String[]{Build.CPU_ABI};
    }

    @TargetApi(8)
    private static String[] getAbisFroyo() {
        return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
    }

    @TargetApi(21)
    private static String[] getAbisLollipop() {
        return Build.SUPPORTED_ABIS;
    }

    /**
     * The most preferred ABI is the first element in the list.
     */
    public static String[] getAbis() {
        if (hasApi(21)) {
            return getAbisLollipop();
        }
        if (hasApi(8)) {
            return getAbisFroyo();
        }
        return getAbisDonut();
    }

}
