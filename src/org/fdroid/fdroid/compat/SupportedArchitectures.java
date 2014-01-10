package org.fdroid.fdroid.compat;

import java.util.HashSet;

import android.annotation.TargetApi;
import android.util.Log;
import android.os.Build;

public class SupportedArchitectures extends Compatibility {

    private static HashSet<String> getOneAbi() {
        HashSet<String> abis = new HashSet<String>(1);
        abis.add(Build.CPU_ABI);
        return abis;
    }

    @TargetApi(8)
    private static HashSet<String> getTwoAbis() {
        HashSet<String> abis = new HashSet<String>(2);
        abis.add(Build.CPU_ABI);
        abis.add(Build.CPU_ABI2);
        return abis;
    }

    public static HashSet<String> getAbis() {
        if (hasApi(8)) return getTwoAbis();
        return getOneAbi();
    }

}
