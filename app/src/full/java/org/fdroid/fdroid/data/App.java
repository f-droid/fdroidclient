package org.fdroid.fdroid.data;

import android.content.Context;

import org.fdroid.index.v2.FileV2;

public class App {
    public String packageName;
    public String name;
    public FileV2 iconFile;
    public Apk installedApk;
    public long installedVersionCode;
    public long autoInstallVersionCode;
    public String installedVersionName;
    public boolean compatible = false;

    public boolean hasUpdates() {
        return autoInstallVersionCode > installedVersionCode;
    }

    public boolean isInstalled(Context context) {
        return installedVersionCode > 0;
    }
}
