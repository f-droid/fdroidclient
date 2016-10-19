package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;

import java.util.List;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.fdroid.fdroid.data.Schema.CatJoinTable;
import org.fdroid.fdroid.data.Schema.PackageTable;

/**
 * This class does all of its operations in a temporary sqlite table.
 */
public class TempAppProvider extends AppProvider {

    /**
     * The name of the in memory database used for updating.
     */
    static final String DB = "temp_update_db";

    private static final String PROVIDER_NAME = "TempAppProvider";

    static final String TABLE_TEMP_APP = "temp_" + AppMetadataTable.NAME;
    static final String TABLE_TEMP_CAT_JOIN = "temp_" + CatJoinTable.NAME;

    private static final String PATH_INIT = "init";
    private static final String PATH_COMMIT = "commit";

    private static final int CODE_INIT = 10000;
    private static final int CODE_COMMIT = CODE_INIT + 1;
    private static final int APPS = CODE_COMMIT + 1;

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    static {
        MATCHER.addURI(getAuthority(), PATH_INIT, CODE_INIT);
        MATCHER.addURI(getAuthority(), PATH_COMMIT, CODE_COMMIT);
        MATCHER.addURI(getAuthority(), PATH_APPS + "/#/*", APPS);
        MATCHER.addURI(getAuthority(), PATH_SPECIFIC_APP + "/#/*", CODE_SINGLE);
    }

    @Override
    protected String getTableName() {
        return TABLE_TEMP_APP;
    }

    @Override
    protected String getCatJoinTableName() {
        return TABLE_TEMP_CAT_JOIN;
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    /**
     * Same as {@link AppProvider#getSpecificAppUri(String, long)}, except loads data from the temp
     * table being used during a repo update rather than the persistent table.
     */
    public static Uri getSpecificTempAppUri(String packageName, long repoId) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_SPECIFIC_APP)
                .appendPath(Long.toString(repoId))
                .appendPath(packageName)
                .build();
    }

    public static Uri getAppsUri(List<String> apps, long repoId) {
        return getContentUri().buildUpon()
                .appendPath(PATH_APPS)
                .appendPath(Long.toString(repoId))
                .appendPath(TextUtils.join(",", apps))
                .build();
    }

    private AppQuerySelection queryRepoApps(long repoId, String packageNames) {
        return queryPackageNames(packageNames, PackageTable.NAME + "." + PackageTable.Cols.PACKAGE_NAME)
                .add(queryRepo(repoId));
    }

    private AppQuerySelection queryRepo(long repoId) {
        String[] args = new String[] {Long.toString(repoId)};
        String selection = getTableName() + "." + Cols.REPO_ID + " = ? ";
        return new AppQuerySelection(selection, args);
    }

    public static class Helper {

        /**
         * Deletes the old temporary table (if it exists). Then creates a new temporary apk provider
         * table and populates it with all the data from the real apk provider table.
         */
        public static void init(Context context) {
            Uri uri = Uri.withAppendedPath(getContentUri(), PATH_INIT);
            context.getContentResolver().insert(uri, new ContentValues());
            TempApkProvider.Helper.init(context);
        }

        public static List<App> findByPackageNames(Context context, List<String> packageNames, long repoId, String[] projection) {
            Uri uri = getAppsUri(packageNames, repoId);
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            return AppProvider.Helper.cursorToList(cursor);
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
        switch (MATCHER.match(uri)) {
            case CODE_INIT:
                initTable();
                return null;
            case CODE_COMMIT:
                updateAllAppDetails();
                commitTable();
                return null;
            default:
                return super.insert(uri, values);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if (MATCHER.match(uri) != CODE_SINGLE) {
            throw new UnsupportedOperationException("Update not supported for " + uri + ".");
        }

        List<String> pathParts = uri.getPathSegments();
        String packageName = pathParts.get(2);
        long repoId = Long.parseLong(pathParts.get(1));
        QuerySelection query = new QuerySelection(where, whereArgs).add(querySingleForUpdate(packageName, repoId));

        // Package names for apps cannot change...
        values.remove(Cols.Package.PACKAGE_NAME);

        if (values.containsKey(Cols.ForWriting.Categories.CATEGORIES)) {
            String[] categories = Utils.parseCommaSeparatedString(values.getAsString(Cols.ForWriting.Categories.CATEGORIES));
            ensureCategories(categories, packageName, repoId);
            values.remove(Cols.ForWriting.Categories.CATEGORIES);
        }

        int count = db().update(getTableName(), values, query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(getHighestPriorityMetadataUri(packageName), null);
        }
        return count;
    }

    private void ensureCategories(String[] categories, String packageName, long repoId) {
        Query query = new AppProvider.Query();
        query.addField(Cols.ROW_ID);
        query.addSelection(querySingle(packageName, repoId));
        Cursor cursor = db().rawQuery(query.toString(), query.getArgs());
        cursor.moveToFirst();
        long appMetadataId = cursor.getLong(0);
        cursor.close();

        ensureCategories(categories, appMetadataId);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        AppQuerySelection selection = new AppQuerySelection(customSelection, selectionArgs);
        switch (MATCHER.match(uri)) {
            case APPS:
                List<String> segments = uri.getPathSegments();
                selection = selection.add(queryRepoApps(Long.parseLong(segments.get(1)), segments.get(2)));
                break;
        }

        return super.runQuery(uri, selection, projection, true, sortOrder, 0);
    }

    private void ensureTempTableDetached(SQLiteDatabase db) {
        try {
            db.execSQL("DETACH DATABASE " + DB);
        } catch (SQLiteException e) {
            // We expect that most of the time the database will not exist unless an error occurred
            // midway through the last update, The resulting exception is:
            //   android.database.sqlite.SQLiteException: no such database: temp_update_db (code 1)
        }
    }

    private void initTable() {
        final SQLiteDatabase db = db();
        ensureTempTableDetached(db);
        db.execSQL("ATTACH DATABASE ':memory:' AS " + DB);
        db.execSQL(DBHelper.CREATE_TABLE_APP_METADATA.replaceFirst(AppMetadataTable.NAME, DB + "." + getTableName()));
        db.execSQL(DBHelper.CREATE_TABLE_CAT_JOIN.replaceFirst(CatJoinTable.NAME, DB + "." + getCatJoinTableName()));
        db.execSQL(copyData(AppMetadataTable.Cols.ALL_COLS, AppMetadataTable.NAME, DB + "." + getTableName()));
        db.execSQL(copyData(CatJoinTable.Cols.ALL_COLS, CatJoinTable.NAME, DB + "." + getCatJoinTableName()));
        db.execSQL("CREATE INDEX IF NOT EXISTS " + DB + ".app_id ON " + getTableName() + " (" + AppMetadataTable.Cols.PACKAGE_ID + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS " + DB + ".app_upstreamVercode ON " + getTableName() + " (" + AppMetadataTable.Cols.UPSTREAM_VERSION_CODE + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS " + DB + ".app_compatible ON " + getTableName() + " (" + AppMetadataTable.Cols.IS_COMPATIBLE + ");");
    }

    /**
     * Constructs an INSERT INTO ... SELECT statement as a means from getting data from one table
     * into another. The list of columns to copy are explicitly specified using colsToCopy.
     */
    static String copyData(String[] colsToCopy, String fromTable, String toTable) {
        String cols = TextUtils.join(", ", colsToCopy);
        return "INSERT INTO " + toTable + " (" + cols + ") SELECT " + cols + " FROM " + fromTable;
    }

    private void commitTable() {
        final SQLiteDatabase db = db();
        try {
            db.beginTransaction();

            final String tempApp = DB + "." + TABLE_TEMP_APP;
            final String tempApk = DB + "." + TempApkProvider.TABLE_TEMP_APK;
            final String tempCatJoin = DB + "." + TABLE_TEMP_CAT_JOIN;

            db.execSQL("DELETE FROM " + AppMetadataTable.NAME + " WHERE 1");
            db.execSQL(copyData(AppMetadataTable.Cols.ALL_COLS, tempApp, AppMetadataTable.NAME));

            db.execSQL("DELETE FROM " + ApkTable.NAME + " WHERE 1");
            db.execSQL(copyData(ApkTable.Cols.ALL_COLS, tempApk, ApkTable.NAME));

            db.execSQL("DELETE FROM " + CatJoinTable.NAME + " WHERE 1");
            db.execSQL(copyData(CatJoinTable.Cols.ALL_COLS, tempCatJoin, CatJoinTable.NAME));

            db.setTransactionSuccessful();

            getContext().getContentResolver().notifyChange(AppProvider.getContentUri(), null);
            getContext().getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
        } finally {
            db.endTransaction();
            db.execSQL("DETACH DATABASE " + DB); // Can't be done in a transaction.
        }
    }
}
