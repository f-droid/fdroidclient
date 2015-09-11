package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.util.HashMap;
import java.util.Map;

public class InstalledAppProvider extends FDroidProvider {

    private static final String TAG = "InstalledAppProvider";

    public static class Helper {

        /**
         * @return The keys are the app ids (package names), and their corresponding values are
         * the version code which is installed.
         */
        public static Map<String, Integer> all(Context context) {

            Map<String, Integer> cachedInfo = new HashMap<>();

            final Uri uri = InstalledAppProvider.getContentUri();
            final String[] projection = InstalledAppProvider.DataColumns.ALL;
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        cachedInfo.put(
                            cursor.getString(cursor.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID)),
                            cursor.getInt(cursor.getColumnIndex(InstalledAppProvider.DataColumns.VERSION_CODE))
                        );
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }

            return cachedInfo;
        }

    }

    public interface DataColumns {

        String _ID = "rowid as _id"; // Required for CursorLoaders
        String APP_ID = "appId";
        String VERSION_CODE = "versionCode";
        String VERSION_NAME = "versionName";
        String APPLICATION_LABEL = "applicationLabel";

        String[] ALL = {
                _ID, APP_ID, VERSION_CODE, VERSION_NAME, APPLICATION_LABEL,
        };

    }

    private static final String PROVIDER_NAME = "InstalledAppProvider";

    private static final String PATH_SEARCH = "search";
    private static final int    CODE_SEARCH = CODE_SINGLE + 1;

    private static final UriMatcher matcher = new UriMatcher(-1);

    static {
        matcher.addURI(getAuthority(), null, CODE_LIST);
        matcher.addURI(getAuthority(), PATH_SEARCH + "/*", CODE_SEARCH);
        matcher.addURI(getAuthority(), "*", CODE_SINGLE);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getAppUri(String appId) {
        return Uri.withAppendedPath(getContentUri(), appId);
    }

    public static Uri getSearchUri(String keywords) {
        return getContentUri().buildUpon()
            .appendPath(PATH_SEARCH)
            .appendPath(keywords)
            .build();
    }

    public static String getApplicationLabel(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return appInfo.loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            Utils.debugLog(TAG, "Could not get application label", e);
        }
        return packageName; // all else fails, return id
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
        return new QuerySelection("appId = ?", new String[]{ appId });
    }

    private QuerySelection querySearch(String query) {
        return new QuerySelection("applicationLabel LIKE ?",
                new String[]{ "%" + query + "%" });
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        if (sortOrder == null) {
            sortOrder = DataColumns.APPLICATION_LABEL;
        }

        QuerySelection selection = new QuerySelection(customSelection, selectionArgs);
        switch (matcher.match(uri)) {
        case CODE_LIST:
            break;

        case CODE_SINGLE:
            selection = selection.add(queryApp(uri.getLastPathSegment()));
            break;

        case CODE_SEARCH:
            selection = selection.add(querySearch(uri.getLastPathSegment()));
            break;

        default:
            String message = "Invalid URI for installed app content provider: " + uri;
            Log.e(TAG, message);
            throw new UnsupportedOperationException(message);
        }

        Cursor cursor = read().query(getTableName(), projection, selection.getSelection(), selection.getArgs(), null, null, sortOrder);
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
        write().replaceOrThrow(getTableName(), null, values);
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return getAppUri(values.getAsString(DataColumns.APP_ID));
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("\"Update' not supported for installed appp provider. Instead, you should insert, and it will overwrite the relevant rows if one exists.");
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
