package org.fdroid.fdroid;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.mock.MockApk;
import org.fdroid.fdroid.mock.MockApp;
import org.fdroid.fdroid.mock.MockRepo;

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
            ApkProvider.DataColumns.NAME,
            ApkProvider.DataColumns.REPO_ID
        };
    }

    public void testUris() {
        assertInvalidUri(ApkProvider.getAuthority());
        assertInvalidUri(RepoProvider.getContentUri());

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

    public void testAppApks() {
        for (int i = 1; i <= 10; i ++) {
            insertApk("org.fdroid.fdroid", i);
            insertApk("com.example", i);
        }

        assertTotalApkCount(20);

        Cursor fdroidApks = getMockContentResolver().query(
                ApkProvider.getAppUri("org.fdroid.fdroid"),
                getMinimalProjection(),
                null, null, null);
        assertResultCount(10, fdroidApks);
        assertBelongsToApp(fdroidApks, "org.fdroid.fdroid");

        Cursor exampleApks = getMockContentResolver().query(
                ApkProvider.getAppUri("com.example"),
                getMinimalProjection(),
                null, null, null);
        assertResultCount(10, exampleApks);
        assertBelongsToApp(exampleApks, "com.example");

        ApkProvider.Helper.deleteApksByApp(getMockContext(), new MockApp("com.example"));

        Cursor all = queryAllApks();
        assertResultCount(10, all);
        assertBelongsToApp(all, "org.fdroid.fdroid");
    }

    public void testInvalidDeleteUris() {
        assertCantDelete(ApkProvider.getContentUri());
        assertCantDelete(ApkProvider.getContentUri(new ArrayList<Apk>()));
        assertCantDelete(ApkProvider.getContentUri("org.fdroid.fdroid", 10));
        assertCantDelete(ApkProvider.getContentUri(new MockApk("org.fdroid.fdroid", 10)));

        try {
            getMockContentResolver().delete(RepoProvider.getContentUri(), null, null);
            fail();
        } catch (IllegalArgumentException e) {
            // Don't fail, it is what we were looking for...
        } catch (Exception e) {
            fail();
        }
    }

    public void testRepoApks() {

        final long REPO_KEEP = 1;
        final long REPO_DELETE = 2;

        // Insert apks into two repos, one of which we will later purge the
        // the apks from.
        for (int i = 1; i <= 5; i ++) {
            insertApkForRepo("org.fdroid.fdroid", i, REPO_KEEP);
            insertApkForRepo("com.example." + i, 1, REPO_DELETE);
        }
        for (int i = 6; i <= 10; i ++) {
            insertApkForRepo("org.fdroid.fdroid", i, REPO_DELETE);
            insertApkForRepo("com.example." + i, 1, REPO_KEEP);
        }

        assertTotalApkCount(20);

        Cursor cursor = getMockContentResolver().query(
            ApkProvider.getRepoUri(REPO_DELETE), getMinimalProjection(), null, null, null);
        assertResultCount(10, cursor);
        assertBelongsToRepo(cursor, REPO_DELETE);

        int count = ApkProvider.Helper.deleteApksByRepo(getMockContext(), new MockRepo(REPO_DELETE));
        assertEquals(10, count);

        assertTotalApkCount(10);
        cursor = getMockContentResolver().query(
            ApkProvider.getRepoUri(REPO_DELETE), getMinimalProjection(), null, null, null);
        assertResultCount(0, cursor);

        // The only remaining apks should be those from REPO_KEEP.
        assertBelongsToRepo(queryAllApks(), REPO_KEEP);
    }

    public void testQuery() {
        Cursor cursor = queryAllApks();
        assertNotNull(cursor);
    }

    public void testInsert() {

        // Start with an empty database...
        Cursor cursor = queryAllApks();
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());

        Apk apk = new MockApk("org.fdroid.fdroid", 13);

        // Insert a new record...
        Uri newUri = insertApk(apk.id, apk.vercode);
        assertEquals(ApkProvider.getContentUri(apk).toString(), newUri.toString());
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
        Apk toCheck = new Apk(cursor);
        assertEquals("org.fdroid.fdroid", toCheck.id);
        assertEquals(13, toCheck.vercode);
    }

    public void testInsertWithExtraFields() {

        assertResultCount(0, queryAllApks());

        String[] repoFields = new String[] {
            RepoProvider.DataColumns.DESCRIPTION,
            RepoProvider.DataColumns.ADDRESS,
            RepoProvider.DataColumns.FINGERPRINT,
            RepoProvider.DataColumns.NAME,
            RepoProvider.DataColumns.PUBLIC_KEY
        };

        for (String field : repoFields) {
            ContentValues invalidRepo = new ContentValues();
            invalidRepo.put(field, "Test data");
            try {
                insertApk("org.fdroid.fdroid", 10, invalidRepo);
                fail();
            } catch (IllegalArgumentException e) {
            } catch (Exception e) {
                fail();
            }
            assertResultCount(0, queryAllApks());
        }

        // ApkProvider.DataColumns.REPO

    }

    public void testIgnore() {
        /*for (int i = 0; i < 10; i ++) {
            insertApk("org.fdroid.fdroid", i);
        }*/
    }

    private void assertBelongsToApp(Cursor apks, String appId) {
        for (Apk apk : ApkProvider.Helper.cursorToList(apks)) {
            assertEquals(appId, apk.id);
        }
    }

    private void assertTotalApkCount(int expected) {
        assertResultCount(expected, queryAllApks());
    }

    private void assertBelongsToRepo(Cursor apkCursor, long repoId) {
        for (Apk apk : ApkProvider.Helper.cursorToList(apkCursor)) {
            assertEquals(repoId, apk.repo);
        }
    }

    private void insertApkForRepo(String id, int versionCode, long repoId) {
        ContentValues additionalValues = new ContentValues();
        additionalValues.put(ApkProvider.DataColumns.REPO_ID, repoId);
        insertApk(id, versionCode, additionalValues);
    }

    private Cursor queryAllApks() {
        return getMockContentResolver().query(ApkProvider.getContentUri(), getMinimalProjection(), null, null, null);
    }

    private Uri insertApk(String id, int versionCode) {
        return insertApk(id, versionCode, new ContentValues());
    }

    private Uri insertApk(String id, int versionCode,
                           ContentValues additionalValues) {
        return TestUtils.insertApk(getMockContentResolver(), id, versionCode, additionalValues);
    }

}
