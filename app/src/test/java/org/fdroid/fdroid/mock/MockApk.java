package org.belmarket.shop.mock;

import org.belmarket.shop.data.Apk;
import org.belmarket.shop.data.App;

public class MockApk extends Apk {

    public MockApk(String id, int versionCode) {
        this.packageName = id;
        this.versionCode = versionCode;
    }

    public MockApk(App app, int versionCode) {
        this.appId = app.getId();
        this.versionCode = versionCode;
    }

    public MockApk(long appId, int versionCode) {
        this.appId = appId;
        this.versionCode = versionCode;
    }

}
