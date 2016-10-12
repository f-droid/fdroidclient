package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.fdroid.fdroid.data.Schema.CategoryTable.Cols;

public class CategoryProvider extends FDroidProvider {

    public static final class Helper {
        private Helper() { }

        public static long ensureExists(Context context, String category) {
            long id = getCategoryId(context, category);
            if (id <= 0) {
                ContentValues values = new ContentValues(1);
                values.put(Cols.NAME, category);
                Uri uri = context.getContentResolver().insert(getContentUri(), values);
                id = Long.parseLong(uri.getLastPathSegment());
            }
            return id;
        }

        public static long getCategoryId(Context context, String category) {
            String[] projection = new String[] {Cols.ROW_ID};
            Cursor cursor = context.getContentResolver().query(getCategoryUri(category), projection, null, null, null);
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
            return Schema.CategoryTable.NAME;
        }

        @Override
        public void addField(String field) {
            appendField(field, getTableName());
        }
    }

    private static final String PROVIDER_NAME = "CategoryProvider";

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    private static final String PATH_CATEGORY_NAME = "categoryName";
    private static final String PATH_CATEGORY_ID = "categoryId";

    static {
        MATCHER.addURI(getAuthority(), PATH_CATEGORY_NAME + "/*", CODE_SINGLE);
    }

    private static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getCategoryUri(String categoryName) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_CATEGORY_NAME)
                .appendPath(categoryName)
                .build();
    }

    /**
     * Not actually used as part of the external API to this content provider.
     * Rather, used as a mechanism for returning the ID of a newly inserted row after calling
     * {@link android.content.ContentProvider#insert(Uri, ContentValues)}, as that is only able
     * to return a {@link Uri}. The {@link Uri#getLastPathSegment()} of this URI contains a
     * {@link Long} which is the {@link Cols#ROW_ID} of the newly inserted row.
     */
    private static Uri getCategoryIdUri(long categoryId) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_CATEGORY_ID)
                .appendPath(Long.toString(categoryId))
                .build();
    }

    @Override
    protected String getTableName() {
        return Schema.CategoryTable.NAME;
    }

    @Override
    protected String getProviderName() {
        return "CategoryProvider";
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    @Override
    protected UriMatcher getMatcher() {
        return MATCHER;
    }

    protected QuerySelection querySingle(String categoryName) {
        final String selection = getTableName() + "." + Cols.NAME + " = ?";
        final String[] args = {categoryName};
        return new QuerySelection(selection, args);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
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
     * Deleting of categories is not required.
     * It doesn't matter if we have a category in the database when no apps are in that category.
     * They wont take up much space, and it is the presence of rows in the
     * {@link Schema.CatJoinTable} which decides whether a category is displayed in F-Droid or not.
     */
    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Delete not supported for " + uri + ".");
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long rowId = db().insertOrThrow(getTableName(), null, values);
        getContext().getContentResolver().notifyChange(AppProvider.getCanUpdateUri(), null);
        return getCategoryIdUri(rowId);
    }

    /**
     * Category names never change. If an app originally is in category "Games" and then in a
     * future repo update is now in "Games & Stuff", then both categories can exist quite happily.
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Update not supported for " + uri + ".");
    }
}
