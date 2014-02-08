package org.fdroid.fdroid.mock;

import android.content.Context;
import android.content.pm.PackageInfo;
import org.fdroid.fdroid.Utils;

import java.util.Map;

public class MockInstalledApkCache extends Utils.InstalledApkCache {

    @Override
    public Map<String, PackageInfo> getApks(Context context) {
        return buildAppList(context);
    }

}
