package org.fdroid.fdroid.data;

import android.content.*;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.util.*;

public class AppProvider extends FDroidProvider {

    /**
     * @see org.fdroid.fdroid.data.ApkProvider.MAX_APKS_TO_QUERY
     */
    public static final int MAX_APPS_TO_QUERY = 900;

    public static final class Helper {

        private Helper() {}

        public static List<App> all(ContentResolver resolver) {
            return all(resolver, DataColumns.ALL);
        }

        public static List<App> all(ContentResolver resolver, String[] projection) {
            Uri uri = AppProvider.getContentUri();
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            return cursorToList(cursor);
        }

        private static List<App> cursorToList(Cursor cursor) {
            List<App> apps = new ArrayList<App>();
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    apps.add(new App(cursor));
                    cursor.moveToNext();
                }
                cursor.close();
            }
            return apps;
        }

        public static String getCategoryAll(Context context) {
            return context.getString(R.string.category_all);
        }

        public static String getCategoryWhatsNew(Context context) {
            return context.getString(R.string.category_whatsnew);
        }

        public static String getCategoryRecentlyUpdated(Context context) {
            return context.getString(R.string.category_recentlyupdated);
        }

        public static List<String> categories(Context context) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getContentUri();
            String[] projection = { DataColumns.CATEGORIES };
            Cursor cursor = resolver.query(uri, projection, null, null, null );
            Set<String> categorySet = new HashSet<String>();
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    String categoriesString = cursor.getString(0);
                    if (categoriesString != null) {
                        for( String s : Utils.CommaSeparatedList.make(categoriesString)) {
                            categorySet.add(s);
                        }
                    }
                    cursor.moveToNext();
                }
            }
            List<String> categories = new ArrayList<String>(categorySet);
            Collections.sort(categories);

            // Populate the category list with the real categories, and the
            // locally generated meta-categories for "What's New", "Recently
            // Updated" and "All"...
            categories.add(0, getCategoryAll(context));
            categories.add(0, getCategoryRecentlyUpdated(context));
            categories.add(0, getCategoryWhatsNew(context));

            return categories;
        }

        public static App findById(ContentResolver resolver, String appId) {
            return findById(resolver, appId, DataColumns.ALL);
        }

        public static App findById(ContentResolver resolver, String appId,
                                   String[] projection) {
            Uri uri = getContentUri(appId);
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                return new App(cursor);
            } else {
                return null;
            }
        }

        public static void deleteAppsWithNoApks(ContentResolver resolver) {
        }
    }

    public interface DataColumns {

        public static final String _ID = "rowid as _id"; // Required for CursorLoaders
        public static final String _COUNT = "_count";
        public static final String IS_COMPATIBLE = "compatible";
        public static final String APP_ID = "id";
        public static final String NAME = "name";
        public static final String SUMMARY = "summary";
        public static final String ICON = "icon";
        public static final String DESCRIPTION = "description";
        public static final String LICENSE = "license";
        public static final String WEB_URL = "webURL";
        public static final String TRACKER_URL = "trackerURL";
        public static final String SOURCE_URL = "sourceURL";
        public static final String DONATE_URL = "donateURL";
        public static final String BITCOIN_ADDR = "bitcoinAddr";
        public static final String LITECOIN_ADDR = "litecoinAddr";
        public static final String DOGECOIN_ADDR = "dogecoinAddr";
        public static final String FLATTR_ID = "flattrID";
        public static final String SUGGESTED_VERSION_CODE = "suggestedVercode";
        public static final String UPSTREAM_VERSION = "upstreamVersion";
        public static final String UPSTREAM_VERSION_CODE = "upstreamVercode";
        public static final String CURRENT_APK = null;
        public static final String ADDED = "added";
        public static final String LAST_UPDATED = "lastUpdated";
        public static final String INSTALLED_VERSION = null;
        public static final String INSTALLED_VERCODE = null;
        public static final String USER_INSTALLED = null;
        public static final String CATEGORIES = "categories";
        public static final String ANTI_FEATURES = "antiFeatures";
        public static final String REQUIREMENTS = "requirements";
        public static final String FILTERED = null;
        public static final String HAS_UPDATES = null;
        public static final String TO_UPDATE = null;
        public static final String IGNORE_ALLUPDATES = "ignoreAllUpdates";
        public static final String IGNORE_THISUPDATE = "ignoreThisUpdate";
        public static final String ICON_URL = "iconUrl";
        public static final String UPDATED = null;
        public static final String APKS = null;

        public interface SuggestedApk {
            public static final String VERSION = "suggestedApkVersion";
        }

        public static String[] ALL = {
                IS_COMPATIBLE, APP_ID, NAME, SUMMARY, ICON, DESCRIPTION,
                LICENSE, WEB_URL, TRACKER_URL, SOURCE_URL, DONATE_URL,
                BITCOIN_ADDR, LITECOIN_ADDR, DOGECOIN_ADDR, FLATTR_ID,
                UPSTREAM_VERSION, UPSTREAM_VERSION_CODE, ADDED, LAST_UPDATED,
                CATEGORIES, ANTI_FEATURES, REQUIREMENTS, IGNORE_ALLUPDATES,
                IGNORE_THISUPDATE, ICON_URL, SUGGESTED_VERSION_CODE,
                SuggestedApk.VERSION
        };
    }

    private static class Query extends QueryBuilder {

        private boolean isSuggestedApkTableAdded = false;

        private boolean categoryFieldAdded = false;

        @Override
        protected String getRequiredTables() {
            return DBHelper.TABLE_APP;
        }

        @Override
        protected boolean isDistinct() {
            return fieldCount() == 1 && categoryFieldAdded;
        }

        @Override
        public void addField(String field) {
            if (field.equals(DataColumns.SuggestedApk.VERSION)) {
                addSuggestedApkVersionField();
            } else if (field.equals(DataColumns._COUNT)) {
                appendCountField();
            } else {
                if (field.equals(DataColumns.CATEGORIES)) {
                    categoryFieldAdded = true;
                }
                appendField(field, "fdroid_app");
            }
        }

        private void appendCountField() {
            appendField("COUNT(*) AS " + DataColumns._COUNT);
        }

        private void addSuggestedApkVersionField() {
            addSuggestedApkField(
                    ApkProvider.DataColumns.VERSION,
                    DataColumns.SuggestedApk.VERSION);
        }

        private void addSuggestedApkField(String fieldName, String alias) {
            if (!isSuggestedApkTableAdded) {
                isSuggestedApkTableAdded = true;
                leftJoin(
                    DBHelper.TABLE_APK,
                    "suggestedApk",
                    "fdroid_app.suggestedVercode = suggestedApk.vercode AND fdroid_app.id = suggestedApk.id");
            }
            appendField(fieldName, "suggestedApk", alias);
        }
    }

    private static final String PROVIDER_NAME = "AppProvider";

    private static final UriMatcher matcher = new UriMatcher(-1);

    private static final String PATH_INSTALLED = "installed";
    private static final String PATH_CAN_UPDATE = "canUpdate";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_NO_APKS = "noApks";
    private static final String PATH_APPS = "apps";
    private static final String PATH_RECENTLY_UPDATED = "recentlyUpdated";
    private static final String PATH_NEWLY_ADDED = "newlyAdded";
    private static final String PATH_CATEGORY = "category";

    private static final int CAN_UPDATE       = CODE_SINGLE + 1;
    private static final int INSTALLED        = CAN_UPDATE + 1;
    private static final int SEARCH           = INSTALLED + 1;
    private static final int NO_APKS          = SEARCH + 1;
    private static final int APPS             = NO_APKS + 1;
    private static final int RECENTLY_UPDATED = APPS + 1;
    private static final int NEWLY_ADDED      = RECENTLY_UPDATED + 1;
    private static final int CATEGORY         = NEWLY_ADDED + 1;

    static {
        matcher.addURI(getAuthority(), null, CODE_LIST);
        matcher.addURI(getAuthority(), PATH_RECENTLY_UPDATED, RECENTLY_UPDATED);
        matcher.addURI(getAuthority(), PATH_NEWLY_ADDED, NEWLY_ADDED);
        matcher.addURI(getAuthority(), PATH_CATEGORY + "/*", CATEGORY);
        matcher.addURI(getAuthority(), PATH_SEARCH + "/*", SEARCH);
        matcher.addURI(getAuthority(), PATH_CAN_UPDATE, CAN_UPDATE);
        matcher.addURI(getAuthority(), PATH_INSTALLED, INSTALLED);
        matcher.addURI(getAuthority(), PATH_NO_APKS, NO_APKS);
        matcher.addURI(getAuthority(), PATH_APPS + "/*", APPS);
        matcher.addURI(getAuthority(), "*", CODE_SINGLE);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getRecentlyUpdatedUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_RECENTLY_UPDATED);
    }

    public static Uri getNewlyAddedUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_NEWLY_ADDED);
    }

    public static Uri getCategoryUri(String category) {
        return getContentUri().buildUpon()
            .appendPath(PATH_CATEGORY)
            .appendPath(category)
            .build();
    }

    public static Uri getNoApksUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_NO_APKS);
    }

    public static Uri getInstalledUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_INSTALLED);
    }

    public static Uri getCanUpdateUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_CAN_UPDATE);
    }

    public static Uri getContentUri(List<App> apps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < apps.size(); i ++) {
            if (i != 0) {
                builder.append(',');
            }
            builder.append(apps.get(i).id);
        }
        return getContentUri().buildUpon()
            .appendPath(PATH_APPS)
            .appendPath(builder.toString())
            .build();
    }

    public static Uri getContentUri(App app) {
        return getContentUri(app.id);
    }

    public static Uri getContentUri(String appId) {
        return Uri.withAppendedPath(getContentUri(), appId);
    }

    public static Uri getSearchUri(String query) {
        return getContentUri().buildUpon()
            .appendPath(PATH_SEARCH)
            .appendPath(query)
            .build();
    }

    @Override
    protected String getTableName() {
        return DBHelper.TABLE_APP;
    }

    @Override
    protected String getProviderName() {
        return "AppProvider";
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    @Override
    protected UriMatcher getMatcher() {
        return matcher;
    }

    private QuerySelection queryCanUpdate() {
        Map<String, PackageInfo> installedApps = Utils.getInstalledApps(getContext());

        String ignoreCurrent = " fdroid_app.ignoreThisUpdate != fdroid_app.suggestedVercode ";
        String ignoreAll = " fdroid_app.ignoreAllUpdates != 1 ";
        String ignore = " ( " + ignoreCurrent + " AND " + ignoreAll + " ) ";

        StringBuilder where = new StringBuilder( ignore + " AND ( 0 ");
        String[] selectionArgs = new String[installedApps.size() * 2];
        int i = 0;
        for (PackageInfo info : installedApps.values() ) {
            where.append(" OR ( fdroid_app.")
                    .append(DataColumns.APP_ID)
                    .append(" = ? AND fdroid_app.")
                    .append(DataColumns.SUGGESTED_VERSION_CODE)
                    .append(" > ?) ");
            selectionArgs[ i * 2 ] = info.packageName;
            selectionArgs[ i * 2 + 1 ] = Integer.toString(info.versionCode);
            i ++;
        }
        where.append(") ");

        return new QuerySelection(where.toString(), selectionArgs);
    }

    private QuerySelection queryInstalled() {
        Map<String, PackageInfo> installedApps = Utils.getInstalledApps(getContext());
        StringBuilder where = new StringBuilder( " ( 0 ");
        String[] selectionArgs = new String[installedApps.size()];
        int i = 0;
        for (Map.Entry<String, PackageInfo> entry : installedApps.entrySet() ) {
            where.append(" OR fdroid_app.")
                    .append(AppProvider.DataColumns.APP_ID)
                    .append(" = ? ");
            selectionArgs[i] = entry.getKey();
            i ++;
        }
        where.append(" ) ");

        return new QuerySelection(where.toString(), selectionArgs);
    }

    private QuerySelection querySearch(String keywords) {
        keywords = "%" + keywords + "%";
        String selection =
                "fdroid_app.id like ? OR " +
                "fdroid_app.name like ? OR " +
                "fdroid_app.summary like ? OR " +
                "fdroid_app.description like ? ";
        String[] args = new String[] { keywords, keywords, keywords, keywords};
        return new QuerySelection(selection, args);
    }

    private QuerySelection querySingle(String id) {
        String selection = "fdroid_app.id = ?";
        String[] args = { id };
        return new QuerySelection(selection, args);
    }

    private QuerySelection queryNewlyAdded() {
        String selection = "fdroid_app.added > ?";
        String[] args = { Utils.DATE_FORMAT.format(Preferences.get().calcMaxHistory()) };
        return new QuerySelection(selection, args);
    }

    private QuerySelection queryRecentlyUpdated() {
        String selection = "fdroid_app.added != fdroid_app.lastUpdated AND fdroid_app.lastUpdated > ?";
        String[] args = { Utils.DATE_FORMAT.format(Preferences.get().calcMaxHistory()) };
        return new QuerySelection(selection, args);
    }

    private QuerySelection queryCategory(String category) {
        // TODO: In the future, add a new table for categories,
        // so we can join onto it.
        String selection =
                " fdroid_app.categories = ? OR " +    // Only category e.g. "internet"
                " fdroid_app.categories LIKE ? OR " + // First category e.g. "internet,%"
                " fdroid_app.categories LIKE ? OR " + // Last category e.g. "%,internet"
                " fdroid_app.categories LIKE ? ";     // One of many categories e.g. "%,internet,%"
        String[] args = {
                category,
                category + ",%",
                "%," + category,
                "%," + category + ",%",
        };
        return new QuerySelection(selection, args);
    }

    private QuerySelection queryNoApks() {
        String selection = "(SELECT COUNT(*) FROM fdroid_apk WHERE fdroid_apk.id = fdroid_app.id) = 0";
        return new QuerySelection(selection);
    }

    private QuerySelection queryApps(String appIds) {
        String[] args = appIds.split(",");
        String selection = "fdroid_app.id IN (" + generateQuestionMarksForInClause(args.length) + ")";
        return new QuerySelection(selection, args);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        QuerySelection query = new QuerySelection(selection, selectionArgs);
        switch (matcher.match(uri)) {
            case CODE_LIST:
                break;

            case CODE_SINGLE:
                query = query.add(querySingle(uri.getLastPathSegment()));
                break;

            case CAN_UPDATE:
                query = query.add(queryCanUpdate());
                break;

            case INSTALLED:
                query = query.add(queryInstalled());
                break;

            case SEARCH:
                query = query.add(querySearch(uri.getLastPathSegment()));
                break;

            case NO_APKS:
                query = query.add(queryNoApks());
                break;

            case APPS:
                query = query.add(queryApps(uri.getLastPathSegment()));
                break;

            case CATEGORY:
                query = query.add(queryCategory(uri.getLastPathSegment()));
                break;

            case RECENTLY_UPDATED:
                sortOrder = " fdroid_app.lastUpdated DESC";
                query = query.add(queryRecentlyUpdated());
                break;

            case NEWLY_ADDED:
                sortOrder = " fdroid_app.added DESC";
                query = query.add(queryNewlyAdded());
                break;

            default:
                Log.e("FDroid", "Invalid URI for app content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for app content provider: " + uri);
        }

        if (AppProvider.DataColumns.NAME.equals(sortOrder)) {
            sortOrder = " lower( fdroid_app." + sortOrder + " ) ";
        }

        Query q = new Query();
        q.addFields(projection);
        q.addSelection(query.getSelection());
        q.addOrderBy(sortOrder);

        Cursor cursor = read().rawQuery(q.toString(), query.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        QuerySelection query = new QuerySelection(where, whereArgs);
        switch (matcher.match(uri)) {

            case NO_APKS:
                query = query.add(queryNoApks());
                break;

            default:
                throw new UnsupportedOperationException("Can't delete yet");

        }

        int count = write().delete(getTableName(), query.getSelection(), query.getArgs());
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        write().insertOrThrow(getTableName(), null, values);
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return getContentUri(values.getAsString(DataColumns.APP_ID));
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        QuerySelection query = new QuerySelection(where, whereArgs);
        switch (matcher.match(uri)) {

            case CODE_SINGLE:
                query = query.add(querySingle(uri.getLastPathSegment()));
                break;

            default:
                throw new UnsupportedOperationException("Update not supported for '" + uri + "'.");

        }
        int count = write().update(getTableName(), values, query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

}
