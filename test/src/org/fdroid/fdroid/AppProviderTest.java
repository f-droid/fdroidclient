package org.fdroid.fdroid;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;

import mock.MockCategoryResources;
import mock.MockContextSwappableComponents;
import mock.MockInstallablePackageManager;

import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;

import java.util.ArrayList;
import java.util.List;

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
            AppProvider.DataColumns.APP_ID,
            AppProvider.DataColumns.NAME
        };
    }

    public void testCantFindApp() {
        assertNull(AppProvider.Helper.findById(getMockContentResolver(), "com.example.doesnt-exist"));
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

    private void insertApps(int count) {
        for (int i = 0; i < count; i ++) {
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

        packageManager.install(id, installedVercode, "v" + installedVercode);
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
        App notInstalled = AppProvider.Helper.findById(r, "not installed");
        assertFalse(notInstalled.canAndWantToUpdate(c));

        App installedOnlyOneVersionAvailable   = AppProvider.Helper.findById(r, "installed, only one version available");
        App installedAlreadyLatestNoIgnore     = AppProvider.Helper.findById(r, "installed, already latest, no ignore");
        App installedAlreadyLatestIgnoreAll    = AppProvider.Helper.findById(r, "installed, already latest, ignore all");
        App installedAlreadyLatestIgnoreLatest = AppProvider.Helper.findById(r, "installed, already latest, ignore latest");
        App installedAlreadyLatestIgnoreOld    = AppProvider.Helper.findById(r, "installed, already latest, ignore old");

        assertFalse(installedOnlyOneVersionAvailable.canAndWantToUpdate(c));
        assertFalse(installedAlreadyLatestNoIgnore.canAndWantToUpdate(c));
        assertFalse(installedAlreadyLatestIgnoreAll.canAndWantToUpdate(c));
        assertFalse(installedAlreadyLatestIgnoreLatest.canAndWantToUpdate(c));
        assertFalse(installedAlreadyLatestIgnoreOld.canAndWantToUpdate(c));

        App installedOldNoIgnore             = AppProvider.Helper.findById(r, "installed, old version, no ignore");
        App installedOldIgnoreAll            = AppProvider.Helper.findById(r, "installed, old version, ignore all");
        App installedOldIgnoreLatest         = AppProvider.Helper.findById(r, "installed, old version, ignore latest");
        App installedOldIgnoreNewerNotLatest = AppProvider.Helper.findById(r, "installed, old version, ignore newer, but not latest");

        assertTrue(installedOldNoIgnore.canAndWantToUpdate(c));
        assertFalse(installedOldIgnoreAll.canAndWantToUpdate(c));
        assertFalse(installedOldIgnoreLatest.canAndWantToUpdate(c));
        assertTrue(installedOldIgnoreNewerNotLatest.canAndWantToUpdate(c));
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

        String[] projection = { AppProvider.DataColumns.APP_ID };
        List<App> ignoredApps = AppProvider.Helper.findIgnored(getMockContext(), projection);

        String[] expectedIgnored = {
            "installed, already latest, ignore all",
            "installed, already latest, ignore latest",
            // NOT "installed, already latest, ignore old" - because it
            // is should only ignore if "ignored version" is >= suggested

            "installed, old version, ignore all",
            "installed, old version, ignore latest"
            // NOT "installed, old version, ignore newer, but not latest"
            // for the same reason as above.
        };

        assertContainsOnlyIds(ignoredApps, expectedIgnored);
    }

    private void assertContainsOnlyIds(List<App> actualApps, String[] expectedIds) {
        List<String> actualIds = new ArrayList<String>(actualApps.size());
        for (App app : actualApps) {
            actualIds.add(app.id);
        }
        TestUtils.assertContainsOnly(actualIds, expectedIds);
    }

    public void testInstalled() {

        Utils.clearInstalledApksCache();

        MockInstallablePackageManager pm = new MockInstallablePackageManager();
        getSwappableContext().setPackageManager(pm);

        insertApps(100);

        assertResultCount(100, AppProvider.getContentUri());
        assertResultCount(0, AppProvider.getInstalledUri());

        for (int i = 10; i < 20; i ++) {
            pm.install("com.example.test." + i, i, "v1");
        }

        assertResultCount(10, AppProvider.getInstalledUri());
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
        return getMockContentResolver().query(AppProvider.getContentUri(), getMinimalProjection(), null, null, null);
    }

    public void testCategoriesSingle() {
        insertAppWithCategory("com.dog", "Dog", "Animal");
        insertAppWithCategory("com.rock", "Rock", "Mineral");
        insertAppWithCategory("com.banana", "Banana", "Vegetable");

        List<String> categories = AppProvider.Helper.categories(getMockContext());
        String[] expected = new String[] {
            getMockContext().getResources().getString(R.string.category_whatsnew),
            getMockContext().getResources().getString(R.string.category_recentlyupdated),
            getMockContext().getResources().getString(R.string.category_all),
            "Animal",
            "Mineral",
            "Vegetable"
        };
        TestUtils.assertContainsOnly(categories, expected);
    }

    public void testCategoriesMultiple() {
        insertAppWithCategory("com.rock.dog", "Rock-Dog", "Mineral,Animal");
        insertAppWithCategory("com.dog.rock.apple", "Dog-Rock-Apple", "Animal,Mineral,Vegetable");
        insertAppWithCategory("com.banana.apple", "Banana", "Vegetable,Vegetable");

        List<String> categories = AppProvider.Helper.categories(getMockContext());
        String[] expected = new String[] {
            getMockContext().getResources().getString(R.string.category_whatsnew),
            getMockContext().getResources().getString(R.string.category_recentlyupdated),
            getMockContext().getResources().getString(R.string.category_all),

            "Animal",
            "Mineral",
            "Vegetable"
        };
        TestUtils.assertContainsOnly(categories, expected);

        insertAppWithCategory("com.example.game", "Game",
                "Running,Shooting,Jumping,Bleh,Sneh,Pleh,Blah,Test category," +
                "The quick brown fox jumps over the lazy dog,With apostrophe's");

        List<String> categoriesLonger = AppProvider.Helper.categories(getMockContext());
        String[] expectedLonger = new String[] {
            getMockContext().getResources().getString(R.string.category_whatsnew),
            getMockContext().getResources().getString(R.string.category_recentlyupdated),
            getMockContext().getResources().getString(R.string.category_all),

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
            "With apostrophe's"
        };

        TestUtils.assertContainsOnly(categoriesLonger, expectedLonger);
    }

    private void insertApp(String id, String name) {
        insertApp(id, name, new ContentValues());
    }

    private void insertAppWithCategory(String id, String name,
                                       String categories) {
        ContentValues values = new ContentValues(1);
        values.put(AppProvider.DataColumns.CATEGORIES, categories);
        insertApp(id, name, values);
    }

    private void insertApp(String id, String name,
                           ContentValues additionalValues) {
        TestUtils.insertApp(getMockContentResolver(), id, name, additionalValues);
    }

}
