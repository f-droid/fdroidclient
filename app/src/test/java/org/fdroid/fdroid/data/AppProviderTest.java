package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.fdroid.fdroid.Assert.assertContainsOnly;
import static org.fdroid.fdroid.Assert.assertResultCount;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// TODO: Use sdk=24 when Robolectric supports this
@Config(constants = BuildConfig.class, application = Application.class, sdk = 23)
@RunWith(RobolectricGradleTestRunner.class)
public class AppProviderTest extends FDroidProviderTest {

    private static final String[] PROJ = Cols.ALL;

    @Before
    public void setup() {
        ShadowContentResolver.registerProvider(AppProvider.getAuthority(), new AppProvider());
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
        assertNull(AppProvider.Helper.findByPackageName(context.getContentResolver(), "com.example.doesnt-exist"));
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
        ContentValues values = new ContentValues(3);
        values.put(Cols.SUGGESTED_VERSION_CODE, suggestedVercode);
        App app = insertApp(packageName, "App: " + packageName, values);
        AppPrefsProvider.Helper.update(context, app, new AppPrefs(ignoreVercode, ignoreAll));

        InstalledAppTestUtils.install(context, packageName, installedVercode, "v" + installedVercode);
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
        App notInstalled = AppProvider.Helper.findByPackageName(r, "not installed");
        assertFalse(notInstalled.canAndWantToUpdate(context));

        App installedOnlyOneVersionAvailable   = AppProvider.Helper.findByPackageName(r, "installed, only one version available");
        App installedAlreadyLatestNoIgnore     = AppProvider.Helper.findByPackageName(r, "installed, already latest, no ignore");
        App installedAlreadyLatestIgnoreAll    = AppProvider.Helper.findByPackageName(r, "installed, already latest, ignore all");
        App installedAlreadyLatestIgnoreLatest = AppProvider.Helper.findByPackageName(r, "installed, already latest, ignore latest");
        App installedAlreadyLatestIgnoreOld    = AppProvider.Helper.findByPackageName(r, "installed, already latest, ignore old");

        assertFalse(installedOnlyOneVersionAvailable.canAndWantToUpdate(context));
        assertFalse(installedAlreadyLatestNoIgnore.canAndWantToUpdate(context));
        assertFalse(installedAlreadyLatestIgnoreAll.canAndWantToUpdate(context));
        assertFalse(installedAlreadyLatestIgnoreLatest.canAndWantToUpdate(context));
        assertFalse(installedAlreadyLatestIgnoreOld.canAndWantToUpdate(context));

        App installedOldNoIgnore             = AppProvider.Helper.findByPackageName(r, "installed, old version, no ignore");
        App installedOldIgnoreAll            = AppProvider.Helper.findByPackageName(r, "installed, old version, ignore all");
        App installedOldIgnoreLatest         = AppProvider.Helper.findByPackageName(r, "installed, old version, ignore latest");
        App installedOldIgnoreNewerNotLatest = AppProvider.Helper.findByPackageName(r, "installed, old version, ignore newer, but not latest");

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

        String[] projection = {Cols.PACKAGE_NAME};
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

    private void assertContainsOnlyIds(List<App> actualApps, String[] expectedIds) {
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
        assertResultCount(contentResolver, 0, AppProvider.getInstalledUri(), PROJ);

        for (int i = 10; i < 20; i++) {
            InstalledAppTestUtils.install(context, "com.example.test." + i, i, "v1");
        }

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

        App otherApp = AppProvider.Helper.findByPackageName(context.getContentResolver(), "org.fdroid.fdroid");
        assertNotNull(otherApp);
        assertEquals("org.fdroid.fdroid", otherApp.packageName);
        assertEquals("F-Droid", otherApp.name);
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
        String[] projection = new String[] {
                Cols._ID,
                Cols.NAME,
                Cols.PACKAGE_NAME,
        };
        return contentResolver.query(AppProvider.getContentUri(), projection, null, null, null);
    }


    // ========================================================================
    //  "Categories"
    //  (at this point) not an additional table, but we treat them sort of
    //  like they are. That means that if we change the implementation to
    //  use a separate table in the future, these should still pass.
    // ========================================================================

    @Test
    public void testCategoriesSingle() {
        insertAppWithCategory("com.dog", "Dog", "Animal");
        insertAppWithCategory("com.rock", "Rock", "Mineral");
        insertAppWithCategory("com.banana", "Banana", "Vegetable");

        List<String> categories = AppProvider.Helper.categories(context);
        String[] expected = new String[] {
                context.getResources().getString(R.string.category_Whats_New),
                context.getResources().getString(R.string.category_Recently_Updated),
                context.getResources().getString(R.string.category_All),
                "Animal",
                "Mineral",
                "Vegetable",
        };
        assertContainsOnly(categories, expected);
    }

    @Test
    public void testCategoriesMultiple() {
        insertAppWithCategory("com.rock.dog", "Rock-Dog", "Mineral,Animal");
        insertAppWithCategory("com.dog.rock.apple", "Dog-Rock-Apple", "Animal,Mineral,Vegetable");
        insertAppWithCategory("com.banana.apple", "Banana", "Vegetable,Vegetable");

        List<String> categories = AppProvider.Helper.categories(context);
        String[] expected = new String[] {
                context.getResources().getString(R.string.category_Whats_New),
                context.getResources().getString(R.string.category_Recently_Updated),
                context.getResources().getString(R.string.category_All),

                "Animal",
                "Mineral",
                "Vegetable",
        };
        assertContainsOnly(categories, expected);

        insertAppWithCategory("com.example.game", "Game",
                "Running,Shooting,Jumping,Bleh,Sneh,Pleh,Blah,Test category," +
                        "The quick brown fox jumps over the lazy dog,With apostrophe's");

        List<String> categoriesLonger = AppProvider.Helper.categories(context);
        String[] expectedLonger = new String[] {
                context.getResources().getString(R.string.category_Whats_New),
                context.getResources().getString(R.string.category_Recently_Updated),
                context.getResources().getString(R.string.category_All),

                "Animal",
                "Mineral",
                "Vegetable",

                "Running",
                "Shooting",
                "Jumping",
                "Bleh",
                "Sneh",
                "Pleh",
                "Blah",
                "Test category",
                "The quick brown fox jumps over the lazy dog",
                "With apostrophe's",
        };

        assertContainsOnly(categoriesLonger, expectedLonger);
    }

    // =======================================================================
    //  Misc helper functions
    //  (to be used by any tests in this suite)
    // =======================================================================

    private void insertApp(String id, String name) {
        insertApp(id, name, new ContentValues());
    }

    private void insertAppWithCategory(String id, String name, String categories) {
        ContentValues values = new ContentValues(1);
        values.put(Cols.CATEGORIES, categories);
        insertApp(id, name, values);
    }

    public App insertApp(String id, String name, ContentValues additionalValues) {

        ContentValues values = new ContentValues();
        values.put(Cols.PACKAGE_NAME, id);
        values.put(Cols.NAME, name);

        // Required fields (NOT NULL in the database).
        values.put(Cols.SUMMARY, "test summary");
        values.put(Cols.DESCRIPTION, "test description");
        values.put(Cols.LICENSE, "GPL?");
        values.put(Cols.IS_COMPATIBLE, 1);

        values.putAll(additionalValues);

        Uri uri = AppProvider.getContentUri();

        contentResolver.insert(uri, values);
        return AppProvider.Helper.findByPackageName(context.getContentResolver(), id);
    }
}
