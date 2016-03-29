package org.fdroid.fdroid.mock;

import org.fdroid.fdroid.data.Apk;

public class MockApk extends Apk {

    public MockApk(String id, int versionCode) {
        this.packageName = id;
        this.vercode = versionCode;
    }

}
