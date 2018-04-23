package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.fdroid.fdroid.Assert.assertContainsOnly;
import static org.fdroid.fdroid.Assert.assertResultCount;
import static org.fdroid.fdroid.Assert.insertApk;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("LineLength")
public class AppProviderTest extends FDroidProviderTest {

    private static final String[] PROJ = Cols.ALL;

    @Before
    public void setup() {
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
        Preferences.setupForTests(context);
    }

    /**
     * Although this doesn't directly relate to the {@link AppProvider}, it is here because
     * the {@link AppProvider} used to stumble across this bug when asking for installed apps,
     * and the device had over 1000 apps installed.
     */
    @Test
    public void testMaxSqliteParams() {
        insertApp("com.example.app1", "App 1");
        insertApp("com.example.app100", "App 100");
        insertApp("com.example.app1000", "App 1000");

        for (int i = 0; i < 50; i++) {
            InstalledAppTestUtils.install(context, "com.example.app" + i, 1, "v1");
        }
        assertResultCount(contentResolver, 1, AppProvider.getInstalledUri(), PROJ);

        for (int i = 50; i < 500; i++) {
            InstalledAppTestUtils.install(context, "com.example.app" + i, 1, "v1");
        }
        assertResultCount(contentResolver, 2, AppProvider.getInstalledUri(), PROJ);

        for (int i = 500; i < 1100; i++) {
            InstalledAppTestUtils.install(context, "com.example.app" + i, 1, "v1");
        }
        assertResultCount(contentResolver, 3, AppProvider.getInstalledUri(), PROJ);
    }

    @Test
    public void testCantFindApp() {
        assertNull(AppProvider.Helper.findSpecificApp(context.getContentResolver(), "com.example.doesnt-exist", 1, Cols.ALL));
    }

    @Test
    public void testQuery() {
        Cursor cursor = queryAllApps();
        assertNotNull(cursor);
        cursor.close();
    }

    private void insertApps(int count) {
        for (int i = 0; i < count; i++) {
            insertApp("com.example.test." + i, "Test app " + i);
        }
    }

    private void insertAndInstallApp(
            String packageName, int installedVercode, int suggestedVercode,
            boolean ignoreAll, int ignoreVercode) {
        App app = insertApp(contentResolver, context, packageName, "App: " + packageName, new ContentValues());
        AppPrefsProvider.Helper.update(context, app, new AppPrefs(ignoreVercode, ignoreAll, false));

        ContentValues certValue = new ContentValues(1);
        certValue.put(Schema.ApkTable.Cols.SIGNATURE, TestUtils.FDROID_SIG);

        // Make sure that the relevant apks are also in the DB, or else the `install` method below will
        // not be able to correctly calculate the suggested version o the apk.
        insertApk(context, packageName, installedVercode, certValue);
        if (installedVercode != suggestedVercode) {
            insertApk(context, packageName, suggestedVercode, certValue);
        }

        InstalledAppTestUtils.install(context, packageName, installedVercode, "v" + installedVercode, TestUtils.FDROID_CERT);
    }

    @Test
    public void testCanUpdate() {
        insertApp("not installed", "not installed");
        insertAndInstallApp("installed, only one version available", 1, 1, false, 0);
        insertAndInstallApp("installed, already latest, no ignore", 10, 10, false, 0);
        insertAndInstallApp("installed, already latest, ignore all", 10, 10, true, 0);
        insertAndInstallApp("installed, already latest, ignore latest", 10, 10, false, 10);
        insertAndInstallApp("installed, already latest, ignore old", 10, 10, false, 5);
        insertAndInstallApp("installed, old version, no ignore", 5, 10, false, 0);
        insertAndInstallApp("installed, old version, ignore all", 5, 10, true, 0);
        insertAndInstallApp("installed, old version, ignore latest", 5, 10, false, 10);
        insertAndInstallApp("installed, old version, ignore newer, but not latest", 5, 10, false, 8);

        ContentResolver r = context.getContentResolver();

        // Can't "update", although can "install"...
        App notInstalled = AppProvider.Helper.findSpecificApp(r, "not installed", 1, Cols.ALL);
        assertFalse(notInstalled.canAndWantToUpdate(context));

        assertResultCount(contentResolver, 2, AppProvider.getCanUpdateUri(), PROJ);
        assertResultCount(contentResolver, 9, AppProvider.getInstalledUri(), PROJ);

        App installedOnlyOneVersionAvailable = AppProvider.Helper.findSpecificApp(r, "installed, only one version available", 1, Cols.ALL);
        App installedAlreadyLatestNoIgnore = AppProvider.Helper.findSpecificApp(r, "installed, already latest, no ignore", 1, Cols.ALL);
        App installedAlreadyLatestIgnoreAll = AppProvider.Helper.findSpecificApp(r, "installed, already latest, ignore all", 1, Cols.ALL);
        App installedAlreadyLatestIgnoreLatest = AppProvider.Helper.findSpecificApp(r, "installed, already latest, ignore latest", 1, Cols.ALL);
        App installedAlreadyLatestIgnoreOld = AppProvider.Helper.findSpecificApp(r, "installed, already latest, ignore old", 1, Cols.ALL);

        assertFalse(installedOnlyOneVersionAvailable.canAndWantToUpdate(context));
        assertFalse(installedAlreadyLatestNoIgnore.canAndWantToUpdate(context));
        assertFalse(installedAlreadyLatestIgnoreAll.canAndWantToUpdate(context));
        assertFalse(installedAlreadyLatestIgnoreLatest.canAndWantToUpdate(context));
        assertFalse(installedAlreadyLatestIgnoreOld.canAndWantToUpdate(context));

        App installedOldNoIgnore = AppProvider.Helper.findSpecificApp(r, "installed, old version, no ignore", 1, Cols.ALL);
        App installedOldIgnoreAll = AppProvider.Helper.findSpecificApp(r, "installed, old version, ignore all", 1, Cols.ALL);
        App installedOldIgnoreLatest = AppProvider.Helper.findSpecificApp(r, "installed, old version, ignore latest", 1, Cols.ALL);
        App installedOldIgnoreNewerNotLatest = AppProvider.Helper.findSpecificApp(r, "installed, old version, ignore newer, but not latest", 1, Cols.ALL);

        assertTrue(installedOldNoIgnore.canAndWantToUpdate(context));
        assertFalse(installedOldIgnoreAll.canAndWantToUpdate(context));
        assertFalse(installedOldIgnoreLatest.canAndWantToUpdate(context));
        assertTrue(installedOldIgnoreNewerNotLatest.canAndWantToUpdate(context));

        Cursor canUpdateCursor = r.query(AppProvider.getCanUpdateUri(), Cols.ALL, null, null, null);
        assertNotNull(canUpdateCursor);
        canUpdateCursor.moveToFirst();
        List<String> canUpdateIds = new ArrayList<>(canUpdateCursor.getCount());
        while (!canUpdateCursor.isAfterLast()) {
            canUpdateIds.add(new App(canUpdateCursor).packageName);
            canUpdateCursor.moveToNext();
        }
        canUpdateCursor.close();

        String[] expectedUpdateableIds = {
                "installed, old version, no ignore",
                "installed, old version, ignore newer, but not latest",
        };

        assertContainsOnly(expectedUpdateableIds, canUpdateIds);
    }

    @Test
    public void testIgnored() {
        insertApp("not installed", "not installed");
        insertAndInstallApp("installed, only one version available", 1, 1, false, 0);
        insertAndInstallApp("installed, already latest, no ignore", 10, 10, false, 0);
        insertAndInstallApp("installed, already latest, ignore all", 10, 10, true, 0);
        insertAndInstallApp("installed, already latest, ignore latest", 10, 10, false, 10);
        insertAndInstallApp("installed, already latest, ignore old", 10, 10, false, 5);
        insertAndInstallApp("installed, old version, no ignore", 5, 10, false, 0);
        insertAndInstallApp("installed, old version, ignore all", 5, 10, true, 0);
        insertAndInstallApp("installed, old version, ignore latest", 5, 10, false, 10);
        insertAndInstallApp("installed, old version, ignore newer, but not latest", 5, 10, false, 8);

        assertResultCount(contentResolver, 10, AppProvider.getContentUri(), PROJ);

        String[] projection = {Cols.Package.PACKAGE_NAME};
        List<App> canUpdateApps = AppProvider.Helper.findCanUpdate(context, projection);

        String[] expectedCanUpdate = {
                "installed, old version, no ignore",
                "installed, old version, ignore newer, but not latest",

                // These are ignored because they don't have updates available:
                // "installed, only one version available",
                // "installed, already latest, no ignore",
                // "installed, already latest, ignore old",
                // "not installed",

                // These four should be ignored due to the app preferences:
                // "installed, already latest, ignore all",
                // "installed, already latest, ignore latest",
                // "installed, old version, ignore all",
                // "installed, old version, ignore latest",

        };

        assertContainsOnlyIds(canUpdateApps, expectedCanUpdate);
    }

    public static void assertContainsOnlyIds(List<App> actualApps, String[] expectedIds) {
        List<String> actualIds = new ArrayList<>(actualApps.size());
        for (App app : actualApps) {
            actualIds.add(app.packageName);
        }
        assertContainsOnly(actualIds, expectedIds);
    }

    @Test
    public void testInstalled() {
        insertApps(100);

        assertResultCount(contentResolver, 100, AppProvider.getContentUri(), PROJ);
        assertResultCount(contentResolver, 0, AppProvider.getCanUpdateUri(), PROJ);
        assertResultCount(contentResolver, 0, AppProvider.getInstalledUri(), PROJ);

        for (int i = 10; i < 20; i++) {
            InstalledAppTestUtils.install(context, "com.example.test." + i, i, "v1");
        }

        assertResultCount(contentResolver, 0, AppProvider.getCanUpdateUri(), PROJ);
        assertResultCount(contentResolver, 10, AppProvider.getInstalledUri(), PROJ);
    }

    @Test
    public void testInsert() {

        // Start with an empty database...
        Cursor cursor = queryAllApps();
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // Insert a new record...
        insertApp("org.fdroid.fdroid", "F-Droid");
        cursor = queryAllApps();
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());

        // And now we should be able to recover these values from the app
        // value object (because the queryAllApps() helper asks for NAME and
        // PACKAGE_NAME.
        cursor.moveToFirst();
        App app = new App(cursor);
        cursor.close();
        assertEquals("org.fdroid.fdroid", app.packageName);
        assertEquals("F-Droid", app.name);

        App otherApp = AppProvider.Helper.findSpecificApp(context.getContentResolver(), "org.fdroid.fdroid", 1, Cols.ALL);
        assertNotNull(otherApp);
        assertEquals("org.fdroid.fdroid", otherApp.packageName);
        assertEquals("F-Droid", otherApp.name);
    }

    @Test
    public void testInsertTrimsNamesAndSummary() {
        // Insert a new record with unwanted newlines...
        App app = insertApp("org.fdroid.trimmer", "Trim me\n", "Trim me too\n");

        assertEquals("org.fdroid.trimmer", app.packageName);
        assertEquals("Trim me", app.name);
        assertEquals("Trim me too", app.summary);
    }

    /**
     * We intentionally throw an IllegalArgumentException if you haven't
     * yet called cursor.move*().
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCursorMustMoveToFirst() {
        insertApp("org.fdroid.fdroid", "F-Droid");
        Cursor cursor = queryAllApps();
        new App(cursor);
    }

    private Cursor queryAllApps() {
        String[] projection = new String[]{
                Cols._ID,
                Cols.NAME,
                Cols.Package.PACKAGE_NAME,
        };
        return contentResolver.query(AppProvider.getContentUri(), projection, null, null, null);
    }

    // =======================================================================
    //  Misc helper functions
    //  (to be used by any tests in this suite)
    // =======================================================================

    private void insertApp(String id, String name) {
        insertApp(contentResolver, context, id, name, new ContentValues());
    }

    private App insertApp(String id, String name, String summary) {
        ContentValues additionalValues = new ContentValues();
        additionalValues.put(Cols.SUMMARY, summary);
        return insertApp(contentResolver, context, id, name, additionalValues);
    }

    public static App insertApp(ShadowContentResolver contentResolver, Context context, String id, String name, ContentValues additionalValues) {
        return insertApp(contentResolver, context, id, name, additionalValues, 1);
    }

    public static App insertApp(ShadowContentResolver contentResolver, Context context, String id, String name, ContentValues additionalValues, long repoId) {

        ContentValues values = new ContentValues();
        values.put(Cols.Package.PACKAGE_NAME, id);
        values.put(Cols.REPO_ID, repoId);
        values.put(Cols.NAME, name);

        // Required fields (NOT NULL in the database).
        values.put(Cols.SUMMARY, "test summary");
        values.put(Cols.DESCRIPTION, "test description");
        values.put(Cols.LICENSE, "GPL?");
        values.put(Cols.IS_COMPATIBLE, 1);

        values.put(Cols.PREFERRED_SIGNER, "eaa1d713b9c2a0475234a86d6539f910");

        values.putAll(additionalValues);

        Uri uri = AppProvider.getContentUri();

        contentResolver.insert(uri, values);

        AppProvider.Helper.recalculatePreferredMetadata(context);

        return AppProvider.Helper.findSpecificApp(context.getContentResolver(), id, repoId, Cols.ALL);
    }
}
