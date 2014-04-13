package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.fdroid.fdroid.R;

import java.util.HashMap;
import java.util.Map;

public class InstalledAppProvider extends FDroidProvider {

    public static class Helper {

        /**
         * @return The keys are the app ids (package names), and their corresponding values are
         * the version code which is installed.
         */
        public static Map<String, Integer> all(Context context) {

            Map<String, Integer> cachedInfo = new HashMap<String, Integer>();

            Uri uri = InstalledAppProvider.getContentUri();
            String[] projection = InstalledAppProvider.DataColumns.ALL;
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    cachedInfo.put(
                        cursor.getString(cursor.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID)),
                        cursor.getInt(cursor.getColumnIndex(InstalledAppProvider.DataColumns.VERSION_CODE))
                    );
                    cursor.moveToNext();
                }
                cursor.close();
            }

            return cachedInfo;
        }

    }

    public interface DataColumns {

        public static final String APP_ID = "appId";
        public static final String VERSION_CODE = "versionCode";
        public static final String VERSION_NAME = "versionName";

        public static String[] ALL = { APP_ID, VERSION_CODE, VERSION_NAME };

    }

    private static final String PROVIDER_NAME = "InstalledAppProvider";

    private static final UriMatcher matcher = new UriMatcher(-1);

    static {
        matcher.addURI(getAuthority(), null, CODE_LIST);
        matcher.addURI(getAuthority(), "*", CODE_SINGLE);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getAppUri(String appId) {
        return Uri.withAppendedPath(getContentUri(), appId);
    }

    @Override
    protected String getTableName() {
        return DBHelper.TABLE_INSTALLED_APP;
    }

    @Override
    protected String getProviderName() {
        return "InstalledAppProvider";
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    @Override
    protected UriMatcher getMatcher() {
        return matcher;
    }

    private QuerySelection queryApp(String appId) {
        return new QuerySelection("appId = ?", new String[] { appId } );
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        QuerySelection selection = new QuerySelection(customSelection, selectionArgs);
        switch (matcher.match(uri)) {
            case CODE_LIST:
                break;

            case CODE_SINGLE:
                selection = selection.add(queryApp(uri.getLastPathSegment()));
                break;

            default:
                String message = "Invalid URI for installed app content provider: " + uri;
                Log.e("FDroid", message);
                throw new UnsupportedOperationException(message);
        }

        Cursor cursor = read().query(getTableName(), projection, selection.getSelection(), selection.getArgs(), null, null, null);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        if (matcher.match(uri) != CODE_SINGLE) {
            throw new UnsupportedOperationException("Delete not supported for " + uri + ".");
        }

        QuerySelection query = new QuerySelection(where, whereArgs);
        query = query.add(queryApp(uri.getLastPathSegment()));

        int count = write().delete(getTableName(), query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        if (matcher.match(uri) != CODE_LIST) {
            throw new UnsupportedOperationException("Insert not supported for " + uri + ".");
        }

        verifyVersionNameNotNull(values);
        write().insertOrThrow(getTableName(), null, values);
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return getAppUri(values.getAsString(DataColumns.APP_ID));
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        if (matcher.match(uri) != CODE_SINGLE) {
            throw new UnsupportedOperationException("Update not supported for " + uri + ".");
        }

        QuerySelection query = new QuerySelection(where, whereArgs);
        query = query.add(queryApp(uri.getLastPathSegment()));

        verifyVersionNameNotNull(values);
        int count = write().update(getTableName(), values, query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * During development, I stumbled across one (out of over 300) installed apps which had a versionName
     * of null. As such, I figured we may as well store it as "Unknown". The alternative is to allow the
     * column to accept NULL values in the database, and then deal with the potential of a null everywhere
     * "versionName" is used.
     */
    private void verifyVersionNameNotNull(ContentValues values) {
        if (values.containsKey(DataColumns.VERSION_NAME) && values.getAsString(DataColumns.VERSION_NAME) == null) {
            values.put(DataColumns.VERSION_NAME, getContext().getString(R.string.unknown));
        }
    }

}
