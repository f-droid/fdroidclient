package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.util.Log;

/**
 * This class does all of its operations in a temporary sqlite table.
 */
public class TempAppProvider extends AppProvider {

    private static final String TAG = "TempAppProvider";

    private static final String PROVIDER_NAME = "TempAppProvider";

    private static final String TABLE_TEMP_APP = "temp_fdroid_app";

    private static final String PATH_INIT = "init";
    private static final String PATH_COMMIT = "commit";

    private static final int CODE_INIT = 10000;
    private static final int CODE_COMMIT = CODE_INIT + 1;

    private static final UriMatcher matcher = new UriMatcher(-1);

    static {
        matcher.addURI(getAuthority(), PATH_INIT, CODE_INIT);
        matcher.addURI(getAuthority(), PATH_COMMIT, CODE_COMMIT);
        matcher.addURI(getAuthority(), "*", CODE_SINGLE);
    }

    @Override
    protected String getTableName() {
        return TABLE_TEMP_APP;
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getAppUri(App app) {
        return Uri.withAppendedPath(getContentUri(), app.id);
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
    protected String getApkTableName() {
        return TempApkProvider.TABLE_TEMP_APK;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (matcher.match(uri)) {
            case CODE_INIT:
                initTable();
                return null;
            case CODE_COMMIT:
                updateAppDetails();
                commitTable();
                return null;
            default:
                return super.insert(uri, values);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        QuerySelection query = new QuerySelection(where, whereArgs);
        switch (matcher.match(uri)) {
            case CODE_SINGLE:
                query = query.add(querySingle(uri.getLastPathSegment()));
                break;

            default:
                throw new UnsupportedOperationException("Update not supported for " + uri + ".");
        }

        int count = write().update(getTableName(), values, query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    private void initTable() {
        write().execSQL("DROP TABLE IF EXISTS " + getTableName());
        write().execSQL("CREATE TABLE " + getTableName() + " AS SELECT * FROM " + DBHelper.TABLE_APP);
    }

    private void commitTable() {
        Log.d(TAG, "Deleting all apks from " + DBHelper.TABLE_APP + " so they can be copied from " + getTableName());
        write().execSQL("DELETE FROM " + DBHelper.TABLE_APP);
        write().execSQL("INSERT INTO " + DBHelper.TABLE_APP + " SELECT * FROM " + getTableName());
        getContext().getContentResolver().notifyChange(AppProvider.getContentUri(), null);
    }
}
