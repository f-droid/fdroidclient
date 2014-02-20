package org.fdroid.fdroid;

import android.content.ContentValues;
import android.database.Cursor;

import mock.MockCategoryResources;
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
        getSwappableContext().setResources(new MockCategoryResources());
    }

    @Override
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

    private void insertApps(int count) {
        for (int i = 0; i < count; i ++) {
            insertApp("com.example.test." + i, "Test app " + i);
        }
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
