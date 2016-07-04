package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.fdroid.fdroid.data.Schema.InstalledAppTable.Cols;

import java.util.HashMap;
import java.util.Map;

public class InstalledAppProvider extends FDroidProvider {

    private static final String TAG = "InstalledAppProvider";

    public static class Helper {

        /**
         * @return The keys are the package names, and their corresponding values are
         * the {@link PackageInfo#lastUpdateTime last update time} in milliseconds.
         */
        public static Map<String, Long> all(Context context) {

            Map<String, Long> cachedInfo = new HashMap<>();

            final Uri uri = InstalledAppProvider.getContentUri();
            final String[] projection = Cols.ALL;
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        cachedInfo.put(
                                cursor.getString(cursor.getColumnIndex(Cols.PACKAGE_NAME)),
                                cursor.getLong(cursor.getColumnIndex(Cols.LAST_UPDATE_TIME))
                        );
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }

            return cachedInfo;
        }

    }

    private static final String PROVIDER_NAME = "InstalledAppProvider";

    private static final String PATH_SEARCH = "search";
    private static final int    CODE_SEARCH = CODE_SINGLE + 1;

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    static {
        MATCHER.addURI(getAuthority(), null, CODE_LIST);
        MATCHER.addURI(getAuthority(), PATH_SEARCH + "/*", CODE_SEARCH);
        MATCHER.addURI(getAuthority(), "*", CODE_SINGLE);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    /**
     * @return the {@link Uri} that points to a specific installed app
     */
    public static Uri getAppUri(String packageName) {
        return Uri.withAppendedPath(getContentUri(), packageName);
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
        return packageName; // all else fails, return packageName
    }

    @Override
    protected String getTableName() {
        return InstalledAppTable.NAME;
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
        return MATCHER;
    }

    private QuerySelection queryApp(String packageName) {
        return new QuerySelection(Cols.PACKAGE_NAME + " = ?", new String[]{packageName});
    }

    private QuerySelection querySearch(String query) {
        return new QuerySelection(Cols.APPLICATION_LABEL + " LIKE ?",
                new String[]{"%" + query + "%"});
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        if (sortOrder == null) {
            sortOrder = Cols.APPLICATION_LABEL;
        }

        QuerySelection selection = new QuerySelection(customSelection, selectionArgs);
        switch (MATCHER.match(uri)) {
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

        Cursor cursor = db().query(getTableName(), projection, selection.getSelection(), selection.getArgs(), null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        if (MATCHER.match(uri) != CODE_SINGLE) {
            throw new UnsupportedOperationException("Delete not supported for " + uri + ".");
        }

        QuerySelection query = new QuerySelection(where, whereArgs);
        query = query.add(queryApp(uri.getLastPathSegment()));

        return db().delete(getTableName(), query.getSelection(), query.getArgs());
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        if (MATCHER.match(uri) != CODE_LIST) {
            throw new UnsupportedOperationException("Insert not supported for " + uri + ".");
        }

        verifyVersionNameNotNull(values);
        db().replaceOrThrow(getTableName(), null, values);
        return getAppUri(values.getAsString(Cols.PACKAGE_NAME));
    }

    /**
     * Update is not supported for {@code InstalledAppProvider}. Instead, use
     * {@link #insert(Uri, ContentValues)}, and it will overwrite the relevant
     * row, if one exists.  This just throws {@link UnsupportedOperationException}
     */
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
        if (values.containsKey(Cols.VERSION_NAME) && values.getAsString(Cols.VERSION_NAME) == null) {
            values.put(Cols.VERSION_NAME, getContext().getString(R.string.unknown));
        }
    }

}
