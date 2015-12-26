package org.fdroid.fdroid;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;

import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.InstalledAppCacheUpdater;

import java.util.ArrayList;
import java.util.List;

import mock.MockCategoryResources;
import mock.MockContextSwappableComponents;
import mock.MockInstallablePackageManager;

public class AppProviderTest extends FDroidProviderTest<AppProvider> {

    public AppProviderTest() {
        super(AppProvider.class, AppProvider.getAuthority());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getSwappableContext().setResources(new MockCategoryResources(getContext()));
    }

    @Override
    protected Resources getMockResources() {
        return new MockCategoryResources(getContext());
    }

    @Override
    protected String[] getMinimalProjection() {
        return new String[] {
            AppProvider.DataColumns.PACKAGE_NAME,
            AppProvider.DataColumns.NAME,
        };
    }

    /**
     * Although this doesn't directly relate to the AppProvider, it is here because
     * the AppProvider used to stumble across this bug when asking for installed apps,
     * and the device had over 1000 apps installed.
     */
    public void testMaxSqliteParams() {

        MockInstallablePackageManager pm = new MockInstallablePackageManager();
        getSwappableContext().setPackageManager(pm);

        insertApp("com.example.app1", "App 1");
        insertApp("com.example.app100", "App 100");
        insertApp("com.example.app1000", "App 1000");

        for (int i = 0; i < 50; i++) {
            pm.install("com.example.app" + i, 1, "v" + 1);
        }
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(1, AppProvider.getInstalledUri());

        for (int i = 50; i < 500; i++) {
            pm.install("com.example.app" + i, 1, "v" + 1);
        }
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(2, AppProvider.getInstalledUri());

        for (int i = 500; i < 1100; i++) {
            pm.install("com.example.app" + i, 1, "v" + 1);
        }
        InstalledAppCacheUpdater.updateInForeground(getMockContext());

        assertResultCount(3, AppProvider.getInstalledUri());
    }

    public void testCantFindApp() {
        assertNull(AppProvider.Helper.findByPackageName(getMockContentResolver(), "com.example.doesnt-exist"));
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
        app.packageName = "org.fdroid.fdroid";

        List<App> apps = new ArrayList<>(1);
        apps.add(app);

        assertValidUri(AppProvider.getContentUri(app));
        assertValidUri(AppProvider.getContentUri(apps));
        assertValidUri(AppProvider.getContentUri("org.fdroid.fdroid"));
    }

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
            MockInstallablePackageManager packageManager,
            String id, int installedVercode, int suggestedVercode,
            boolean ignoreAll, int ignoreVercode) {
        ContentValues values = new ContentValues(3);
        values.put(AppProvider.DataColumns.SUGGESTED_VERSION_CODE, suggestedVercode);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, ignoreAll);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, ignoreVercode);
        insertApp(id, "App: " + id, values);

        TestUtils.installAndBroadcast(getSwappableContext(), packageManager, id, installedVercode, "v" + installedVercode);
    }

    public void testCanUpdate() {

        MockContextSwappableComponents c = getSwappableContext();

        MockInstallablePackageManager pm = new MockInstallablePackageManager();
        c.setPackageManager(pm);

        insertApp("not installed", "not installed");
        insertAndInstallApp(pm, "installed, only one version available", 1, 1, false, 0);
        insertAndInstallApp(pm, "installed, already latest, no ignore", 10, 10, false, 0);
        insertAndInstallApp(pm, "installed, already latest, ignore all", 10, 10, true, 0);
        insertAndInstallApp(pm, "installed, already latest, ignore latest", 10, 10, false, 10);
        insertAndInstallApp(pm, "installed, already latest, ignore old", 10, 10, false, 5);
        insertAndInstallApp(pm, "installed, old version, no ignore", 5, 10, false, 0);
        insertAndInstallApp(pm, "installed, old version, ignore all", 5, 10, true, 0);
        insertAndInstallApp(pm, "installed, old version, ignore latest", 5, 10, false, 10);
        insertAndInstallApp(pm, "installed, old version, ignore newer, but not latest", 5, 10, false, 8);

        ContentResolver r = getMockContentResolver();

        // Can't "update", although can "install"...
        App notInstalled = AppProvider.Helper.findByPackageName(r, "not installed");
        assertFalse(notInstalled.canAndWantToUpdate());

        App installedOnlyOneVersionAvailable   = AppProvider.Helper.findByPackageName(r, "installed, only one version available");
        App installedAlreadyLatestNoIgnore     = AppProvider.Helper.findByPackageName(r, "installed, already latest, no ignore");
        App installedAlreadyLatestIgnoreAll    = AppProvider.Helper.findByPackageName(r, "installed, already latest, ignore all");
        App installedAlreadyLatestIgnoreLatest = AppProvider.Helper.findByPackageName(r, "installed, already latest, ignore latest");
        App installedAlreadyLatestIgnoreOld    = AppProvider.Helper.findByPackageName(r, "installed, already latest, ignore old");

        assertFalse(installedOnlyOneVersionAvailable.canAndWantToUpdate());
        assertFalse(installedAlreadyLatestNoIgnore.canAndWantToUpdate());
        assertFalse(installedAlreadyLatestIgnoreAll.canAndWantToUpdate());
        assertFalse(installedAlreadyLatestIgnoreLatest.canAndWantToUpdate());
        assertFalse(installedAlreadyLatestIgnoreOld.canAndWantToUpdate());

        App installedOldNoIgnore             = AppProvider.Helper.findByPackageName(r, "installed, old version, no ignore");
        App installedOldIgnoreAll            = AppProvider.Helper.findByPackageName(r, "installed, old version, ignore all");
        App installedOldIgnoreLatest         = AppProvider.Helper.findByPackageName(r, "installed, old version, ignore latest");
        App installedOldIgnoreNewerNotLatest = AppProvider.Helper.findByPackageName(r, "installed, old version, ignore newer, but not latest");

        assertTrue(installedOldNoIgnore.canAndWantToUpdate());
        assertFalse(installedOldIgnoreAll.canAndWantToUpdate());
        assertFalse(installedOldIgnoreLatest.canAndWantToUpdate());
        assertTrue(installedOldIgnoreNewerNotLatest.canAndWantToUpdate());

        Cursor canUpdateCursor = r.query(AppProvider.getCanUpdateUri(), AppProvider.DataColumns.ALL, null, null, null);
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

        TestUtils.assertContainsOnly(expectedUpdateableIds, canUpdateIds);
    }

    public void testIgnored() {

        MockInstallablePackageManager pm = new MockInstallablePackageManager();
        getSwappableContext().setPackageManager(pm);

        insertApp("not installed", "not installed");
        insertAndInstallApp(pm, "installed, only one version available", 1, 1, false, 0);
        insertAndInstallApp(pm, "installed, already latest, no ignore", 10, 10, false, 0);
        insertAndInstallApp(pm, "installed, already latest, ignore all", 10, 10, true, 0);
        insertAndInstallApp(pm, "installed, already latest, ignore latest", 10, 10, false, 10);
        insertAndInstallApp(pm, "installed, already latest, ignore old", 10, 10, false, 5);
        insertAndInstallApp(pm, "installed, old version, no ignore", 5, 10, false, 0);
        insertAndInstallApp(pm, "installed, old version, ignore all", 5, 10, true, 0);
        insertAndInstallApp(pm, "installed, old version, ignore latest", 5, 10, false, 10);
        insertAndInstallApp(pm, "installed, old version, ignore newer, but not latest", 5, 10, false, 8);

        assertResultCount(10, AppProvider.getContentUri());

        String[] projection = {AppProvider.DataColumns.PACKAGE_NAME};
        List<App> ignoredApps = AppProvider.Helper.findIgnored(getMockContext(), projection);

        String[] expectedIgnored = {
            "installed, already latest, ignore all",
            "installed, already latest, ignore latest",
            // NOT "installed, already latest, ignore old" - because it
            // is should only ignore if "ignored version" is >= suggested

            "installed, old version, ignore all",
            "installed, old version, ignore latest",
            // NOT "installed, old version, ignore newer, but not latest"
            // for the same reason as above.
        };

        assertContainsOnlyIds(ignoredApps, expectedIgnored);
    }

    private void assertContainsOnlyIds(List<App> actualApps, String[] expectedIds) {
        List<String> actualIds = new ArrayList<>(actualApps.size());
        for (App app : actualApps) {
            actualIds.add(app.packageName);
        }
        TestUtils.assertContainsOnly(actualIds, expectedIds);
    }

    public void testInstalled() {
        MockInstallablePackageManager pm = new MockInstallablePackageManager();
        getSwappableContext().setPackageManager(pm);

        insertApps(100);

        assertResultCount(100, AppProvider.getContentUri());
        assertResultCount(0, AppProvider.getInstalledUri());

        for (int i = 10; i < 20; i++) {
            TestUtils.installAndBroadcast(getSwappableContext(), pm, "com.example.test." + i, i, "v1");
        }

        assertResultCount(10, AppProvider.getInstalledUri());
    }

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
        // PACKAGE_NAME.
        cursor.moveToFirst();
        App app = new App(cursor);
        cursor.close();
        assertEquals("org.fdroid.fdroid", app.packageName);
        assertEquals("F-Droid", app.name);
    }

    private Cursor queryAllApps() {
        return getMockContentResolver().query(AppProvider.getContentUri(), getMinimalProjection(), null, null, null);
    }

    // ========================================================================
    //  "Categories"
    //  (at this point) not an additional table, but we treat them sort of
    //  like they are. That means that if we change the implementation to
    //  use a separate table in the future, these should still pass.
    // ========================================================================

    public void testCategoriesSingle() {
        insertAppWithCategory("com.dog", "Dog", "Animal");
        insertAppWithCategory("com.rock", "Rock", "Mineral");
        insertAppWithCategory("com.banana", "Banana", "Vegetable");

        List<String> categories = AppProvider.Helper.categories(getMockContext());
        String[] expected = new String[] {
            getMockContext().getResources().getString(R.string.category_Whats_New),
            getMockContext().getResources().getString(R.string.category_Recently_Updated),
            getMockContext().getResources().getString(R.string.category_All),
            "Animal",
            "Mineral",
            "Vegetable",
        };
        TestUtils.assertContainsOnly(categories, expected);
    }

    public void testCategoriesMultiple() {
        insertAppWithCategory("com.rock.dog", "Rock-Dog", "Mineral,Animal");
        insertAppWithCategory("com.dog.rock.apple", "Dog-Rock-Apple", "Animal,Mineral,Vegetable");
        insertAppWithCategory("com.banana.apple", "Banana", "Vegetable,Vegetable");

        List<String> categories = AppProvider.Helper.categories(getMockContext());
        String[] expected = new String[] {
            getMockContext().getResources().getString(R.string.category_Whats_New),
            getMockContext().getResources().getString(R.string.category_Recently_Updated),
            getMockContext().getResources().getString(R.string.category_All),

            "Animal",
            "Mineral",
            "Vegetable",
        };
        TestUtils.assertContainsOnly(categories, expected);

        insertAppWithCategory("com.example.game", "Game",
                "Running,Shooting,Jumping,Bleh,Sneh,Pleh,Blah,Test category," +
                "The quick brown fox jumps over the lazy dog,With apostrophe's");

        List<String> categoriesLonger = AppProvider.Helper.categories(getMockContext());
        String[] expectedLonger = new String[] {
            getMockContext().getResources().getString(R.string.category_Whats_New),
            getMockContext().getResources().getString(R.string.category_Recently_Updated),
            getMockContext().getResources().getString(R.string.category_All),

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

        TestUtils.assertContainsOnly(categoriesLonger, expectedLonger);
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
        values.put(AppProvider.DataColumns.CATEGORIES, categories);
        insertApp(id, name, values);
    }

    private void insertApp(String id, String name,
                           ContentValues additionalValues) {
        TestUtils.insertApp(getMockContentResolver(), id, name, additionalValues);
    }

}
