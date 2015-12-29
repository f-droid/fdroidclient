package org.fdroid.fdroid;

import android.content.ContentValues;

import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.RepoProvider;

import mock.MockInstallablePackageManager;

public class InstalledAppProviderTest extends FDroidProviderTest<InstalledAppProvider> {

    private MockInstallablePackageManager packageManager;

    public InstalledAppProviderTest() {
        super(InstalledAppProvider.class, InstalledAppProvider.getAuthority());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        packageManager = new MockInstallablePackageManager();
        getSwappableContext().setPackageManager(packageManager);
    }

    protected MockInstallablePackageManager getPackageManager() {
        return packageManager;
    }

    public void testUris() {
        assertInvalidUri(InstalledAppProvider.getAuthority());
        assertInvalidUri(RepoProvider.getContentUri());
        assertInvalidUri(AppProvider.getContentUri());
        assertInvalidUri(ApkProvider.getContentUri());
        assertInvalidUri("blah");

        assertValidUri(InstalledAppProvider.getContentUri());
        assertValidUri(InstalledAppProvider.getAppUri("com.example.com"));
        assertValidUri(InstalledAppProvider.getAppUri("blah"));
    }

    public void testInsert() {

        assertResultCount(0, InstalledAppProvider.getContentUri());

        insertInstalledApp("com.example.com1", 1, "v1");
        insertInstalledApp("com.example.com2", 2, "v2");
        insertInstalledApp("com.example.com3", 3, "v3");

        assertResultCount(3, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.com1", 1, "v1");
        assertIsInstalledVersionInDb("com.example.com2", 2, "v2");
        assertIsInstalledVersionInDb("com.example.com3", 3, "v3");
    }

    public void testUpdate() {

        insertInstalledApp("com.example.app1", 10, "1.0");
        insertInstalledApp("com.example.app2", 10, "1.0");

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app2", 10, "1.0");

        try {
            getMockContentResolver().update(
                    InstalledAppProvider.getAppUri("com.example.app2"),
                    createContentValues(11, "1.1"),
                    null, null
            );
            fail();
        } catch (UnsupportedOperationException e) {
            // We expect this to happen, because we should be using insert() instead.
        }

        getMockContentResolver().insert(
                InstalledAppProvider.getContentUri(),
                createContentValues("com.example.app2", 11, "1.1")
        );

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app2", 11, "1.1");

    }

    public void testDelete() {

        insertInstalledApp("com.example.app1", 10, "1.0");
        insertInstalledApp("com.example.app2", 10, "1.0");

        assertResultCount(2, InstalledAppProvider.getContentUri());

        getMockContentResolver().delete(InstalledAppProvider.getAppUri("com.example.app1"), null, null);

        assertResultCount(1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.app2", 10, "1.0");

    }

    public void testInsertWithBroadcast() {

        installAndBroadcast("com.example.broadcasted1", 10, "v1.0");
        installAndBroadcast("com.example.broadcasted2", 105, "v1.05");

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.broadcasted1", 10, "v1.0");
        assertIsInstalledVersionInDb("com.example.broadcasted2", 105, "v1.05");
    }

    public void testUpdateWithBroadcast() {

        installAndBroadcast("com.example.toUpgrade", 1, "v0.1");

        assertResultCount(1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.toUpgrade", 1, "v0.1");

        upgradeAndBroadcast("com.example.toUpgrade", 2,  "v0.2");

        assertResultCount(1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.toUpgrade", 2, "v0.2");

    }

    public void testDeleteWithBroadcast() {

        installAndBroadcast("com.example.toKeep", 1, "v0.1");
        installAndBroadcast("com.example.toDelete", 1, "v0.1");

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.toKeep", 1, "v0.1");
        assertIsInstalledVersionInDb("com.example.toDelete", 1, "v0.1");

        removeAndBroadcast("com.example.toDelete");

        assertResultCount(1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.toKeep", 1, "v0.1");

    }

    @Override
    protected String[] getMinimalProjection() {
        return new String[] {
                InstalledAppProvider.DataColumns.PACKAGE_NAME,
                InstalledAppProvider.DataColumns.VERSION_CODE,
                InstalledAppProvider.DataColumns.VERSION_NAME,
        };
    }

    private ContentValues createContentValues(int versionCode, String versionNumber) {
        return createContentValues(null, versionCode, versionNumber);
    }

    private ContentValues createContentValues(String appId, int versionCode, String versionNumber) {
        ContentValues values = new ContentValues(3);
        if (appId != null) {
            values.put(InstalledAppProvider.DataColumns.PACKAGE_NAME, appId);
        }
        values.put(InstalledAppProvider.DataColumns.APPLICATION_LABEL, "Mock app: " + appId);
        values.put(InstalledAppProvider.DataColumns.VERSION_CODE, versionCode);
        values.put(InstalledAppProvider.DataColumns.VERSION_NAME, versionNumber);
        values.put(InstalledAppProvider.DataColumns.SIGNATURE, "");
        return values;
    }

    private void insertInstalledApp(String appId, int versionCode, String versionNumber) {
        ContentValues values = createContentValues(appId, versionCode, versionNumber);
        getMockContentResolver().insert(InstalledAppProvider.getContentUri(), values);
    }

    private void removeAndBroadcast(String appId) {
        TestUtils.removeAndBroadcast(getSwappableContext(), getPackageManager(), appId);
    }

    private void upgradeAndBroadcast(String appId, int versionCode, String versionName) {
        TestUtils.upgradeAndBroadcast(getSwappableContext(), getPackageManager(), appId, versionCode, versionName);
    }

    private void installAndBroadcast(String appId, int versionCode, String versionName) {
        TestUtils.installAndBroadcast(getSwappableContext(), getPackageManager(), appId, versionCode, versionName);
    }

}
