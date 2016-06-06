package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import junit.framework.AssertionFailedError;

import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ProviderTestUtils {

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
        Cursor cursor = resolver.query(uri, new String[] {}, null, null, null);
        assertNull(cursor);
    }

    public static void assertValidUri(ShadowContentResolver resolver, Uri uri, String[] projection) {
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        assertNotNull(cursor);
        cursor.close();
    }

    public static void assertValidUri(ShadowContentResolver resolver, Uri actualUri, String expectedUri, String[] projection) {
        assertValidUri(resolver, actualUri, projection);
        assertEquals(expectedUri, actualUri.toString());
    }

    public static void assertResultCount(ShadowContentResolver resolver, int expectedCount, Uri uri) {
        assertResultCount(resolver, expectedCount, uri, new String[] {});
    }

    public static void assertResultCount(ShadowContentResolver resolver, int expectedCount, Uri uri, String[] projection) {
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

    public static void assertIsInstalledVersionInDb(ShadowContentResolver resolver, String appId, int versionCode, String versionName) {
        Uri uri = InstalledAppProvider.getAppUri(appId);

        String[] projection = {
                InstalledAppProvider.DataColumns.PACKAGE_NAME,
                InstalledAppProvider.DataColumns.VERSION_CODE,
                InstalledAppProvider.DataColumns.VERSION_NAME,
                InstalledAppProvider.DataColumns.APPLICATION_LABEL,
        };

        Cursor cursor = resolver.query(uri, projection, null, null, null);

        assertNotNull(cursor);
        assertEquals("App \"" + appId + "\" not installed", 1, cursor.getCount());

        cursor.moveToFirst();

        assertEquals(appId, cursor.getString(cursor.getColumnIndex(InstalledAppProvider.DataColumns.PACKAGE_NAME)));
        assertEquals(versionCode, cursor.getInt(cursor.getColumnIndex(InstalledAppProvider.DataColumns.VERSION_CODE)));
        assertEquals(versionName, cursor.getString(cursor.getColumnIndex(InstalledAppProvider.DataColumns.VERSION_NAME)));
        cursor.close();
    }

    public static void insertApp(ShadowContentResolver resolver, String appId, String name) {
        insertApp(resolver, appId, name, new ContentValues());
    }

    public static void insertApp(ShadowContentResolver resolver, String id, String name, ContentValues additionalValues) {

        ContentValues values = new ContentValues();
        values.put(AppProvider.DataColumns.PACKAGE_NAME, id);
        values.put(AppProvider.DataColumns.NAME, name);

        // Required fields (NOT NULL in the database).
        values.put(AppProvider.DataColumns.SUMMARY, "test summary");
        values.put(AppProvider.DataColumns.DESCRIPTION, "test description");
        values.put(AppProvider.DataColumns.LICENSE, "GPL?");
        values.put(AppProvider.DataColumns.IS_COMPATIBLE, 1);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, 0);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, 0);

        values.putAll(additionalValues);

        Uri uri = AppProvider.getContentUri();

        resolver.insert(uri, values);
    }

    public static Uri insertApk(ShadowContentResolver resolver, String id, int versionCode) {
        return insertApk(resolver, id, versionCode, new ContentValues());
    }

    public static Uri insertApk(ShadowContentResolver resolver, String id, int versionCode, ContentValues additionalValues) {

        ContentValues values = new ContentValues();

        values.put(ApkProvider.DataColumns.PACKAGE_NAME, id);
        values.put(ApkProvider.DataColumns.VERSION_CODE, versionCode);

        // Required fields (NOT NULL in the database).
        values.put(ApkProvider.DataColumns.REPO_ID, 1);
        values.put(ApkProvider.DataColumns.VERSION_NAME, "The good one");
        values.put(ApkProvider.DataColumns.HASH, "11111111aaaaaaaa");
        values.put(ApkProvider.DataColumns.NAME, "Test Apk");
        values.put(ApkProvider.DataColumns.SIZE, 10000);
        values.put(ApkProvider.DataColumns.IS_COMPATIBLE, 1);

        values.putAll(additionalValues);

        Uri uri = ApkProvider.getContentUri();

        return resolver.insert(uri, values);
    }

}
