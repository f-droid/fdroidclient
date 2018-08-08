package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.fdroid.fdroid.data.Schema.AppPrefsTable;
import org.fdroid.fdroid.data.Schema.AppPrefsTable.Cols;

public class AppPrefsProvider extends FDroidProvider {

    public static final class Helper {
        private Helper() {
        }

        public static void update(Context context, App app, AppPrefs prefs) {
            ContentValues values = new ContentValues(3);
            values.put(Cols.IGNORE_ALL_UPDATES, prefs.ignoreAllUpdates);
            values.put(Cols.IGNORE_THIS_UPDATE, prefs.ignoreThisUpdate);
            values.put(Cols.IGNORE_VULNERABILITIES, prefs.ignoreVulnerabilities);

            if (getPrefsOrNull(context, app) == null) {
                values.put(Cols.PACKAGE_NAME, app.packageName);
                context.getContentResolver().insert(getContentUri(), values);
            } else {
                context.getContentResolver().update(getAppUri(app.packageName), values, null, null);
            }
        }

        @NonNull
        public static AppPrefs getPrefsOrDefault(Context context, App app) {
            AppPrefs prefs = getPrefsOrNull(context, app);
            return prefs == null ? AppPrefs.createDefault() : prefs;
        }

        @Nullable
        public static AppPrefs getPrefsOrNull(Context context, App app) {
            Cursor cursor = context.getContentResolver().query(getAppUri(app.packageName), Cols.ALL,
                    null, null, null);
            if (cursor == null) {
                return null;
            }

            try {
                if (cursor.getCount() == 0) {
                    return null;
                }

                cursor.moveToFirst();
                return new AppPrefs(
                        cursor.getInt(cursor.getColumnIndexOrThrow(Cols.IGNORE_THIS_UPDATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Cols.IGNORE_ALL_UPDATES)) > 0,
                        cursor.getInt(cursor.getColumnIndexOrThrow(Cols.IGNORE_VULNERABILITIES)) > 0);
            } finally {
                cursor.close();
            }
        }
    }

    private class Query extends QueryBuilder {

        @Override
        protected String getRequiredTables() {
            return AppPrefsTable.NAME;
        }

        @Override
        public void addField(String field) {
            appendField(field, getTableName());
        }
    }

    private static final String PROVIDER_NAME = "AppPrefsProvider";

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    private static final String PATH_PACKAGE_NAME = "packageName";

    static {
        MATCHER.addURI(getAuthority(), PATH_PACKAGE_NAME + "/*", CODE_SINGLE);
    }

    private static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getAppUri(String packageName) {
        return getContentUri().buildUpon().appendPath(PATH_PACKAGE_NAME).appendPath(packageName).build();
    }

    @Override
    protected String getTableName() {
        return AppPrefsTable.NAME;
    }

    @Override
    protected String getProviderName() {
        return "AppPrefsProvider";
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
    public Cursor query(@NonNull Uri uri, String[] projection,
                        String customSelection, String[] selectionArgs, String sortOrder) {
        if (MATCHER.match(uri) != CODE_SINGLE) {
            throw new UnsupportedOperationException("Invalid URI for app content provider: " + uri);
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

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Delete not supported for " + uri + ".");
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        db().insertOrThrow(getTableName(), null, values);
        getContext().getContentResolver().notifyChange(AppProvider.getCanUpdateUri(), null);
        return getAppUri(values.getAsString(Cols.PACKAGE_NAME));
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        if (MATCHER.match(uri) != CODE_SINGLE) {
            throw new UnsupportedOperationException("Update not supported for " + uri + ".");
        }

        QuerySelection query = new QuerySelection(where, whereArgs).add(querySingle(uri.getLastPathSegment()));
        int count = db().update(getTableName(), values, query.getSelection(), query.getArgs());
        getContext().getContentResolver().notifyChange(AppProvider.getCanUpdateUri(), null);
        return count;
    }
}
