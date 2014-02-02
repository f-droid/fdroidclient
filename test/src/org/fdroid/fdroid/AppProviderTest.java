package org.fdroid.fdroid;

import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import mock.MockInstallablePackageManager;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppProviderTest extends FDroidProviderTest<AppProvider> {

    public AppProviderTest() {
        super(AppProvider.class, AppProvider.getAuthority());
    }

    protected String[] getMinimalProjection() {
        return new String[] {
            AppProvider.DataColumns.APP_ID,
            AppProvider.DataColumns.NAME
        };
    }

    public void testUris() {
        assertInvalidUri(AppProvider.getAuthority());
        assertInvalidUri(ApkProvider.getContentUri());

        assertValidUri(AppProvider.getContentUri());
        assertValidUri(AppProvider.getSearchUri("'searching!'"));
        assertValidUri(AppProvider.getNoApksUri());
        assertValidUri(AppProvider.getInstalledUri());
        assertValidUri(AppProvider.getCanUpdateUri());

        App app = new App();
        app.id = "org.fdroid.fdroid";

        List<App> apps = new ArrayList<App>(1);
        apps.add(app);

        assertValidUri(AppProvider.getContentUri(app));
        assertValidUri(AppProvider.getContentUri(apps));
        assertValidUri(AppProvider.getContentUri("org.fdroid.fdroid"));
    }

    public void testQuery() {
        Cursor cursor = queryAllApps();
        assertNotNull(cursor);
    }

    public void testInstalled() {

        Utils.clearInstalledApksCache();

        MockInstallablePackageManager pm = new MockInstallablePackageManager();
        getSwappableContext().setPackageManager(pm);

        for (int i = 0; i < 100; i ++) {
            insertApp("com.example.test." + i, "Test app " + i);
        }

        assertAppCount(100, AppProvider.getContentUri());
        assertAppCount(0, AppProvider.getInstalledUri());

        for (int i = 10; i < 20; i ++) {
            pm.install("com.example.test." + i, i, "v1");
        }

        assertAppCount(10, AppProvider.getInstalledUri());
    }

    private void assertAppCount(int expectedCount, Uri uri) {
        Cursor cursor = getProvider().query(uri, getMinimalProjection(), null, null, null);
        assertNotNull(cursor);
        assertEquals(expectedCount, cursor.getCount());
    }

    public void testInsert() {

        // Start with an empty database...
        Cursor cursor = queryAllApps();
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());

        // Insert a new record...
        insertApp("org.fdroid.fdroid", "F-Droid");
        cursor = queryAllApps();
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());

        // We intentionally throw an IllegalArgumentException if you haven't
        // yet called cursor.move*()...
        try {
            new App(cursor);
            fail();
        } catch (IllegalArgumentException e) {
            // Success!
        } catch (Exception e) {
            fail();
        }

        // And now we should be able to recover these values from the app
        // value object (because the queryAllApps() helper asks for NAME and
        // APP_ID.
        cursor.moveToFirst();
        App app = new App(cursor);
        assertEquals("org.fdroid.fdroid", app.id);
        assertEquals("F-Droid", app.name);
    }

    private Cursor queryAllApps() {
        return getProvider().query(AppProvider.getContentUri(), getMinimalProjection(), null, null, null);
    }

    private void insertApp(String id, String name) {
        ContentValues values = new ContentValues(2);
        values.put(AppProvider.DataColumns.APP_ID, id);
        values.put(AppProvider.DataColumns.NAME, name);

        // Required fields (NOT NULL in the database).
        values.put(AppProvider.DataColumns.SUMMARY, "test summary");
        values.put(AppProvider.DataColumns.DESCRIPTION, "test description");
        values.put(AppProvider.DataColumns.LICENSE, "GPL?");
        values.put(AppProvider.DataColumns.IS_COMPATIBLE, 1);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, 0);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, 0);

        Uri uri = AppProvider.getContentUri();

        getProvider().insert(uri, values);
    }

}
