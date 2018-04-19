package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Schema.InstalledAppTable.Cols;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

import static org.fdroid.fdroid.Assert.assertIsInstalledVersionInDb;
import static org.fdroid.fdroid.Assert.assertResultCount;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class InstalledAppProviderTest extends FDroidProviderTest {

    @Before
    public void setup() {
        TestUtils.registerContentProvider(InstalledAppProvider.getAuthority(), InstalledAppProvider.class);
        Preferences.setupForTests(context);
    }

    @Test
    public void insertSingleApp() {
        Map<String, Long> foundBefore = InstalledAppProvider.Helper.all(RuntimeEnvironment.application);
        assertEquals(foundBefore.size(), 0);

        ContentValues values = new ContentValues();
        values.put(Cols.Package.NAME, "org.example.test-app");
        values.put(Cols.APPLICATION_LABEL, "Test App");
        values.put(Cols.VERSION_CODE, 1021);
        values.put(Cols.VERSION_NAME, "Longhorn");
        values.put(Cols.HASH, "has of test app");
        values.put(Cols.HASH_TYPE, "fake hash type");
        values.put(Cols.LAST_UPDATE_TIME, 100000000L);
        values.put(Cols.SIGNATURE, "000111222333444555666777888999aaabbbcccdddeeefff");
        contentResolver.insert(InstalledAppProvider.getContentUri(), values);

        Map<String, Long> foundAfter = InstalledAppProvider.Helper.all(RuntimeEnvironment.application);
        assertEquals(1, foundAfter.size());
        assertEquals(100000000L, foundAfter.get("org.example.test-app").longValue());

        Cursor cursor = contentResolver.query(InstalledAppProvider.getAppUri("org.example.test-app"), Cols.ALL,
                null, null, null);
        assertEquals(cursor.getCount(), 1);

        cursor.moveToFirst();
        assertEquals("org.example.test-app", cursor.getString(cursor.getColumnIndex(Cols.Package.NAME)));
        assertEquals("Test App", cursor.getString(cursor.getColumnIndex(Cols.APPLICATION_LABEL)));
        assertEquals(1021, cursor.getInt(cursor.getColumnIndex(Cols.VERSION_CODE)));
        assertEquals("Longhorn", cursor.getString(cursor.getColumnIndex(Cols.VERSION_NAME)));
        assertEquals("has of test app", cursor.getString(cursor.getColumnIndex(Cols.HASH)));
        assertEquals("fake hash type", cursor.getString(cursor.getColumnIndex(Cols.HASH_TYPE)));
        assertEquals(100000000L, cursor.getLong(cursor.getColumnIndex(Cols.LAST_UPDATE_TIME)));
        assertEquals("000111222333444555666777888999aaabbbcccdddeeefff",
                cursor.getString(cursor.getColumnIndex(Cols.SIGNATURE)));

        cursor.close();
    }

    @Test
    public void testInsert() {

        assertResultCount(contentResolver, 0, InstalledAppProvider.getContentUri());

        insertInstalledApp("com.example.com1", 1, "v1");
        insertInstalledApp("com.example.com2", 2, "v2");
        insertInstalledApp("com.example.com3", 3, "v3");

        assertResultCount(contentResolver, 3, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb(contentResolver, "com.example.com1", 1, "v1");
        assertIsInstalledVersionInDb(contentResolver, "com.example.com2", 2, "v2");
        assertIsInstalledVersionInDb(contentResolver, "com.example.com3", 3, "v3");
    }

    @Test
    public void testUpdate() {
        insertInstalledApp("com.example.app1", 10, "1.0");
        insertInstalledApp("com.example.app2", 10, "1.0");

        assertResultCount(contentResolver, 2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb(contentResolver, "com.example.app2", 10, "1.0");

        contentResolver.insert(
                InstalledAppProvider.getContentUri(),
                createContentValues("com.example.app2", 11, "1.1")
        );

        assertResultCount(contentResolver, 2, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb(contentResolver, "com.example.app2", 11, "1.1");
    }

    /**
     * We expect this to happen, because we should be using insert() instead as it will
     * do an insert/replace query.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateFails() {
        contentResolver.update(
                InstalledAppProvider.getAppUri("com.example.app2"),
                createContentValues(11, "1.1"),
                null, null
        );
    }

    @Test
    public void testLastUpdateTime() {
        String packageName = "com.example.app";

        insertInstalledApp(packageName, 10, "1.0");
        assertResultCount(contentResolver, 1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb(contentResolver, packageName, 10, "1.0");

        Uri uri = InstalledAppProvider.getAppUri(packageName);

        String[] projection = {
                Cols.Package.NAME,
                Cols.LAST_UPDATE_TIME,
        };

        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        assertNotNull(cursor);
        assertEquals("App \"" + packageName + "\" not installed", 1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(packageName, cursor.getString(cursor.getColumnIndex(Cols.Package.NAME)));
        long lastUpdateTime = cursor.getLong(cursor.getColumnIndex(Cols.LAST_UPDATE_TIME));
        assertTrue(lastUpdateTime > 0);
        assertTrue(lastUpdateTime < System.currentTimeMillis());
        cursor.close();

        insertInstalledApp(packageName, 11, "1.1");
        cursor = contentResolver.query(uri, projection, null, null, null);
        assertNotNull(cursor);
        assertEquals("App \"" + packageName + "\" not installed", 1, cursor.getCount());
        cursor.moveToFirst();
        assertTrue(lastUpdateTime < cursor.getLong(cursor.getColumnIndex(Cols.LAST_UPDATE_TIME)));
        cursor.close();
    }

    @Test
    public void testDelete() {

        insertInstalledApp("com.example.app1", 10, "1.0");
        insertInstalledApp("com.example.app2", 10, "1.0");

        assertResultCount(contentResolver, 2, InstalledAppProvider.getContentUri());

        contentResolver.delete(InstalledAppProvider.getAppUri("com.example.app1"), null, null);

        assertResultCount(contentResolver, 1, InstalledAppProvider.getContentUri());
        assertIsInstalledVersionInDb(contentResolver, "com.example.app2", 10, "1.0");

    }

    private ContentValues createContentValues(int versionCode, String versionNumber) {
        return createContentValues(null, versionCode, versionNumber);
    }

    private ContentValues createContentValues(String appId, int versionCode, String versionNumber) {
        ContentValues values = new ContentValues(3);
        if (appId != null) {
            values.put(Cols.Package.NAME, appId);
        }
        values.put(Cols.APPLICATION_LABEL, "Mock app: " + appId);
        values.put(Cols.VERSION_CODE, versionCode);
        values.put(Cols.VERSION_NAME, versionNumber);
        values.put(Cols.SIGNATURE, "");
        values.put(Cols.LAST_UPDATE_TIME, System.currentTimeMillis());
        values.put(Cols.HASH_TYPE, "sha256");
        values.put(Cols.HASH, "cafecafecafecafecafecafecafecafecafecafecafecafecafecafecafecafe");
        return values;
    }

    private void insertInstalledApp(String appId, int versionCode, String versionNumber) {
        ContentValues values = createContentValues(appId, versionCode, versionNumber);
        contentResolver.insert(InstalledAppProvider.getContentUri(), values);
    }
}

// https://github.com/robolectric/robolectric/wiki/2.4-to-3.0-Upgrade-Guide
