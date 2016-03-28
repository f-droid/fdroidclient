package mock;

import android.content.pm.PackageInfo;
import android.test.mock.MockPackageManager;

import java.util.ArrayList;
import java.util.List;

public class MockEmptyPackageManager extends MockPackageManager {

    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        return new ArrayList<>();
    }

}
