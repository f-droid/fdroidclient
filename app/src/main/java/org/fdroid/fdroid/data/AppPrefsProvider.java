package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.fdroid.fdroid.data.Schema.AppPrefsTable;
import org.fdroid.fdroid.data.Schema.AppPrefsTable.Cols;

public class AppPrefsProvider extends FDroidProvider {

    private static final String TAG = "AppPrefsProvider";

    public static final class Helper {
        private Helper() { }

        public static void update(Context context, App app, AppPrefs prefs) {
            ContentValues values = new ContentValues(3);
            values.put(Cols.IGNORE_ALL_UPDATES, prefs.ignoreAllUpdates);
            values.put(Cols.IGNORE_THIS_UPDATE, prefs.ignoreThisUpdate);

            if (getPrefsOrNull(context, app) == null) {
                values.put(Cols.APP_ID, app.getId());
                context.getContentResolver().insert(getContentUri(), values);
            } else {
                context.getContentResolver().update(getAppUri(app.getId()), values, null, null);
            }
        }

        @NonNull
        public static AppPrefs getPrefsOrDefault(Context context, App app) {
            AppPrefs prefs = getPrefsOrNull(context, app);
            return prefs == null ? AppPrefs.createDefault() : prefs;
        }

        @Nullable
        public static AppPrefs getPrefsOrNull(Context context, App app) {
            Cursor cursor = context.getContentResolver().query(getAppUri(app.getId()), Cols.ALL, null, null, null);
            if (cursor == null) {
                return null;
            }

            try {
                if (cursor.getCount() == 0) {
                    return null;
                } else {
                    cursor.moveToFirst();
                    return new AppPrefs(
                            cursor.getInt(cursor.getColumnIndexOrThrow(Cols.IGNORE_THIS_UPDATE)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(Cols.IGNORE_ALL_UPDATES)) > 0);
                }
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

    private static final String PATH_APP_ID = "appId";

    static {
        MATCHER.addURI(getAuthority(), PATH_APP_ID + "/#", CODE_SINGLE);
    }

    private static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getAppUri(long appId) {
        return getContentUri().buildUpon().appendPath(PATH_APP_ID).appendPath(Long.toString(appId)).build();
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

    protected QuerySelection querySingle(long appId) {
        final String selection = getTableName() + "." + Cols.APP_ID + " = ?";
        final String[] args = {Long.toString(appId)};
        return new QuerySelection(selection, args);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        QuerySelection selection = new QuerySelection(customSelection, selectionArgs);

        switch (MATCHER.match(uri)) {
            case CODE_SINGLE:
                selection = selection.add(querySingle(Long.parseLong(uri.getLastPathSegment())));
                break;

            default:
                Log.e(TAG, "Invalid URI for app content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for app content provider: " + uri);
        }

        Query query = new Query();
        query.addSelection(selection);
        query.addFields(projection);
        query.addOrderBy(sortOrder);

        Cursor cursor = LoggingQuery.query(db(), query.toString(), query.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        switch (MATCHER.match(uri)) {
            default:
                throw new UnsupportedOperationException("Delete not supported for " + uri + ".");
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        db().insertOrThrow(getTableName(), null, values);
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return getAppUri(values.getAsLong(Cols.APP_ID));
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        switch (MATCHER.match(uri)) {
            case CODE_SINGLE:
                QuerySelection query = new QuerySelection(where, whereArgs)
                        .add(querySingle(Long.parseLong(uri.getLastPathSegment())));
                return db().update(getTableName(), values, query.getSelection(), query.getArgs());

            default:
                throw new UnsupportedOperationException("Update not supported for " + uri + ".");

        }
    }
}
