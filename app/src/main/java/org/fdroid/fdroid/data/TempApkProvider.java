package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.ApkTable.Cols;

/**
 * This class does all of its operations in a temporary sqlite table.
 */
@SuppressWarnings("LineLength")
public class TempApkProvider extends ApkProvider {

    private static final String PROVIDER_NAME = "TempApkProvider";

    static final String TABLE_TEMP_APK = "temp_" + ApkTable.NAME;

    private static final String PATH_INIT = "init";

    private static final int CODE_INIT = 10000;

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    static {
        MATCHER.addURI(getAuthority(), PATH_INIT + "/#", CODE_INIT);
        MATCHER.addURI(getAuthority(), PATH_APK_FROM_ANY_REPO + "/#/*", CODE_APK_FROM_ANY_REPO);
        MATCHER.addURI(getAuthority(), PATH_APK_FROM_REPO + "/#/#", CODE_APK_FROM_REPO);
    }

    @Override
    protected String getTableName() {
        return TABLE_TEMP_APK;
    }

    @Override
    protected String getApkAntiFeatureJoinTableName() {
        return TempAppProvider.TABLE_TEMP_APK_ANTI_FEATURE_JOIN;
    }

    @Override
    protected String getAppTableName() {
        return TempAppProvider.TABLE_TEMP_APP;
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static class Helper {

        /**
         * Deletes the old temporary table (if it exists). Then creates a new temporary apk provider
         * table and populates it with all the data from the real apk provider table.
         *
         * This is package local because it must be invoked after
         * {@link org.fdroid.fdroid.data.TempAppProvider.Helper#init(Context, long)}. Due to this
         * dependence, that method invokes this one itself, rather than leaving it to the
         * {@link RepoPersister}.
         */
        static void init(Context context, long repoIdToUpdate) {
            Uri uri = getContentUri().buildUpon()
                    .appendPath(PATH_INIT)
                    .appendPath(Long.toString(repoIdToUpdate))
                    .build();
            context.getContentResolver().insert(uri, new ContentValues());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (MATCHER.match(uri) == CODE_INIT) {
            initTable(Long.parseLong(uri.getLastPathSegment()));
            return null;
        }

        return super.insert(uri, values);
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
    }

    private void initTable(long repoIdBeingUpdated) {
        final SQLiteDatabase db = db();
        final String memoryDbName = TempAppProvider.DB;
        db.execSQL(DBHelper.CREATE_TABLE_APK.replaceFirst(ApkTable.NAME, memoryDbName + "." + getTableName()));
        db.execSQL(DBHelper.CREATE_TABLE_APK_ANTI_FEATURE_JOIN.replaceFirst(Schema.ApkAntiFeatureJoinTable.NAME, memoryDbName + "." + getApkAntiFeatureJoinTableName()));

        String where = ApkTable.NAME + "." + Cols.REPO_ID + " != ?";
        String[] whereArgs = new String[]{Long.toString(repoIdBeingUpdated)};
        db.execSQL(TempAppProvider.copyData(Cols.ALL_COLS, ApkTable.NAME, memoryDbName + "." + getTableName(), where), whereArgs);

        String antiFeaturesWhere =
                Schema.ApkAntiFeatureJoinTable.NAME + "." + Schema.ApkAntiFeatureJoinTable.Cols.APK_ID + " IN " +
                "(SELECT innerApk." + Cols.ROW_ID + " FROM " + ApkTable.NAME + " AS innerApk " +
                "WHERE innerApk." + Cols.REPO_ID + " != ?)";

        db.execSQL(TempAppProvider.copyData(
                Schema.ApkAntiFeatureJoinTable.Cols.ALL_COLS,
                Schema.ApkAntiFeatureJoinTable.NAME,
                memoryDbName + "." + getApkAntiFeatureJoinTableName(),
                antiFeaturesWhere), whereArgs);

        db.execSQL("CREATE INDEX IF NOT EXISTS " + memoryDbName + ".apk_appId on " + getTableName() + " (" + Cols.APP_ID + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS " + memoryDbName + ".apk_compatible ON " + getTableName() + " (" + Cols.IS_COMPATIBLE + ");");
    }

}
