package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import org.fdroid.fdroid.Assert;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Schema.ApkTable.Cols;
import org.fdroid.fdroid.data.Schema.RepoTable;
import org.fdroid.fdroid.mock.MockApk;
import org.fdroid.fdroid.mock.MockRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.List;

import static org.fdroid.fdroid.Assert.assertCantDelete;
import static org.fdroid.fdroid.Assert.assertResultCount;
import static org.fdroid.fdroid.Assert.insertApp;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class ApkProviderTest extends FDroidProviderTest {

    private static final String[] PROJ = Cols.ALL;

    @Test
    public void testAppApks() {
        App fdroidApp = insertApp(context, "org.fdroid.fdroid", "F-Droid");
        App exampleApp = insertApp(context, "com.example", "Example");
        for (int i = 1; i <= 10; i++) {
            Assert.insertApk(context, fdroidApp, i);
            Assert.insertApk(context, exampleApp, i);
        }

        assertTotalApkCount(20);

        Cursor fdroidApks = contentResolver.query(
                ApkProvider.getAppUri("org.fdroid.fdroid"),
                PROJ,
                null, null, null);
        assertResultCount(10, fdroidApks);
        assertBelongsToApp(fdroidApks, "org.fdroid.fdroid");
        fdroidApks.close();

        Cursor exampleApks = contentResolver.query(
                ApkProvider.getAppUri("com.example"),
                PROJ,
                null, null, null);
        assertResultCount(10, exampleApks);
        assertBelongsToApp(exampleApks, "com.example");
        exampleApks.close();
    }

    @Test
    public void testInvalidDeleteUris() {
        Apk apk = new MockApk("org.fdroid.fdroid", 10);

        assertCantDelete(contentResolver, ApkProvider.getContentUri());
        assertCantDelete(contentResolver, ApkProvider.getApkFromAnyRepoUri("org.fdroid.fdroid", 10, null));
        assertCantDelete(contentResolver, ApkProvider.getApkFromAnyRepoUri(apk));
        assertCantDelete(contentResolver, Uri.withAppendedPath(ApkProvider.getContentUri(), "some-random-path"));
    }

    private static final long REPO_KEEP = 1;
    private static final long REPO_DELETE = 2;

    @Test
    public void testRepoApks() {

        // Insert apks into two repos, one of which we will later purge the
        // the apks from.
        for (int i = 1; i <= 5; i++) {
            insertApkForRepo("org.fdroid.fdroid", i, REPO_KEEP);
            insertApkForRepo("com.example." + i, 1, REPO_DELETE);
        }
        for (int i = 6; i <= 10; i++) {
            insertApkForRepo("org.fdroid.fdroid", i, REPO_DELETE);
            insertApkForRepo("com.example." + i, 1, REPO_KEEP);
        }

        assertTotalApkCount(20);

        Cursor cursor = contentResolver.query(
                ApkProvider.getRepoUri(REPO_DELETE), PROJ, null, null, null);
        assertResultCount(10, cursor);
        assertBelongsToRepo(cursor, REPO_DELETE);
        cursor.close();

        int count = ApkProvider.Helper.deleteApksByRepo(context, new MockRepo(REPO_DELETE));
        assertEquals(10, count);

        assertTotalApkCount(10);
        cursor = contentResolver.query(
                ApkProvider.getRepoUri(REPO_DELETE), PROJ, null, null, null);
        assertResultCount(0, cursor);
        cursor.close();

        // The only remaining apks should be those from REPO_KEEP.
        assertBelongsToRepo(queryAllApks(), REPO_KEEP);
    }

    @Test
    public void testQuery() {
        Cursor cursor = queryAllApks();
        assertNotNull(cursor);
        cursor.close();
    }

    @Test
    public void testInsert() {

        // Start with an empty database...
        Cursor cursor = queryAllApks();
        assertNotNull(cursor);
        assertEquals(0, cursor.getCount());
        cursor.close();

        Apk apk = new MockApk("org.fdroid.fdroid", 13);

        // Insert a new record...
        Assert.insertApk(context, apk.packageName, apk.versionCode);
        cursor = queryAllApks();
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());

        // And now we should be able to recover these values from the apk
        // value object (because the queryAllApks() helper asks for VERSION_CODE and
        // PACKAGE_NAME.
        cursor.moveToFirst();
        Apk toCheck = new Apk(cursor);
        cursor.close();
        assertEquals("org.fdroid.fdroid", toCheck.packageName);
        assertEquals(13, toCheck.versionCode);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCursorMustMoveToFirst() {
        Assert.insertApk(context, "org.example.test", 12);
        Cursor cursor = queryAllApks();
        new Apk(cursor);
    }

    @Test
    public void testCount() {
        String[] projectionCount = new String[] {Cols._COUNT};

        for (int i = 0; i < 13; i++) {
            Assert.insertApk(context, "com.example", i);
        }

        Uri all = ApkProvider.getContentUri();
        Cursor allWithFields = contentResolver.query(all, PROJ, null, null, null);
        Cursor allWithCount = contentResolver.query(all, projectionCount, null, null, null);

        assertResultCount(13, allWithFields);
        allWithFields.close();
        assertResultCount(1, allWithCount);

        allWithCount.moveToFirst();
        int countColumn = allWithCount.getColumnIndex(Cols._COUNT);
        assertEquals(13, allWithCount.getInt(countColumn));
        allWithCount.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertWithInvalidExtraFieldDescription() {
        assertInvalidExtraField(RepoTable.Cols.DESCRIPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertWithInvalidExtraFieldAddress() {
        assertInvalidExtraField(RepoTable.Cols.ADDRESS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertWithInvalidExtraFieldFingerprint() {
        assertInvalidExtraField(RepoTable.Cols.FINGERPRINT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertWithInvalidExtraFieldName() {
        assertInvalidExtraField(RepoTable.Cols.NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertWithInvalidExtraFieldSigningCert() {
        assertInvalidExtraField(RepoTable.Cols.SIGNING_CERT);
    }

    public void assertInvalidExtraField(String field) {
        ContentValues invalidRepo = new ContentValues();
        invalidRepo.put(field, "Test data");
        Assert.insertApk(context, "org.fdroid.fdroid", 10, invalidRepo);
    }

    @Test
    public void testInsertWithValidExtraFields() {

        assertResultCount(0, queryAllApks());

        ContentValues values = new ContentValues();
        values.put(Cols.REPO_ID, 10);
        values.put(Cols.Repo.ADDRESS, "http://example.com");
        values.put(Cols.Repo.VERSION, 3);
        values.put(Cols.FEATURES, "Some features");
        Uri uri = Assert.insertApk(context, "com.example.com", 1, values);

        assertResultCount(1, queryAllApks());

        String[] projections = Cols.ALL;
        Cursor cursor = contentResolver.query(uri, projections, null, null, null);
        cursor.moveToFirst();
        Apk apk = new Apk(cursor);
        cursor.close();

        // These should have quietly been dropped when we tried to save them,
        // because the provider only knows how to query them (not update them).
        assertEquals(null, apk.repoAddress);
        assertEquals(0, apk.repoVersion);

        // But this should have saved correctly...
        assertEquals(1, apk.features.length);
        assertEquals("Some features", apk.features[0]);
        assertEquals("com.example.com", apk.packageName);
        assertEquals(1, apk.versionCode);
        assertEquals(10, apk.repoId);
    }

    @Test
    public void testFindByApp() {

        for (int i = 0; i < 7; i++) {
            Assert.insertApk(context, "org.fdroid.fdroid", i);
        }

        for (int i = 0; i < 9; i++) {
            Assert.insertApk(context, "org.example", i);
        }

        for (int i = 0; i < 3; i++) {
            Assert.insertApk(context, "com.example", i);
        }

        Assert.insertApk(context, "com.apk.thingo", 1);

        assertTotalApkCount(7 + 9 + 3 + 1);

        List<Apk> fdroidApks = ApkProvider.Helper.findByPackageName(context, "org.fdroid.fdroid");
        assertResultCount(7, fdroidApks);
        assertBelongsToApp(fdroidApks, "org.fdroid.fdroid");

        List<Apk> exampleApks = ApkProvider.Helper.findByPackageName(context, "org.example");
        assertResultCount(9, exampleApks);
        assertBelongsToApp(exampleApks, "org.example");

        List<Apk> exampleApks2 = ApkProvider.Helper.findByPackageName(context, "com.example");
        assertResultCount(3, exampleApks2);
        assertBelongsToApp(exampleApks2, "com.example");

        List<Apk> thingoApks = ApkProvider.Helper.findByPackageName(context, "com.apk.thingo");
        assertResultCount(1, thingoApks);
        assertBelongsToApp(thingoApks, "com.apk.thingo");
    }

    @Test
    public void findApksForAppInSpecificRepo() {
        Repo fdroidRepo = RepoProvider.Helper.findByAddress(context, "https://f-droid.org/repo");
        Repo swapRepo = RepoProviderTest.insertRepo(context, "http://192.168.1.3/fdroid/repo", "", "22", "", true);

        App officialFDroid = insertApp(context, "org.fdroid.fdroid", "F-Droid (Official)", fdroidRepo);
        TestUtils.insertApk(context, officialFDroid, 4, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, officialFDroid, 5, TestUtils.FDROID_SIG);

        App debugSwapFDroid = insertApp(context, "org.fdroid.fdroid", "F-Droid (Debug)", swapRepo);
        TestUtils.insertApk(context, debugSwapFDroid, 6, TestUtils.THIRD_PARTY_SIG);

        List<Apk> foundOfficialApks = ApkProvider.Helper.findAppVersionsByRepo(context, officialFDroid, fdroidRepo);
        assertEquals(2, foundOfficialApks.size());

        List<Apk> debugSwapApks = ApkProvider.Helper.findAppVersionsByRepo(context, officialFDroid, swapRepo);
        assertEquals(1, debugSwapApks.size());
        assertEquals(debugSwapFDroid.getId(), debugSwapApks.get(0).appId);
        assertEquals(6, debugSwapApks.get(0).versionCode);
    }

    @Test
    public void testUpdate() {

        Uri apkUri = Assert.insertApk(context, "com.example", 10);

        String[] allFields = Cols.ALL;
        Cursor cursor = contentResolver.query(apkUri, allFields, null, null, null);
        assertResultCount(1, cursor);

        cursor.moveToFirst();
        Apk apk = new Apk(cursor);
        cursor.close();

        assertEquals("com.example", apk.packageName);
        assertEquals(10, apk.versionCode);

        assertNull(apk.antiFeatures);
        assertNull(apk.features);
        assertNull(apk.added);
        assertNull(apk.hashType);

        apk.antiFeatures = new String[] {"KnownVuln", "Other anti feature"};
        apk.features = new String[] {"one", "two", "three" };
        long dateTimestamp = System.currentTimeMillis();
        apk.added = new Date(dateTimestamp);
        apk.hashType = "i'm a hash type";

        ApkProvider.Helper.update(context, apk);

        // Should not have inserted anything else, just updated the already existing apk.
        Cursor allCursor = contentResolver.query(ApkProvider.getContentUri(), allFields, null, null, null);
        assertResultCount(1, allCursor);
        allCursor.close();

        Cursor updatedCursor = contentResolver.query(apkUri, allFields, null, null, null);
        assertResultCount(1, updatedCursor);

        updatedCursor.moveToFirst();
        Apk updatedApk = new Apk(updatedCursor);
        updatedCursor.close();

        assertEquals("com.example", updatedApk.packageName);
        assertEquals(10, updatedApk.versionCode);

        assertArrayEquals(new String[]{"KnownVuln", "Other anti feature"}, updatedApk.antiFeatures);
        assertArrayEquals(new String[]{"one", "two", "three"}, updatedApk.features);
        assertEquals(new Date(dateTimestamp).getYear(), updatedApk.added.getYear());
        assertEquals(new Date(dateTimestamp).getMonth(), updatedApk.added.getMonth());
        assertEquals(new Date(dateTimestamp).getDay(), updatedApk.added.getDay());
        assertEquals("i'm a hash type", updatedApk.hashType);
    }

    @Test
    public void testFind() {
        // Insert some random apks either side of the "com.example", so that
        // the Helper.find() method doesn't stumble upon the app we are interested
        // in by shear dumb luck...
        for (int i = 0; i < 10; i++) {
            Assert.insertApk(context, "org.fdroid.apk." + i, i);
        }

        App exampleApp = insertApp(context, "com.example", "Example");

        ContentValues values = new ContentValues();
        values.put(Cols.VERSION_NAME, "v1.1");
        values.put(Cols.HASH, "xxxxyyyy");
        values.put(Cols.HASH_TYPE, "a hash type");
        Assert.insertApk(context, exampleApp, 11, values);

        // ...and a few more for good measure...
        for (int i = 15; i < 20; i++) {
            Assert.insertApk(context, "com.other.thing." + i, i);
        }

        Apk apk = ApkProvider.Helper.findApkFromAnyRepo(context, "com.example", 11);

        assertNotNull(apk);

        // The find() method populates ALL fields if you don't specify any,
        // so we expect to find each of the ones we inserted above...
        assertEquals("com.example", apk.packageName);
        assertEquals(11, apk.versionCode);
        assertEquals("v1.1", apk.versionName);
        assertEquals("xxxxyyyy", apk.hash);
        assertEquals("a hash type", apk.hashType);

        String[] projection = {
            Cols.Package.PACKAGE_NAME,
            Cols.HASH,
        };

        Apk apkLessFields = ApkProvider.Helper.findApkFromAnyRepo(context, "com.example", 11, null, projection);

        assertNotNull(apkLessFields);

        assertEquals("com.example", apkLessFields.packageName);
        assertEquals("xxxxyyyy", apkLessFields.hash);

        // Didn't ask for these fields, so should be their default values...
        assertNull(apkLessFields.hashType);
        assertNull(apkLessFields.versionName);
        assertEquals(0, apkLessFields.versionCode);

        Apk notFound = ApkProvider.Helper.findApkFromAnyRepo(context, "com.doesnt.exist", 1000);
        assertNull(notFound);
    }

    protected final Cursor queryAllApks() {
        return contentResolver.query(ApkProvider.getContentUri(), PROJ, null, null, null);
    }

    protected void assertContains(List<Apk> apks, Apk apk) {
        boolean found = false;
        for (Apk a : apks) {
            if (a.versionCode == apk.versionCode && a.packageName.equals(apk.packageName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            fail("Apk [" + apk + "] not found in " + Assert.listToString(apks));
        }
    }

    protected void assertBelongsToApp(Cursor apks, String appId) {
        assertBelongsToApp(ApkProvider.Helper.cursorToList(apks), appId);
    }

    protected void assertBelongsToApp(List<Apk> apks, String appId) {
        for (Apk apk : apks) {
            assertEquals(appId, apk.packageName);
        }
    }

    protected void assertTotalApkCount(int expected) {
        assertResultCount(expected, queryAllApks());
    }

    protected void assertBelongsToRepo(Cursor apkCursor, long repoId) {
        for (Apk apk : ApkProvider.Helper.cursorToList(apkCursor)) {
            assertEquals(repoId, apk.repoId);
        }
    }

    protected Apk insertApkForRepo(String id, int versionCode, long repoId) {
        ContentValues additionalValues = new ContentValues();
        additionalValues.put(Cols.REPO_ID, repoId);
        Uri uri = Assert.insertApk(context, id, versionCode, additionalValues);
        return ApkProvider.Helper.get(context, uri);
    }
}
