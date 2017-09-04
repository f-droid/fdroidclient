package org.fdroid.fdroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import junit.framework.AssertionFailedError;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class Assert {

    public static <T extends Comparable> void assertContainsOnly(List<T> actualList, T[] expectedArray) {
        List<T> expectedList = new ArrayList<>(expectedArray.length);
        Collections.addAll(expectedList, expectedArray);
        assertContainsOnly(actualList, expectedList);
    }

    public static <T extends Comparable> void assertContainsOnly(T[] actualArray, List<T> expectedList) {
        List<T> actualList = new ArrayList<>(actualArray.length);
        Collections.addAll(actualList, actualArray);
        assertContainsOnly(actualList, expectedList);
    }

    public static <T extends Comparable> void assertContainsOnly(T[] actualArray, T[] expectedArray) {
        List<T> expectedList = new ArrayList<>(expectedArray.length);
        Collections.addAll(expectedList, expectedArray);
        assertContainsOnly(actualArray, expectedList);
    }

    public static <T> String listToString(List<T> list) {
        String string = "[";
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                string += ", ";
            }
            string += "'" + list.get(i) + "'";
        }
        string += "]";
        return string;
    }

    public static <T extends Comparable> void assertContainsOnly(List<T> actualList, List<T> expectedContains) {
        if (actualList.size() != expectedContains.size()) {
            String message =
                    "List sizes don't match.\n" +
                            "Expected: " +
                            listToString(expectedContains) + "\n" +
                            "Actual:   " +
                            listToString(actualList);
            throw new AssertionFailedError(message);
        }
        for (T required : expectedContains) {
            boolean containsRequired = false;
            for (T itemInList : actualList) {
                if (required.equals(itemInList)) {
                    containsRequired = true;
                    break;
                }
            }
            if (!containsRequired) {
                String message =
                        "List doesn't contain \"" + required + "\".\n" +
                                "Expected: " +
                                listToString(expectedContains) + "\n" +
                                "Actual:   " +
                                listToString(actualList);
                throw new AssertionFailedError(message);
            }
        }
    }

    public static void assertCantDelete(ShadowContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
            fail();
        } catch (UnsupportedOperationException e) {
            // Successful condition
        } catch (Exception e) {
            fail();
        }
    }

    public static void assertCantUpdate(ShadowContentResolver resolver, Uri uri) {
        try {
            resolver.update(uri, new ContentValues(), null, null);
            fail();
        } catch (UnsupportedOperationException e) {
            // Successful condition
        } catch (Exception e) {
            fail();
        }
    }

    public static void assertInvalidUri(ShadowContentResolver resolver, String uri) {
        assertInvalidUri(resolver, Uri.parse(uri));
    }

    public static void assertValidUri(ShadowContentResolver resolver, String uri, String[] projection) {
        assertValidUri(resolver, Uri.parse(uri), projection);
    }

    public static void assertInvalidUri(ShadowContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, new String[]{}, null, null, null);
        assertNull(cursor);
    }

    public static void assertValidUri(ShadowContentResolver resolver, Uri uri, String[] projection) {
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        assertNotNull(cursor);
        cursor.close();
    }

    public static void assertValidUri(ShadowContentResolver resolver, Uri actualUri, String expectedUri,
                                      String[] projection) {
        assertValidUri(resolver, actualUri, projection);
        assertEquals(expectedUri, actualUri.toString());
    }

    public static void assertResultCount(ShadowContentResolver resolver, int expectedCount, Uri uri) {
        assertResultCount(resolver, expectedCount, uri, new String[]{});
    }

    public static void assertResultCount(ShadowContentResolver resolver, int expectedCount, Uri uri,
                                         String[] projection) {
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        assertResultCount(expectedCount, cursor);
        cursor.close();
    }

    public static void assertResultCount(int expectedCount, List items) {
        assertNotNull(items);
        assertEquals(expectedCount, items.size());
    }

    public static void assertResultCount(int expectedCount, Cursor result) {
        assertNotNull(result);
        assertEquals(expectedCount, result.getCount());
    }

    public static void assertIsInstalledVersionInDb(ShadowContentResolver resolver,
                                                    String appId, int versionCode, String versionName) {
        Uri uri = InstalledAppProvider.getAppUri(appId);

        String[] projection = {
                InstalledAppTable.Cols.Package.NAME,
                InstalledAppTable.Cols.VERSION_CODE,
                InstalledAppTable.Cols.VERSION_NAME,
                InstalledAppTable.Cols.APPLICATION_LABEL,
        };

        Cursor cursor = resolver.query(uri, projection, null, null, null);

        assertNotNull(cursor);
        assertEquals("App \"" + appId + "\" not installed", 1, cursor.getCount());

        cursor.moveToFirst();

        assertEquals(appId, cursor.getString(cursor.getColumnIndex(InstalledAppTable.Cols.Package.NAME)));
        assertEquals(versionCode, cursor.getInt(cursor.getColumnIndex(InstalledAppTable.Cols.VERSION_CODE)));
        assertEquals(versionName, cursor.getString(cursor.getColumnIndex(InstalledAppTable.Cols.VERSION_NAME)));
        cursor.close();
    }

    public static App insertApp(Context context, String packageName, String name) {
        return insertApp(context, packageName, name, new ContentValues());
    }

    public static App insertApp(Context context, String packageName, String name, Repo repo) {
        ContentValues values = new ContentValues();
        values.put(AppMetadataTable.Cols.REPO_ID, repo.getId());
        return insertApp(context, packageName, name, values);
    }

    public static App insertApp(Context context, String packageName, String name, ContentValues additionalValues) {

        ContentValues values = new ContentValues();
        values.put(AppMetadataTable.Cols.REPO_ID, 1);
        values.put(AppMetadataTable.Cols.Package.PACKAGE_NAME, packageName);
        values.put(AppMetadataTable.Cols.NAME, name);

        // Required fields (NOT NULL in the database).
        values.put(AppMetadataTable.Cols.SUMMARY, "test summary");
        values.put(AppMetadataTable.Cols.DESCRIPTION, "test description");
        values.put(AppMetadataTable.Cols.LICENSE, "GPL?");
        values.put(AppMetadataTable.Cols.IS_COMPATIBLE, 1);

        values.putAll(additionalValues);

        // Don't hard code to 1, let consumers override it in additionalValues then ask for it back.
        int repoId = values.getAsInteger(AppMetadataTable.Cols.REPO_ID);

        Uri uri = AppProvider.getContentUri();

        context.getContentResolver().insert(uri, values);
        App app = AppProvider.Helper.findSpecificApp(context.getContentResolver(), packageName,
                repoId, AppMetadataTable.Cols.ALL);
        assertNotNull(app);
        return app;
    }

    public static App ensureApp(Context context, String packageName) {
        App app = AppProvider.Helper.findSpecificApp(context.getContentResolver(), packageName, 1,
                AppMetadataTable.Cols.ALL);
        if (app == null) {
            insertApp(context, packageName, packageName);
            app = AppProvider.Helper.findSpecificApp(context.getContentResolver(), packageName, 1,
                    AppMetadataTable.Cols.ALL);
        }
        assertNotNull(app);
        return app;
    }

    public static Uri insertApk(Context context, String packageName, int versionCode) {
        return insertApk(context, ensureApp(context, packageName), versionCode);
    }

    public static Uri insertApk(Context context, String packageName, int versionCode,
                                ContentValues additionalValues) {
        return insertApk(context, ensureApp(context, packageName), versionCode, additionalValues);
    }

    public static Uri insertApk(Context context, App app, int versionCode) {
        return insertApk(context, app, versionCode, new ContentValues());
    }

    public static Uri insertApk(Context context, App app, int versionCode, ContentValues additionalValues) {

        ContentValues values = new ContentValues();

        values.put(ApkTable.Cols.APP_ID, app.getId());
        values.put(ApkTable.Cols.VERSION_CODE, versionCode);

        // Required fields (NOT NULL in the database).
        values.put(ApkTable.Cols.REPO_ID, 1);
        values.put(ApkTable.Cols.VERSION_NAME, "The good one");
        values.put(ApkTable.Cols.HASH, "11111111aaaaaaaa");
        values.put(ApkTable.Cols.NAME, "Test Apk");
        values.put(ApkTable.Cols.SIZE, 10000);
        values.put(ApkTable.Cols.IS_COMPATIBLE, 1);

        values.putAll(additionalValues);

        Uri uri = ApkProvider.getContentUri();

        return context.getContentResolver().insert(uri, values);
    }

}
