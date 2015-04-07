package mock;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

public class MockContextSwappableComponents extends MockContext {

    private PackageManager packageManager;

    private Resources resources;
    private MockContentResolver contentResolver;

    public MockContextSwappableComponents setPackageManager(PackageManager pm) {
        packageManager = pm;
        return this;
    }

    public MockContextSwappableComponents setResources(Resources resources) {
        this.resources = resources;
        return this;
    }

    public MockContextSwappableComponents setContentResolver(MockContentResolver contentResolver) {
        this.contentResolver = contentResolver;
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

    @Override
    public MockContentResolver getContentResolver() {
        return contentResolver;
    }
}
