package mock;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.IOException;

@SuppressLint("ParcelCreator")
public class MockApplicationInfo extends ApplicationInfo {

    private final PackageInfo info;

    public MockApplicationInfo(PackageInfo info) {
        this.info = info;
        try {
            this.publicSourceDir = File.createTempFile(info.packageName, "apk").getAbsolutePath();
        } catch (IOException e) {
            this.publicSourceDir = "/data/app/" + info.packageName + "-4.apk";
        }
    }

    @Override
    public CharSequence loadLabel(PackageManager pm) {
        return "Mock app: " + info.packageName;
    }
}
