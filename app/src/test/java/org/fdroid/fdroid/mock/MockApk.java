package org.fdroid.fdroid.mock;

import org.fdroid.fdroid.data.Apk;

public class MockApk extends Apk {

    public MockApk(String id, int versionCode, String repoAddress, String apkName) {
        this.packageName = id;
        this.versionCode = versionCode;
        this.repoAddress = repoAddress;
        this.apkName = apkName;
    }

}
