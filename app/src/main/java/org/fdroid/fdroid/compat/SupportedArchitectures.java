package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.os.Build;

public class SupportedArchitectures {

    /**
     * The most preferred ABI is the first element in the list.
     */
    @TargetApi(21)
    @SuppressWarnings("deprecation")
    public static String[] getAbis() {
        if (Build.VERSION.SDK_INT >= 21) {
            return Build.SUPPORTED_ABIS;
        }
        return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
    }

}
