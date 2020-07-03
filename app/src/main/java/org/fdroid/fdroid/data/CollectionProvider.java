package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.fdroid.fdroid.data.Schema.CollectionTable;

public class CollectionProvider extends FDroidProvider {

    private static final UriMatcher MATCHER = new UriMatcher(-1);
    private static final String TAG = "CollectionProvider";

    private static final String PROVIDER_NAME = "CollectionProvider";
    private static final String PATH_COLLECTION_HIDDEN = "collectionHidden";
    private static final String PATH_COLLECTION = "collection";
    private static final String PATH_JSON = "json";

    static final int CODE_SINGLE = 2;
    static final int COLLECTION_HIDDEN = CODE_SINGLE + 1;
    static final int COLLECTION = COLLECTION_HIDDEN + 1;
    static final int JSON = COLLECTION + 1;

    static {
        MATCHER.addURI(getAuthority(), PATH_COLLECTION_HIDDEN, COLLECTION_HIDDEN);
        MATCHER.addURI(getAuthority(), PATH_COLLECTION, COLLECTION);
        MATCHER.addURI(getAuthority(), PATH_JSON, JSON);

    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getCollectionHiddenUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_COLLECTION_HIDDEN);
    }

    public static Uri getCollectionUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_COLLECTION);
    }

    public static Uri getJSONUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_JSON);
    }

    @Override
    protected String getTableName() {
        return null;
    }

    @Override
    protected String getProviderName() {
        return null;
    }

    @Override
    protected UriMatcher getMatcher() {
        return null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {

        String mQuery;
        switch (MATCHER.match(uri)) {
            case JSON:
                mQuery =
                        "SELECT  " +
                                Schema.CollectionTable.NAME + "." + CollectionTable.Cols.NAME + " as " + Schema.AppMetadataTable.Cols.NAME + ", " +
                                select_alias() +
                                " FROM " + Schema.CollectionTable.NAME;
                break;
            case COLLECTION_HIDDEN:
                mQuery =
                        tmpQuery() +
                                " AND " + Schema.AppMetadataTable.Cols.Collection.HIDDEN + " = 0" +
                                " ORDER BY name ASC";

                break;
            case COLLECTION:
                mQuery =
                        tmpQuery() +
                                " ORDER BY name ASC";

                break;
            default:
                throw new IllegalStateException("Unexpected value: " + MATCHER.match(uri));
        }

        return db().rawQuery(mQuery, null);
    }

    private String tmpQuery() {
        return " SELECT case when " + Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.NAME + " is null then " + Schema.CollectionTable.NAME + "." + CollectionTable.Cols.NAME + " else " + Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.NAME + " end as " + Schema.AppMetadataTable.Cols.NAME + ", " +
                select_alias() + ", " +
                Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.SUMMARY + ", " +
                Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.ICON + ", " +
                Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.ICON_URL + ", " +
                Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.REPO_ID +
                " FROM " + Schema.CollectionTable.NAME +
                " LEFT JOIN " + Schema.PackageTable.NAME +
                " ON " + Schema.CollectionTable.NAME + "." + CollectionTable.Cols.PACKAGE_NAME + " = " + Schema.PackageTable.NAME + "." + Schema.PackageTable.Cols.PACKAGE_NAME +
                " LEFT JOIN " + Schema.AppMetadataTable.NAME +
                " ON " + Schema.AppMetadataTable.NAME + "." + Schema.AppMetadataTable.Cols.PACKAGE_ID + " = " + Schema.PackageTable.NAME + "." + Schema.PackageTable.Cols.ROW_ID +
                " LEFT JOIN " + Schema.InstalledAppTable.NAME + " AS installed" +
                " ON installed." + Schema.InstalledAppTable.Cols.PACKAGE_ID + " = " + Schema.PackageTable.NAME + "." + Schema.PackageTable.Cols.ROW_ID +
                " WHERE installed." + Schema.InstalledAppTable.Cols.PACKAGE_ID + " is null";
    }

    private String select_alias() {
        return Schema.CollectionTable.NAME + "." + CollectionTable.Cols.PACKAGE_NAME + " as " + Schema.AppMetadataTable.Cols.Package.PACKAGE_NAME + ", " +
                Schema.CollectionTable.NAME + "." + CollectionTable.Cols.LAST_MODIFIED + " as " + Schema.AppMetadataTable.Cols.Collection.LAST_MODIFIED + ", " +
                Schema.CollectionTable.NAME + "." + CollectionTable.Cols.HIDDEN + " as " + Schema.AppMetadataTable.Cols.Collection.HIDDEN + ", " +
                Schema.CollectionTable.NAME + "." + CollectionTable.Cols.VERSION_CODE + " as " + Schema.AppMetadataTable.Cols.SUGGESTED_VERSION_CODE + ", " +
                Schema.CollectionTable.NAME + "." + CollectionTable.Cols.VERSION_NAME + " as " + Schema.AppMetadataTable.Cols.SUGGESTED_VERSION_NAME;

    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        switch (MATCHER.match(uri)) {
            case JSON:
                db().insert(Schema.CollectionTable.NAME, null, values);
                break;
            default:
                break;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String whereString, @Nullable String[] whereArgsStrings) {
        switch (MATCHER.match(uri)) {
            case JSON:
                db().delete(Schema.CollectionTable.NAME, whereString, whereArgsStrings);
                break;
        }

        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String whereStrings, @Nullable String[] whereArgsStrings) {
        long id = 0;
        switch (MATCHER.match(uri)) {

            case JSON:
                id = db().update(Schema.CollectionTable.NAME, contentValues, whereStrings, whereArgsStrings);
                break;
            default:
                Log.e("Query", "Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return (int) id;
    }


    public static void CollectionSync(Context context) {

        Cursor cursor = context.getContentResolver().query(AppProvider.getInstalledUri(), Schema.AppMetadataTable.Cols.ALL, null, null, null);
        assert cursor != null;

        cursor.moveToPosition(-1);

        while (cursor.moveToNext()) {
            App app = new App(cursor);
            ContentValues values_import = new ContentValues();


            int ignoreThisUpdate = app.getPrefs(context).ignoreThisUpdate;
            if (app.name != null) {
                values_import.put(Schema.CollectionTable.Cols.NAME, app.name);
            }
            if (app.packageName != null) {
                values_import.put(Schema.CollectionTable.Cols.PACKAGE_NAME, app.packageName);
            }
            if (app.installedVersionCode > 0) {
                values_import.put(Schema.CollectionTable.Cols.VERSION_CODE, app.installedVersionCode);
            }
            if (app.installedVersionName != null) {
                values_import.put(Schema.CollectionTable.Cols.VERSION_NAME, app.installedVersionName);
            }
            if (ignoreThisUpdate > 0) {
                values_import.put(Schema.CollectionTable.Cols.IGNORING_VERSION_CODE, ignoreThisUpdate);
            }


            int ret = context.getContentResolver().update(getJSONUri(), values_import, Schema.CollectionTable.Cols.PACKAGE_NAME + "=?", new String[]{app.packageName});
            if (ret == 0) {
                context.getContentResolver().insert(getJSONUri(), values_import);
            }
        }
        cursor.close();

        Log.e(TAG, "Synced");
    }

}


