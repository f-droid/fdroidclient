package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.CatJoinTable;
import org.fdroid.fdroid.data.Schema.CategoryTable;
import org.fdroid.fdroid.data.Schema.CategoryTable.Cols;
import org.fdroid.fdroid.data.Schema.PackageTable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CategoryProvider extends FDroidProvider {

    public static final class Helper {
        private Helper() {
        }

        /**
         * During repo updates, each app needs to know the ID of each category it belongs to.
         * This results in lots of database lookups, usually at least one for each app, sometimes more.
         * To improve performance, this caches the association between categories and their database IDs.
         *
         * It can stay around for the entire F-Droid process, even across multiple repo updates, as we
         * don't actually remove data from the categories table.
         */
        private static final Map<String, Long> KNOWN_CATEGORIES = new HashMap<>();

        /**
         * Used by tests to clear that the "Category -> ID" cache (used to prevent excessive disk reads).
         */
        static void clearCategoryIdCache() {
            KNOWN_CATEGORIES.clear();
        }

        public static long ensureExists(Context context, String category) {
            // Check our in-memory cache to potentially prevent a trip to the database (and hence disk).
            String lowerCaseCategory = category.toLowerCase(Locale.ENGLISH);
            if (KNOWN_CATEGORIES.containsKey(lowerCaseCategory)) {
                return KNOWN_CATEGORIES.get(lowerCaseCategory);
            }

            long id = getCategoryId(context, category);
            if (id <= 0) {
                ContentValues values = new ContentValues(1);
                values.put(Cols.NAME, category);
                Uri uri = context.getContentResolver().insert(getContentUri(), values);
                id = Long.parseLong(uri.getLastPathSegment());
            }

            KNOWN_CATEGORIES.put(lowerCaseCategory, id);

            return id;
        }

        private static long getCategoryId(Context context, String category) {
            String[] projection = new String[]{Cols.ROW_ID};
            Cursor cursor = context.getContentResolver().query(getCategoryUri(category), projection,
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
            return CategoryTable.NAME + " LEFT JOIN " + CatJoinTable.NAME + " ON (" +
                    CatJoinTable.Cols.CATEGORY_ID + " = " + CategoryTable.NAME + "." + Cols.ROW_ID + ") ";
        }

        @Override
        public void addField(String field) {
            appendField(field, getTableName());
        }

        @Override
        protected String groupBy() {
            return CategoryTable.NAME + "." + Cols.ROW_ID;
        }

        public void setOnlyCategoriesWithApps() {
            // Make sure that metadata from the preferred repository is used to determine if
            // there is an app present or not.
            join(AppMetadataTable.NAME, "app", "app." + AppMetadataTable.Cols.ROW_ID
                    + " = " + CatJoinTable.NAME + "." + CatJoinTable.Cols.APP_METADATA_ID);
            join(PackageTable.NAME, "pkg", "pkg." + PackageTable.Cols.PREFERRED_METADATA
                    + " = " + "app." + AppMetadataTable.Cols.ROW_ID);
        }
    }

    private static final String PROVIDER_NAME = "CategoryProvider";

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    private static final String PATH_CATEGORY_NAME = "categoryName";
    private static final String PATH_ALL_CATEGORIES = "all";
    private static final String PATH_CATEGORY_ID = "categoryId";

    static {
        MATCHER.addURI(getAuthority(), PATH_CATEGORY_NAME + "/*", CODE_SINGLE);
        MATCHER.addURI(getAuthority(), PATH_ALL_CATEGORIES, CODE_LIST);
    }

    static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getAllCategories() {
        return Uri.withAppendedPath(getContentUri(), PATH_ALL_CATEGORIES);
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
        return CategoryTable.NAME;
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
        final String selection = getTableName() + "." + Cols.NAME + " = ? COLLATE NOCASE";
        final String[] args = {categoryName};
        return new QuerySelection(selection, args);
    }

    protected QuerySelection queryAllInUse() {
        final String selection = CatJoinTable.NAME + "." + CatJoinTable.Cols.APP_METADATA_ID + " IS NOT NULL ";
        final String[] args = {};
        return new QuerySelection(selection, args);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection,
                        String customSelection, String[] selectionArgs, String sortOrder) {
        QuerySelection selection = new QuerySelection(customSelection, selectionArgs);
        boolean onlyCategoriesWithApps = false;
        switch (MATCHER.match(uri)) {
            case CODE_SINGLE:
                selection = selection.add(querySingle(uri.getLastPathSegment()));
                break;

            case CODE_LIST:
                selection = selection.add(queryAllInUse());
                onlyCategoriesWithApps = true;
                break;

            default:
                throw new UnsupportedOperationException("Invalid URI for content provider: " + uri);
        }

        Query query = new Query();
        query.addSelection(selection);
        query.addFields(projection);
        query.addOrderBy(sortOrder);

        if (onlyCategoriesWithApps) {
            query.setOnlyCategoriesWithApps();
        }

        Cursor cursor = LoggingQuery.query(db(), query.toString(), query.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Deleting of categories is not required.
     * It doesn't matter if we have a category in the database when no apps are in that category.
     * They wont take up much space, and it is the presence of rows in the
     * {@link CatJoinTable} which decides whether a category is displayed in F-Droid or not.
     */
    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Delete not supported for " + uri + ".");
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long rowId = db().insertOrThrow(getTableName(), null, values);
        // Don't try and notify listeners here, because it will instead happen when the TempAppProvider
        // is committed (when the AppProvider and ApkProviders notify their listeners). There is no
        // other time where categories get added (at time of writing) so this should be okay.
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
