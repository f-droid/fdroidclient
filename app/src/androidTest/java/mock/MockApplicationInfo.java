package mock;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class MockApplicationInfo extends ApplicationInfo {

    private final PackageInfo info;

    public MockApplicationInfo(PackageInfo info) {
        this.info = info;
    }

    @Override
    public CharSequence loadLabel(PackageManager pm) {
        return "Mock app: " + info.packageName;
    }
}
