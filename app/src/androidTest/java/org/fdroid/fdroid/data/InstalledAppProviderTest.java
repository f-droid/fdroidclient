package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.pm.PackageInfo;

import mock.MockContextSwappableComponents;
import mock.MockInstallablePackageManager;

@SuppressWarnings("PMD")  // TODO port this to JUnit 4 semantics
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
        install("com.example.broadcasted1", 10, "v1.0");
        install("com.example.broadcasted2", 105, "v1.05");

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.broadcasted1", 10, "v1.0");
        assertIsInstalledVersionInDb("com.example.broadcasted2", 105, "v1.05");
    }

    public void testUpdateWithBroadcast() {

        install("com.example.toUpgrade", 1, "v0.1");

        assertResultCount(1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.toUpgrade", 1, "v0.1");

        install("com.example.toUpgrade", 2, "v0.2");

        assertResultCount(1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.toUpgrade", 2, "v0.2");

    }

    public void testDeleteWithBroadcast() {

        install("com.example.toKeep", 1, "v0.1");
        install("com.example.toDelete", 1, "v0.1");

        assertResultCount(2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.toKeep", 1, "v0.1");
        assertIsInstalledVersionInDb("com.example.toDelete", 1, "v0.1");

        remove("com.example.toDelete");

        assertResultCount(1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb("com.example.toKeep", 1, "v0.1");

    }

    @Override
    protected String[] getMinimalProjection() {
        return new String[]{
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

    private void remove(String packageName) {
        remove(getSwappableContext(), getPackageManager(), packageName);
    }

    private void install(String appId, int versionCode, String versionName) {
        install(getSwappableContext(), getPackageManager(), appId, versionCode, versionName);
    }

    /**
     * Will tell {@code pm} that we are installing {@code packageName}, and then update the
     * "installed apps" table in the database.
     */
    public static void install(MockContextSwappableComponents context,
                               MockInstallablePackageManager pm, String packageName,
                               int versionCode, String versionName) {

        context.setPackageManager(pm);
        pm.install(packageName, versionCode, versionName);
        PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
        InstalledAppProviderService.insertAppIntoDb(context, packageName, packageInfo);
    }

    /**
     * @see #install(mock.MockContextSwappableComponents, mock.MockInstallablePackageManager, String, int, String)
     */
    public static void remove(MockContextSwappableComponents context, MockInstallablePackageManager pm, String packageName) {

        context.setPackageManager(pm);
        pm.remove(packageName);
        InstalledAppProviderService.deleteAppFromDb(context, packageName);
    }

}
