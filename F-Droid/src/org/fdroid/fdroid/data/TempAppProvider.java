package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import org.fdroid.fdroid.Utils;

/**
 * This class does all of its operations in a temporary sqlite table.
 */
public class TempAppProvider extends AppProvider {

    private static final String TAG = "TempAppProvider";

    private static final String PROVIDER_NAME = "TempAppProvider";

    private static final String TABLE_TEMP_APP = "temp_" + DBHelper.TABLE_APP;

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
        return Uri.withAppendedPath(getContentUri(), app.packageName);
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
        public static void commitAppsAndApks(Context context) {
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
        write().execSQL("CREATE INDEX IF NOT EXISTS app_id ON " + getTableName() + " (id);");
        write().execSQL("CREATE INDEX IF NOT EXISTS app_upstreamVercode ON " + getTableName() + " (upstreamVercode);");
        write().execSQL("CREATE INDEX IF NOT EXISTS app_compatible ON " + getTableName() + " (compatible);");
    }

    private void commitTable() {
        final SQLiteDatabase db = write();
        try {
            db.beginTransaction();

            Log.i(TAG, "Renaming " + TABLE_TEMP_APP + " to " + DBHelper.TABLE_APP);
            db.execSQL("DROP TABLE " + DBHelper.TABLE_APP);
            db.execSQL("ALTER TABLE " + TABLE_TEMP_APP + " RENAME TO " + DBHelper.TABLE_APP);

            Log.i(TAG, "Renaming " + TempApkProvider.TABLE_TEMP_APK + " to " + DBHelper.TABLE_APK);
            db.execSQL("DROP TABLE " + DBHelper.TABLE_APK);
            db.execSQL("ALTER TABLE " + TempApkProvider.TABLE_TEMP_APK + " RENAME TO " + DBHelper.TABLE_APK);

            Utils.debugLog(TAG, "Successfully renamed both tables, will commit transaction");
            db.setTransactionSuccessful();

            getContext().getContentResolver().notifyChange(AppProvider.getContentUri(), null);
            getContext().getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
        } finally {
            db.endTransaction();
        }
    }
}
