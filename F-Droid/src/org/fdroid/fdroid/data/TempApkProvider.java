package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.util.Log;

import java.util.List;

/**
 * This class does all of its operations in a temporary sqlite table.
 */
public class TempApkProvider extends ApkProvider {

    private static final String TAG = "TempApkProvider";

    private static final String PROVIDER_NAME = "TempApkProvider";

    static final String TABLE_TEMP_APK = "temp_fdroid_apk";

    private static final String PATH_INIT = "init";
    private static final String PATH_COMMIT = "commit";

    private static final int CODE_INIT = 10000;
    private static final int CODE_COMMIT = CODE_INIT + 1;

    private static final UriMatcher matcher = new UriMatcher(-1);

    static {
        matcher.addURI(getAuthority(), PATH_INIT, CODE_INIT);
        matcher.addURI(getAuthority(), PATH_COMMIT, CODE_COMMIT);
        matcher.addURI(getAuthority(), PATH_APK + "/#/*", CODE_SINGLE);
        matcher.addURI(getAuthority(), PATH_REPO_APK + "/#/*", CODE_REPO_APK);
    }

    @Override
    protected String getTableName() {
        return "temp_" + super.getTableName();
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getApkUri(Apk apk) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_APK)
                .appendPath(Integer.toString(apk.vercode))
                .appendPath(apk.id)
                .build();
    }

    public static Uri getApksUri(Repo repo, List<Apk> apks) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_REPO_APK)
                .appendPath(Long.toString(repo.id))
                .appendPath(buildApkString(apks))
                .build();
    }

    public static class Helper {

        /**
         * Deletes the old temporary table (if it exists). Then creates a new temporary apk provider
         * table and populates it with all the data from the real apk provider table.
         */
        public static void init(Context context) {
            Uri uri = Uri.withAppendedPath(getContentUri(), PATH_INIT);
            context.getContentResolver().insert(uri, new ContentValues());
        }

        /**
         * Saves data from the temp table to the apk table, by removing _EVERYTHING_ from the real
         * apk table and inserting all of the records from here. The temporary table is then removed.
         */
        public static void commit(Context context) {
            Uri uri = Uri.withAppendedPath(getContentUri(), PATH_COMMIT);
            context.getContentResolver().insert(uri, new ContentValues());
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (matcher.match(uri)) {
            case CODE_INIT:
                initTable();
                return null;
            case CODE_COMMIT:
                commitTable();
                return null;
            default:
                return super.insert(uri, values);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        if (matcher.match(uri) != CODE_SINGLE) {
            throw new UnsupportedOperationException("Cannot update anything other than a single apk.");
        }

        return performUpdateUnchecked(uri, values, where, whereArgs);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        QuerySelection query = new QuerySelection(where, whereArgs);

        switch (matcher.match(uri)) {
            case CODE_REPO_APK:
                List<String> pathSegments = uri.getPathSegments();
                query = query.add(queryRepo(Long.parseLong(pathSegments.get(1)))).add(queryApks(pathSegments.get(2)));
                break;

            default:
                Log.e(TAG, "Invalid URI for apk content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
        }

        int rowsAffected = write().delete(getTableName(), query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsAffected;

    }

    private void initTable() {
        write().execSQL("DROP TABLE IF EXISTS " + getTableName());
        write().execSQL("CREATE TABLE " + getTableName() + " AS SELECT * FROM " + DBHelper.TABLE_APK);
    }

    private void commitTable() {
        Log.i(TAG, "Deleting all apks from " + DBHelper.TABLE_APK + " so they can be copied from " + getTableName());
        write().execSQL("DELETE FROM " + DBHelper.TABLE_APK);
        write().execSQL("INSERT INTO " + DBHelper.TABLE_APK + " SELECT * FROM " + getTableName());
        getContext().getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
    }
}
