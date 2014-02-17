package org.fdroid.fdroid;

import android.content.ContentValues;
import android.database.Cursor;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.mock.MockApk;

import java.util.ArrayList;
import java.util.List;

public class ApkProviderTest extends FDroidProviderTest<ApkProvider> {

    public ApkProviderTest() {
        super(ApkProvider.class, ApkProvider.getAuthority());
    }

    protected String[] getMinimalProjection() {
        return new String[] {
            ApkProvider.DataColumns.APK_ID,
            ApkProvider.DataColumns.VERSION_CODE,
            ApkProvider.DataColumns.NAME
        };
    }

    public void testUris() {
        assertInvalidUri(ApkProvider.getAuthority());
        assertInvalidUri(AppProvider.getContentUri());

        List<Apk> apks = new ArrayList<Apk>(3);
        for (int i = 0; i < 10; i ++) {
            apks.add(new MockApk("com.example." + i, i));
        }

        assertValidUri(ApkProvider.getContentUri());
        assertValidUri(ApkProvider.getAppUri("org.fdroid.fdroid"));
        assertValidUri(ApkProvider.getContentUri(new MockApk("org.fdroid.fdroid", 100)));
        assertValidUri(ApkProvider.getContentUri());
        assertValidUri(ApkProvider.getContentUri(apks));
        assertValidUri(ApkProvider.getContentUri("org.fdroid.fdroid", 100));
        assertValidUri(ApkProvider.getRepoUri(1000));

        List<Apk> manyApks = new ArrayList<Apk>(ApkProvider.MAX_APKS_TO_QUERY - 5);
        for (int i = 0; i < ApkProvider.MAX_APKS_TO_QUERY - 1; i ++) {
            manyApks.add(new MockApk("com.example." + i, i));
        }
        assertValidUri(ApkProvider.getContentUri(manyApks));

        manyApks.add(new MockApk("org.fdroid.fdroid.1", 1));
        manyApks.add(new MockApk("org.fdroid.fdroid.2", 2));
        try {
            // Technically, it is a valid URI, because it doesn't
            // throw an UnsupportedOperationException. However it
            // is still not okay (we run out of bindable parameters
            // in the sqlite query.
            assertValidUri(ApkProvider.getContentUri(manyApks));
            fail();
        } catch (IllegalArgumentException e) {
            // This is the expected error behaviour.
        } catch (Exception e) {
            fail();
        }

    }

    public void testQuery() {
        Cursor cursor = queryAllApks();
        assertNotNull(cursor);
    }

    private void insertApks(int count) {
        for (int i = 0; i < count; i ++) {
            insertApk("com.example.test." + i, i);
        }
    }

    public void testInsert() {

        // Start with an empty database...
        Cursor cursor = queryAllApks();
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());

        // Insert a new record...
        insertApk("org.fdroid.fdroid", 13);
        cursor = queryAllApks();
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());

        // We intentionally throw an IllegalArgumentException if you haven't
        // yet called cursor.move*()...
        try {
            new Apk(cursor);
            fail();
        } catch (IllegalArgumentException e) {
            // Success!
        } catch (Exception e) {
            fail();
        }

        // And now we should be able to recover these values from the apk
        // value object (because the queryAllApks() helper asks for VERSION_CODE and
        // APK_ID.
        cursor.moveToFirst();
        Apk apk = new Apk(cursor);
        assertEquals("org.fdroid.fdroid", apk.id);
        assertEquals(13, apk.vercode);
    }

    public void testIgnore() {
        for (int i = 0; i < 10; i ++) {
            insertApk("org.fdroid.fdroid", i);
        }

    }

    private Cursor queryAllApks() {
        return getMockContentResolver().query(ApkProvider.getContentUri(), getMinimalProjection(), null, null, null);
    }

    private void insertApk(String id, int versionCode) {
        insertApk(id, versionCode, new ContentValues());
    }

    private void insertApk(String id, int versionCode,
                           ContentValues additionalValues) {
        TestUtils.insertApk(getMockContentResolver(), id, versionCode, additionalValues);
    }

}
