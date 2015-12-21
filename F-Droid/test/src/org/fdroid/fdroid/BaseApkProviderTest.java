package org.fdroid.fdroid;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;

import java.util.List;

/**
 * Provides helper methods that can be used by both Helper and plain old
 * Provider tests. Allows the test classes to contain only test methods,
 * hopefully making them easier to understand.
 *
 * This should not contain any test methods, or else they get executed
 * once for every concrete subclass.
 */
abstract class BaseApkProviderTest extends FDroidProviderTest<ApkProvider> {

    BaseApkProviderTest() {
        super(ApkProvider.class, ApkProvider.getAuthority());
    }

    @Override
    protected String[] getMinimalProjection() {
        return new String[] {
            ApkProvider.DataColumns.PACKAGE_NAME,
            ApkProvider.DataColumns.VERSION_CODE,
            ApkProvider.DataColumns.NAME,
            ApkProvider.DataColumns.REPO_ID,
        };
    }

    protected final Cursor queryAllApks() {
        return getMockContentResolver().query(ApkProvider.getContentUri(), getMinimalProjection(), null, null, null);
    }

    protected void assertContains(List<Apk> apks, Apk apk) {
        boolean found = false;
        for (Apk a : apks) {
            if (a.vercode == apk.vercode && a.packageName.equals(apk.packageName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            fail("Apk [" + apk + "] not found in " + TestUtils.listToString(apks));
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
            assertEquals(repoId, apk.repo);
        }
    }

    protected Apk insertApkForRepo(String id, int versionCode, long repoId) {
        ContentValues additionalValues = new ContentValues();
        additionalValues.put(ApkProvider.DataColumns.REPO_ID, repoId);
        Uri uri = TestUtils.insertApk(this, id, versionCode, additionalValues);
        return ApkProvider.Helper.get(getSwappableContext(), uri);
    }
}
