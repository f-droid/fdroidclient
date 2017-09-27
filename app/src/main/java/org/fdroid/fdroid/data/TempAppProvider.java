package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.fdroid.fdroid.data.Schema.CatJoinTable;
import org.fdroid.fdroid.data.Schema.PackageTable;

import java.util.List;

/**
 * This class does all of its operations in a temporary sqlite table.
 */
@SuppressWarnings("LineLength")
public class TempAppProvider extends AppProvider {

    /**
     * The name of the in memory database used for updating.
     */
    static final String DB = "temp_update_db";

    private static final String PROVIDER_NAME = "TempAppProvider";

    static final String TABLE_TEMP_APP = "temp_" + AppMetadataTable.NAME;
    static final String TABLE_TEMP_APK_ANTI_FEATURE_JOIN = "temp_" + Schema.ApkAntiFeatureJoinTable.NAME;
    static final String TABLE_TEMP_CAT_JOIN = "temp_" + CatJoinTable.NAME;

    private static final String PATH_INIT = "init";
    private static final String PATH_COMMIT = "commit";

    private static final int CODE_INIT = 10000;
    private static final int CODE_COMMIT = CODE_INIT + 1;
    private static final int APPS = CODE_COMMIT + 1;

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    static {
        MATCHER.addURI(getAuthority(), PATH_INIT + "/#", CODE_INIT);
        MATCHER.addURI(getAuthority(), PATH_COMMIT + "/#", CODE_COMMIT);
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
        String[] args = new String[]{Long.toString(repoId)};
        String selection = getTableName() + "." + Cols.REPO_ID + " = ? ";
        return new AppQuerySelection(selection, args);
    }

    public static class Helper {

        /**
         * Deletes the old temporary table (if it exists). Then creates a new temporary apk provider
         * table and populates it with all the data from the real apk provider table.
         */
        public static void init(Context context, long repoIdToUpdate) {
            Uri uri = getContentUri().buildUpon()
                    .appendPath(PATH_INIT)
                    .appendPath(Long.toString(repoIdToUpdate))
                    .build();
            context.getContentResolver().insert(uri, new ContentValues());
            TempApkProvider.Helper.init(context, repoIdToUpdate);
        }

        public static List<App> findByPackageNames(Context context,
                                                   List<String> packageNames, long repoId, String[] projection) {
            Uri uri = getAppsUri(packageNames, repoId);
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            return AppProvider.Helper.cursorToList(cursor);
        }

        /**
         * Saves data from the temp table to the apk table, by removing _EVERYTHING_ from the real
         * apk table and inserting all of the records from here. The temporary table is then removed.
         */
        public static void commitAppsAndApks(Context context, long repoIdToCommit) {
            Uri uri = getContentUri().buildUpon()
                    .appendPath(PATH_COMMIT)
                    .appendPath(Long.toString(repoIdToCommit))
                    .build();
            context.getContentResolver().insert(uri, new ContentValues());
        }
    }

    @Override
    protected String getApkTableName() {
        return TempApkProvider.TABLE_TEMP_APK;
    }

    protected String getApkAntiFeatureJoinTableName() {
        return TempApkProvider.TABLE_TEMP_APK;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (MATCHER.match(uri)) {
            case CODE_INIT:
                initTable(Long.parseLong(uri.getLastPathSegment()));
                return null;
            case CODE_COMMIT:
                updateAllAppDetails();
                commitTable(Long.parseLong(uri.getLastPathSegment()));
                return null;
            default:
                return super.insert(uri, values);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Update not supported for " + uri + ".");
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
                        String customSelection, String[] selectionArgs, String sortOrder) {
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
            // Ideally we'd ask SQLite if the temp table is attached, but that is not possible.
            // Instead, we resort to hackery:
            // If the first statement does not throw an exception, then the temp db is attached and the second
            // statement will detach the database.
            db.rawQuery("SELECT * FROM " + DB + "." + getTableName() + " WHERE 0", null);
            db.execSQL("DETACH DATABASE " + DB);
        } catch (SQLiteException ignored) {

        }
    }

    private void initTable(long repoIdBeingUpdated) {
        final SQLiteDatabase db = db();

        String mainApp = AppMetadataTable.NAME;
        String tempApp = DB + "." + getTableName();
        String mainCat = CatJoinTable.NAME;
        String tempCat = DB + "." + getCatJoinTableName();

        ensureTempTableDetached(db);
        db.execSQL("ATTACH DATABASE ':memory:' AS " + DB);
        db.execSQL(DBHelper.CREATE_TABLE_APP_METADATA.replaceFirst(AppMetadataTable.NAME, tempApp));
        db.execSQL(DBHelper.CREATE_TABLE_CAT_JOIN.replaceFirst(CatJoinTable.NAME, tempCat));

        String appWhere = mainApp + "." + Cols.REPO_ID + " != ?";
        String[] repoArgs = new String[]{Long.toString(repoIdBeingUpdated)};
        db.execSQL(copyData(Cols.ALL_COLS, mainApp, tempApp, appWhere), repoArgs);

        // TODO: String catWhere = mainCat + "." + CatJoinTable.Cols..Cols.REPO_ID + " != ?";
        db.execSQL(copyData(CatJoinTable.Cols.ALL_COLS, mainCat, tempCat, null));

        db.execSQL("CREATE INDEX IF NOT EXISTS " + DB + ".app_id ON " + getTableName() + " (" + Cols.PACKAGE_ID + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS " + DB + ".app_upstreamVercode ON " + getTableName() + " (" + Cols.UPSTREAM_VERSION_CODE + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS " + DB + ".app_compatible ON " + getTableName() + " (" + Cols.IS_COMPATIBLE + ");");
    }

    /**
     * Constructs an INSERT INTO ... SELECT statement as a means from getting data from one table
     * into another. The list of columns to copy are explicitly specified using colsToCopy.
     */
    static String copyData(String[] colsToCopy, String fromTable, String toTable, String where) {
        String cols = TextUtils.join(", ", colsToCopy);
        String sql = "INSERT INTO " + toTable + " (" + cols + ") SELECT " + cols + " FROM " + fromTable;
        if (!TextUtils.isEmpty(where)) {
            sql += " WHERE " + where;
        }
        return sql;
    }

    private void commitTable(long repoIdToCommit) {
        final SQLiteDatabase db = db();
        try {
            db.beginTransaction();

            final String tempApp = DB + "." + TABLE_TEMP_APP;
            final String tempApk = DB + "." + TempApkProvider.TABLE_TEMP_APK;
            final String tempCatJoin = DB + "." + TABLE_TEMP_CAT_JOIN;
            final String tempAntiFeatureJoin = DB + "." + TABLE_TEMP_APK_ANTI_FEATURE_JOIN;

            final String[] repoArgs = new String[]{Long.toString(repoIdToCommit)};

            db.execSQL("DELETE FROM " + AppMetadataTable.NAME + " WHERE " + Cols.REPO_ID + " = ?", repoArgs);
            db.execSQL(copyData(Cols.ALL_COLS, tempApp, AppMetadataTable.NAME, Cols.REPO_ID + " = ?"), repoArgs);

            db.execSQL("DELETE FROM " + ApkTable.NAME + " WHERE " + ApkTable.Cols.REPO_ID + " = ?", repoArgs);
            db.execSQL(copyData(ApkTable.Cols.ALL_COLS, tempApk, ApkTable.NAME, ApkTable.Cols.REPO_ID + " = ?"), repoArgs);

            db.execSQL("DELETE FROM " + CatJoinTable.NAME + " WHERE " + getCatRepoWhere(CatJoinTable.NAME), repoArgs);
            db.execSQL(copyData(CatJoinTable.Cols.ALL_COLS, tempCatJoin, CatJoinTable.NAME, getCatRepoWhere(tempCatJoin)), repoArgs);

            db.execSQL(
                    "DELETE FROM " + Schema.ApkAntiFeatureJoinTable.NAME + " " +
                    "WHERE " + getAntiFeatureRepoWhere(Schema.ApkAntiFeatureJoinTable.NAME), repoArgs);

            db.execSQL(copyData(
                    Schema.ApkAntiFeatureJoinTable.Cols.ALL_COLS,
                    tempAntiFeatureJoin,
                    Schema.ApkAntiFeatureJoinTable.NAME,
                    getAntiFeatureRepoWhere(tempAntiFeatureJoin)), repoArgs);

            db.setTransactionSuccessful();

            getContext().getContentResolver().notifyChange(AppProvider.getContentUri(), null);
            getContext().getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
            getContext().getContentResolver().notifyChange(CategoryProvider.getContentUri(), null);
        } finally {
            db.endTransaction();
            db.execSQL("DETACH DATABASE " + DB); // Can't be done in a transaction.
        }
    }

    private String getCatRepoWhere(String categoryTable) {
        String catRepoSubquery =
                "SELECT DISTINCT innerCatJoin." + CatJoinTable.Cols.ROW_ID + " " +
                "FROM " + categoryTable + " AS innerCatJoin " +
                "JOIN " + getTableName() + " AS app ON (app." + Cols.ROW_ID + " = innerCatJoin." + CatJoinTable.Cols.APP_METADATA_ID + ") " +
                "WHERE app." + Cols.REPO_ID + " = ?";

        return CatJoinTable.Cols.ROW_ID + " IN (" + catRepoSubquery + ")";
    }

    private String getAntiFeatureRepoWhere(String antiFeatureTable) {
        String subquery =
                "SELECT innerApk." + ApkTable.Cols.ROW_ID + " " +
                "FROM " + ApkTable.NAME + " AS innerApk " +
                "WHERE innerApk." + ApkTable.Cols.REPO_ID + " = ?";

        return antiFeatureTable + "." + Schema.ApkAntiFeatureJoinTable.Cols.APK_ID + " IN (" + subquery + ")";
    }
}
