package org.fdroid.fdroid.compat;

import java.util.Set;
import java.util.HashSet;

import android.annotation.TargetApi;
import android.util.Log;
import android.os.Build;

public class SupportedArchitectures extends Compatibility {

    private static Set<String> getOneAbi() {
        Set<String> abis = new HashSet<String>(1);
        abis.add(Build.CPU_ABI);
        return abis;
    }

    @TargetApi(8)
    private static Set<String> getTwoAbis() {
        Set<String> abis = new HashSet<String>(2);
        abis.add(Build.CPU_ABI);
        abis.add(Build.CPU_ABI2);
        return abis;
    }

    public static Set<String> getAbis() {
        if (hasApi(8)) return getTwoAbis();
        return getOneAbi();
    }

}
