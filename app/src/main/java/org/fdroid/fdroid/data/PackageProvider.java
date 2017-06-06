package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import org.fdroid.fdroid.data.Schema.PackageTable;
import org.fdroid.fdroid.data.Schema.PackageTable.Cols;

public class PackageProvider extends FDroidProvider {

    public static final class Helper {
        private Helper() {
        }

        public static long ensureExists(Context context, String packageName) {
            long id = getPackageId(context, packageName);
            if (id <= 0) {
                ContentValues values = new ContentValues(1);
                values.put(Cols.PACKAGE_NAME, packageName);
                Uri uri = context.getContentResolver().insert(getContentUri(), values);
                id = Long.parseLong(uri.getLastPathSegment());
            }
            return id;
        }

        public static long getPackageId(Context context, String packageName) {
            String[] projection = new String[]{Cols.ROW_ID};
            Cursor cursor = context.getContentResolver().query(getPackageUri(packageName), projection,
                    null, null, null);
            if (cursor == null) {
                return 0;
            }

            try {
                if (cursor.getCount() == 0) {
                    return 0;
                } else {
                    cursor.moveToFirst();
                    return cursor.getLong(cursor.getColumnIndexOrThrow(Cols.ROW_ID));
                }
            } finally {
                cursor.close();
            }
        }
    }

    private class Query extends QueryBuilder {

        @Override
        protected String getRequiredTables() {
            return PackageTable.NAME;
        }

        @Override
        public void addField(String field) {
            appendField(field, getTableName());
        }
    }

    private static final String PROVIDER_NAME = "PackageProvider";

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    private static final String PATH_PACKAGE_NAME = "packageName";
    private static final String PATH_PACKAGE_ID = "packageId";

    static {
        MATCHER.addURI(getAuthority(), PATH_PACKAGE_NAME + "/*", CODE_SINGLE);
    }

    private static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getPackageUri(String packageName) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_PACKAGE_NAME)
                .appendPath(packageName)
                .build();
    }

    /**
     * Not actually used as part of the external API to this content provider.
     * Rather, used as a mechanism for returning the ID of a newly inserted row after calling
     * {@link android.content.ContentProvider#insert(Uri, ContentValues)}, as that is only able
     * to return a {@link Uri}. The {@link Uri#getLastPathSegment()} of this URI contains a
     * {@link Long} which is the {@link PackageTable.Cols#ROW_ID} of the newly inserted row.
     */
    private static Uri getPackageIdUri(long packageId) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_PACKAGE_ID)
                .appendPath(Long.toString(packageId))
                .build();
    }

    @Override
    protected String getTableName() {
        return PackageTable.NAME;
    }

    @Override
    protected String getProviderName() {
        return "PackageProvider";
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    @Override
    protected UriMatcher getMatcher() {
        return MATCHER;
    }

    protected QuerySelection querySingle(String packageName) {
        final String selection = getTableName() + "." + Cols.PACKAGE_NAME + " = ?";
        final String[] args = {packageName};
        return new QuerySelection(selection, args);
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
                        String customSelection, String[] selectionArgs, String sortOrder) {
        if (MATCHER.match(uri) != CODE_SINGLE) {
            throw new UnsupportedOperationException("Invalid URI for content provider: " + uri);
        }

        QuerySelection selection = new QuerySelection(customSelection, selectionArgs)
                .add(querySingle(uri.getLastPathSegment()));

        Query query = new Query();
        query.addSelection(selection);
        query.addFields(projection);
        query.addOrderBy(sortOrder);

        Cursor cursor = LoggingQuery.query(db(), query.toString(), query.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Deleting of packages is not required.
     * It doesn't matter if we have a package name in the database after the package is no longer
     * present in the repo any more. They wont take up much space, and it is the presence of rows
     * in the {@link Schema.AppMetadataTable} which decides whether something is available in the
     * F-Droid client or not.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Delete not supported for " + uri + ".");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowId = db().insertOrThrow(getTableName(), null, values);
        getContext().getContentResolver().notifyChange(AppProvider.getCanUpdateUri(), null);
        return getPackageIdUri(rowId);
    }

    /**
     * Package names never change. If a package name has changed, then that means that it is a
     * new app all together as far as Android is concerned.
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Update not supported for " + uri + ".");
    }
}
