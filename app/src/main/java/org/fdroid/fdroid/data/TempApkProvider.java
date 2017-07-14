package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.ApkTable.Cols;

import java.util.List;

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
        MATCHER.addURI(getAuthority(), PATH_REPO_APK + "/#/*", CODE_REPO_APK);
    }

    @Override
    protected String getTableName() {
        return TABLE_TEMP_APK;
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

    public static Uri getApkUri(Apk apk) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_APK_FROM_REPO)
                .appendPath(Long.toString(apk.appId))
                .appendPath(Integer.toString(apk.versionCode))
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
        if (MATCHER.match(uri) != CODE_APK_FROM_REPO) {
            throw new UnsupportedOperationException("Cannot update anything other than a single apk.");
        }

        return performUpdateUnchecked(uri, values, where, whereArgs);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if (MATCHER.match(uri) != CODE_REPO_APK) {
            throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
        }

        List<String> pathSegments = uri.getPathSegments();
        QuerySelection query = new QuerySelection(where, whereArgs)
                .add(queryRepo(Long.parseLong(pathSegments.get(1)), false))
                .add(queryApks(pathSegments.get(2), false));

        int rowsAffected = db().delete(getTableName(), query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsAffected;

    }

    private void initTable(long repoIdBeingUpdated) {
        final SQLiteDatabase db = db();
        final String memoryDbName = TempAppProvider.DB;
        db.execSQL(DBHelper.CREATE_TABLE_APK.replaceFirst(ApkTable.NAME, memoryDbName + "." + getTableName()));

        String where = ApkTable.NAME + "." + Cols.REPO_ID + " != ?";
        String[] whereArgs = new String[]{Long.toString(repoIdBeingUpdated)};
        db.execSQL(TempAppProvider.copyData(Cols.ALL_COLS, ApkTable.NAME, memoryDbName + "." + getTableName(), where), whereArgs);

        db.execSQL("CREATE INDEX IF NOT EXISTS " + memoryDbName + ".apk_appId on " + getTableName() + " (" + Cols.APP_ID + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS " + memoryDbName + ".apk_compatible ON " + getTableName() + " (" + Cols.IS_COMPATIBLE + ");");
    }

}
