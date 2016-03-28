package org.fdroid.fdroid;

import org.fdroid.fdroid.data.InstalledAppProvider;

import mock.MockInstallablePackageManager;

/**
 * Tests the ability of the {@link  org.fdroid.fdroid.data.InstalledAppCacheUpdater} to stay in sync with
 * the {@link android.content.pm.PackageManager}.
 * For practical reasons, it extends FDroidProviderTest<InstalledAppProvider>, although there is also a
 * separate test for the InstalledAppProvider which tests the CRUD operations in more detail.
 */
public class InstalledAppCacheTest extends FDroidProviderTest<InstalledAppProvider> {

    private MockInstallablePackageManager packageManager;

    public InstalledAppCacheTest() {
        super(InstalledAppProvider.class, InstalledAppProvider.getAuthority());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        packageManager = new MockInstallablePackageManager();
        getSwappableContext().setPackageManager(packageManager);
    }

    @Override
    protected String[] getMinimalProjection() {
        return new String[] {
            InstalledAppProvider.DataColumns.PACKAGE_NAME,
        };
    }

    public void install(String appId, int versionCode, String versionName) {
        packageManager.install(appId, versionCode, versionName);
    }

    public void remove(String appId) {
        packageManager.remove(appId);
    }

/* TODO fix me
    public void testFromEmptyCache() {
        assertResultCount(0, InstalledAppProvider.getContentUri());
        for (int i = 1; i <= 15; i ++) {
            install("com.example.app" + i, 200, "2.0");
        }
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        String[] expectedInstalledIds = {
            "com.example.app1",
            "com.example.app2",
            "com.example.app3",
            "com.example.app4",
            "com.example.app5",
            "com.example.app6",
            "com.example.app7",
            "com.example.app8",
            "com.example.app9",
            "com.example.app10",
            "com.example.app11",
            "com.example.app12",
            "com.example.app13",
            "com.example.app14",
            "com.example.app15",
        };

        TestUtils.assertContainsOnly(getInstalledAppIdsFromProvider(), expectedInstalledIds);
    }

    private String[] getInstalledAppIdsFromProvider() {
        Uri uri = InstalledAppProvider.getContentUri();
        String[] projection = { InstalledAppProvider.DataColumns.PACKAGE_NAME };
        Cursor result = getMockContext().getContentResolver().query(uri, projection, null, null, null);
        if (result == null) {
            return new String[0];
        }

        String[] installedAppIds = new String[result.getCount()];
        result.moveToFirst();
        int i = 0;
        while (!result.isAfterLast()) {
            installedAppIds[i] = result.getString(result.getColumnIndex(InstalledAppProvider.DataColumns.PACKAGE_NAME));
            result.moveToNext();
            i ++;
        }
        result.close();
        return installedAppIds;
    }

    public void testAppsAdded() {
        assertResultCount(0, InstalledAppProvider.getContentUri());

        install("com.example.app1", 1, "v1");
        install("com.example.app2", 1, "v1");
        install("com.example.app3", 1, "v1");
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(3, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app1", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app2", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app3", 1, "v1");

        install("com.example.app10", 1, "v1");
        install("com.example.app11", 1, "v1");
        install("com.example.app12", 1, "v1");
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(6, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app10", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app11", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app12", 1, "v1");
    }

    public void testAppsRemoved() {
        install("com.example.app1", 1, "v1");
        install("com.example.app2", 1, "v1");
        install("com.example.app3", 1, "v1");
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(3, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app1", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app2", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app3", 1, "v1");

        remove("com.example.app2");
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app1", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app3", 1, "v1");
    }

    public void testAppsUpdated() {
        install("com.example.app1", 1, "v1");
        install("com.example.app2", 1, "v1");
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app1", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app2", 1, "v1");

        install("com.example.app2", 20, "v2.0");
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app1", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app2", 20, "v2.0");
    }

    public void testAppsAddedRemovedAndUpdated() {
        install("com.example.app1", 1, "v1");
        install("com.example.app2", 1, "v1");
        install("com.example.app3", 1, "v1");
        install("com.example.app4", 1, "v1");
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(4, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app1", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app2", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app3", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app4", 1, "v1");

        install("com.example.app1", 13, "v1.3");
        remove("com.example.app2");
        remove("com.example.app3");
        install("com.example.app10", 1, "v1");
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(3, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app1", 13, "v1.3");
        assertIsInstalledVersionInDb("com.example.app4", 1, "v1");
        assertIsInstalledVersionInDb("com.example.app10", 1, "v1");

    }
*/
}
