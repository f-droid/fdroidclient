package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.os.Build;

public class SupportedArchitectures extends Compatibility {

    /**
     * The most preferred ABI is the first element in the list.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    public static String[] getAbis() {
        if (hasApi(21)) {
            return Build.SUPPORTED_ABIS;
        }
        return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
    }

}
