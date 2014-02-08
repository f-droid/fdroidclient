package mock;

import android.content.pm.PackageInfo;
import android.test.mock.MockPackageManager;

import java.util.ArrayList;
import java.util.List;

public class MockInstallablePackageManager extends MockPackageManager {

    private List<PackageInfo> info = new ArrayList<PackageInfo>();

    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        return info;
    }

    public void install(String id, int version, String versionName) {
        PackageInfo p = new PackageInfo();
        p.packageName = id;
        p.versionCode = version;
        p.versionName = versionName;
        info.add(p);
    }

}
