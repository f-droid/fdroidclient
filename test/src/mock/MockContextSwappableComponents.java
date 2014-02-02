package mock;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.test.mock.MockContext;

public class MockContextSwappableComponents extends MockContext {

    private PackageManager packageManager;

    private Resources resources;

    public MockContextSwappableComponents setPackageManager(PackageManager pm) {
        packageManager = pm;
        return this;
    }

    public MockContextSwappableComponents setResources(Resources resources) {
        this.resources = resources;
        return this;
    }

    @Override
    public PackageManager getPackageManager() {
        return packageManager;
    }

    @Override
    public Resources getResources() {
        return resources;
    }
}
