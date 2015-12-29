package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppProvider extends FDroidProvider {

    private static final String TAG = "AppProvider";

    public static final class Helper {

        private Helper() { }

        public static int count(Context context, Uri uri) {
            final String[] projection = {AppProvider.DataColumns._COUNT};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            int count = 0;
            if (cursor != null) {
                if (cursor.getCount() == 1) {
                    cursor.moveToFirst();
                    count = cursor.getInt(0);
                }
                cursor.close();
            }
            return count;
        }

        public static List<App> all(ContentResolver resolver) {
            return all(resolver, DataColumns.ALL);
        }

        public static List<App> all(ContentResolver resolver, String[] projection) {
            final Uri uri = AppProvider.getContentUri();
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            return cursorToList(cursor);
        }

        public static List<App> findIgnored(Context context, String[] projection) {
            final Uri uri = AppProvider.getIgnoredUri();
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            return cursorToList(cursor);
        }

        private static List<App> cursorToList(Cursor cursor) {
            int knownAppCount = cursor != null ? cursor.getCount() : 0;
            List<App> apps = new ArrayList<>(knownAppCount);
            if (cursor != null) {
                if (knownAppCount > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        apps.add(new App(cursor));
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            return apps;
        }

        public static String getCategoryAll(Context context) {
            return context.getString(R.string.category_All);
        }

        public static String getCategoryWhatsNew(Context context) {
            return context.getString(R.string.category_Whats_New);
        }

        public static String getCategoryRecentlyUpdated(Context context) {
            return context.getString(R.string.category_Recently_Updated);
        }

        public static List<String> categories(Context context) {
            final ContentResolver resolver = context.getContentResolver();
            final Uri uri = getContentUri();
            final String[] projection = {DataColumns.CATEGORIES};
            final Cursor cursor = resolver.query(uri, projection, null, null, null);
            final Set<String> categorySet = new HashSet<>();
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        final String categoriesString = cursor.getString(0);
                        Utils.CommaSeparatedList categoriesList = Utils.CommaSeparatedList.make(categoriesString);
                        if (categoriesList != null) {
                            for (final String s : categoriesList) {
                                categorySet.add(s);
                            }
                        }
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            final List<String> categories = new ArrayList<>(categorySet);
            Collections.sort(categories);

            // Populate the category list with the real categories, and the
            // locally generated meta-categories for "What's New", "Recently
            // Updated" and "All"...
            categories.add(0, getCategoryAll(context));
            categories.add(0, getCategoryRecentlyUpdated(context));
            categories.add(0, getCategoryWhatsNew(context));

            return categories;
        }

        public static App findByPackageName(ContentResolver resolver, String packageName) {
            return findByPackageName(resolver, packageName, DataColumns.ALL);
        }

        public static App findByPackageName(ContentResolver resolver, String packageName,
                                            String[] projection) {
            final Uri uri = getContentUri(packageName);
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            App app = null;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    app = new App(cursor);
                }
                cursor.close();
            }
            return app;
        }

        /*
         * I wasn't quite sure on the best way to execute arbitrary queries using the same DBHelper as the
         * content provider class, so I've hidden the implementation of this (by making it private) in case
         * I find a better way in the future.
         */
        public static void calcDetailsFromIndex(Context context) {
            final Uri fromUpstream = calcAppDetailsFromIndexUri();
            context.getContentResolver().update(fromUpstream, null, null, null);
        }

    }

    /**
     * Class that only exists to call private methods in the {@link AppProvider} without having
     * to go via a Context/ContentResolver. The reason is that if the {@link DBHelper} class
     * was to try and use its getContext().getContentResolver() in order to access the app
     * provider, then the AppProvider will end up creating a new instance of a writeable
     * SQLiteDatabase. This causes problems because the {@link DBHelper} still has its reference
     * open and locks certain tables.
     */
    static final class UpgradeHelper {

        public static void updateIconUrls(Context context, SQLiteDatabase db) {
            AppProvider.updateIconUrls(context, db, DBHelper.TABLE_APP, DBHelper.TABLE_APK);
        }

    }

    public interface DataColumns {

        String _ID = "rowid as _id"; // Required for CursorLoaders
        String _COUNT = "_count";
        String IS_COMPATIBLE = "compatible";
        String PACKAGE_NAME = "id";
        String NAME = "name";
        String SUMMARY = "summary";
        String ICON = "icon";
        String DESCRIPTION = "description";
        String LICENSE = "license";
        String WEB_URL = "webURL";
        String TRACKER_URL = "trackerURL";
        String SOURCE_URL = "sourceURL";
        String CHANGELOG_URL = "changelogURL";
        String DONATE_URL = "donateURL";
        String BITCOIN_ADDR = "bitcoinAddr";
        String LITECOIN_ADDR = "litecoinAddr";
        String FLATTR_ID = "flattrID";
        String SUGGESTED_VERSION_CODE = "suggestedVercode";
        String UPSTREAM_VERSION = "upstreamVersion";
        String UPSTREAM_VERSION_CODE = "upstreamVercode";
        String ADDED = "added";
        String LAST_UPDATED = "lastUpdated";
        String CATEGORIES = "categories";
        String ANTI_FEATURES = "antiFeatures";
        String REQUIREMENTS = "requirements";
        String IGNORE_ALLUPDATES = "ignoreAllUpdates";
        String IGNORE_THISUPDATE = "ignoreThisUpdate";
        String ICON_URL = "iconUrl";
        String ICON_URL_LARGE = "iconUrlLarge";

        interface SuggestedApk {
            String VERSION = "suggestedApkVersion";
        }

        interface InstalledApp {
            String VERSION_CODE = "installedVersionCode";
            String VERSION_NAME = "installedVersionName";
            String SIGNATURE = "installedSig";
        }

        String[] ALL = {
            _ID, IS_COMPATIBLE, PACKAGE_NAME, NAME, SUMMARY, ICON, DESCRIPTION,
            LICENSE, WEB_URL, TRACKER_URL, SOURCE_URL, CHANGELOG_URL, DONATE_URL,
            BITCOIN_ADDR, LITECOIN_ADDR, FLATTR_ID,
            UPSTREAM_VERSION, UPSTREAM_VERSION_CODE, ADDED, LAST_UPDATED,
            CATEGORIES, ANTI_FEATURES, REQUIREMENTS, IGNORE_ALLUPDATES,
            IGNORE_THISUPDATE, ICON_URL, ICON_URL_LARGE,
            SUGGESTED_VERSION_CODE, SuggestedApk.VERSION,
            InstalledApp.VERSION_CODE, InstalledApp.VERSION_NAME,
            InstalledApp.SIGNATURE,
        };
    }

    /**
     * A QuerySelection which is aware of the option/need to join onto the
     * installed apps table. Not that the base classes
     * {@link org.fdroid.fdroid.data.QuerySelection#add(QuerySelection)} and
     * {@link org.fdroid.fdroid.data.QuerySelection#add(String, String[])} methods
     * will only return the base class {@link org.fdroid.fdroid.data.QuerySelection}
     * which is not aware of the installed app table.
     * However, the
     * {@link org.fdroid.fdroid.data.AppProvider.AppQuerySelection#add(org.fdroid.fdroid.data.AppProvider.AppQuerySelection)}
     * method from this class will return an instance of this class, that is aware of
     * the install apps table.
     */
    private static class AppQuerySelection extends QuerySelection {

        private boolean naturalJoinToInstalled;

        AppQuerySelection() {
            // The same as no selection, because "1" will always resolve to true when executing the SQL query.
            // e.g. "WHERE 1 AND ..." is the same as "WHERE ..."
            super("1");
        }

        AppQuerySelection(String selection) {
            super(selection);
        }

        AppQuerySelection(String selection, String[] args) {
            super(selection, args);
        }

        AppQuerySelection(String selection, List<String> args) {
            super(selection, args);
        }

        public boolean naturalJoinToInstalled() {
            return naturalJoinToInstalled;
        }

        /**
         * Tells the query selection that it will need to join onto the installed apps table
         * when used. This should be called when your query makes use of fields from that table
         * (for example, list all installed, or list those which can be updated).
         * @return A reference to this object, to allow method chaining, for example
         * <code>return new AppQuerySelection(selection).requiresInstalledTable())</code>
         */
        public AppQuerySelection requireNaturalInstalledTable() {
            naturalJoinToInstalled = true;
            return this;
        }

        public AppQuerySelection add(AppQuerySelection query) {
            QuerySelection both = super.add(query);
            AppQuerySelection bothWithJoin = new AppQuerySelection(both.getSelection(), both.getArgs());
            if (this.naturalJoinToInstalled() || query.naturalJoinToInstalled()) {
                bothWithJoin.requireNaturalInstalledTable();
            }
            return bothWithJoin;
        }

    }

    private class Query extends QueryBuilder {

        private boolean isSuggestedApkTableAdded;
        private boolean requiresInstalledTable;
        private boolean categoryFieldAdded;
        private boolean countFieldAppended;

        @Override
        protected String getRequiredTables() {
            final String app  = getTableName();
            final String apk  = getApkTableName();
            final String repo = DBHelper.TABLE_REPO;

            return app +
                " LEFT JOIN " + apk + " ON ( " + apk + ".id = " + app + ".id ) " +
                " LEFT JOIN " + repo + " ON ( " + apk + ".repo = " + repo + "._id )";
        }

        @Override
        protected boolean isDistinct() {
            return fieldCount() == 1 && categoryFieldAdded;
        }

        @Override
        protected String groupBy() {
            // If the count field has been requested, then we want to group all rows together.
            return countFieldAppended ? null : getTableName() + ".id";
        }

        public void addSelection(AppQuerySelection selection) {
            addSelection(selection.getSelection());
            if (selection.naturalJoinToInstalled()) {
                naturalJoinToInstalledTable();
            }
        }

        // TODO: What if the selection requires a natural join, but we first get a left join
        // because something causes leftJoin to be caused first? Maybe throw an exception?
        public void naturalJoinToInstalledTable() {
            if (!requiresInstalledTable) {
                join(
                        DBHelper.TABLE_INSTALLED_APP,
                        "installed",
                        "installed." + InstalledAppProvider.DataColumns.PACKAGE_NAME + " = " + getTableName() + ".id");
                requiresInstalledTable = true;
            }
        }

        public void leftJoinToInstalledTable() {
            if (!requiresInstalledTable) {
                leftJoin(
                        DBHelper.TABLE_INSTALLED_APP,
                        "installed",
                        "installed." + InstalledAppProvider.DataColumns.PACKAGE_NAME + " = " + getTableName() + ".id");
                requiresInstalledTable = true;
            }
        }

        @Override
        public void addField(String field) {
            switch (field) {
                case DataColumns.SuggestedApk.VERSION:
                    addSuggestedApkVersionField();
                    break;
                case DataColumns.InstalledApp.VERSION_NAME:
                    addInstalledAppVersionName();
                    break;
                case DataColumns.InstalledApp.VERSION_CODE:
                    addInstalledAppVersionCode();
                    break;
                case DataColumns.InstalledApp.SIGNATURE:
                    addInstalledSig();
                    break;
                case DataColumns._COUNT:
                    appendCountField();
                    break;
                default:
                    if (field.equals(DataColumns.CATEGORIES)) {
                        categoryFieldAdded = true;
                    }
                    appendField(field, getTableName());
                    break;
            }
        }

        private void appendCountField() {
            countFieldAppended = true;
            appendField("COUNT( DISTINCT " + getTableName() + ".id ) AS " + DataColumns._COUNT);
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
                        getApkTableName(),
                        "suggestedApk",
                        getTableName() + ".suggestedVercode = suggestedApk.vercode AND " + getTableName() + ".id = suggestedApk.id");
            }
            appendField(fieldName, "suggestedApk", alias);
        }

        private void addInstalledAppVersionName() {
            addInstalledAppField(
                    InstalledAppProvider.DataColumns.VERSION_NAME,
                    DataColumns.InstalledApp.VERSION_NAME
            );
        }

        private void addInstalledAppVersionCode() {
            addInstalledAppField(
                    InstalledAppProvider.DataColumns.VERSION_CODE,
                    DataColumns.InstalledApp.VERSION_CODE
            );
        }

        private void addInstalledSig() {
            addInstalledAppField(
                    InstalledAppProvider.DataColumns.SIGNATURE,
                    DataColumns.InstalledApp.SIGNATURE
            );
        }

        private void addInstalledAppField(String fieldName, String alias) {
            leftJoinToInstalledTable();
            appendField(fieldName, "installed", alias);
        }
    }

    private static final String PROVIDER_NAME = "AppProvider";

    private static final UriMatcher matcher = new UriMatcher(-1);

    private static final String PATH_INSTALLED = "installed";
    private static final String PATH_CAN_UPDATE = "canUpdate";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_SEARCH_INSTALLED = "searchInstalled";
    private static final String PATH_SEARCH_CAN_UPDATE = "searchCanUpdate";
    private static final String PATH_SEARCH_REPO = "searchRepo";
    private static final String PATH_NO_APKS = "noApks";
    private static final String PATH_APPS = "apps";
    private static final String PATH_RECENTLY_UPDATED = "recentlyUpdated";
    private static final String PATH_NEWLY_ADDED = "newlyAdded";
    private static final String PATH_CATEGORY = "category";
    private static final String PATH_IGNORED = "ignored";
    private static final String PATH_CALC_APP_DETAILS_FROM_INDEX = "calcDetailsFromIndex";
    private static final String PATH_REPO = "repo";

    private static final int CAN_UPDATE = CODE_SINGLE + 1;
    private static final int INSTALLED = CAN_UPDATE + 1;
    private static final int SEARCH = INSTALLED + 1;
    private static final int NO_APKS = SEARCH + 1;
    private static final int APPS = NO_APKS + 1;
    private static final int RECENTLY_UPDATED = APPS + 1;
    private static final int NEWLY_ADDED = RECENTLY_UPDATED + 1;
    private static final int CATEGORY = NEWLY_ADDED + 1;
    private static final int IGNORED = CATEGORY + 1;
    private static final int CALC_APP_DETAILS_FROM_INDEX = IGNORED + 1;
    private static final int REPO = CALC_APP_DETAILS_FROM_INDEX + 1;
    private static final int SEARCH_REPO = REPO + 1;
    private static final int SEARCH_INSTALLED = SEARCH_REPO + 1;
    private static final int SEARCH_CAN_UPDATE = SEARCH_INSTALLED + 1;

    static {
        matcher.addURI(getAuthority(), null, CODE_LIST);
        matcher.addURI(getAuthority(), PATH_CALC_APP_DETAILS_FROM_INDEX, CALC_APP_DETAILS_FROM_INDEX);
        matcher.addURI(getAuthority(), PATH_IGNORED, IGNORED);
        matcher.addURI(getAuthority(), PATH_RECENTLY_UPDATED, RECENTLY_UPDATED);
        matcher.addURI(getAuthority(), PATH_NEWLY_ADDED, NEWLY_ADDED);
        matcher.addURI(getAuthority(), PATH_CATEGORY + "/*", CATEGORY);
        matcher.addURI(getAuthority(), PATH_SEARCH + "/*", SEARCH);
        matcher.addURI(getAuthority(), PATH_SEARCH_INSTALLED + "/*", SEARCH_INSTALLED);
        matcher.addURI(getAuthority(), PATH_SEARCH_CAN_UPDATE + "/*", SEARCH_CAN_UPDATE);
        matcher.addURI(getAuthority(), PATH_SEARCH_REPO + "/*/*", SEARCH_REPO);
        matcher.addURI(getAuthority(), PATH_REPO + "/#", REPO);
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

    public static Uri getIgnoredUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_IGNORED);
    }

    private static Uri calcAppDetailsFromIndexUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_CALC_APP_DETAILS_FROM_INDEX);
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

    public static Uri getRepoUri(Repo repo) {
        return getContentUri().buildUpon()
            .appendPath(PATH_REPO)
            .appendPath(String.valueOf(repo.id))
            .build();
    }

    public static Uri getContentUri(List<App> apps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < apps.size(); i++) {
            if (i != 0) {
                builder.append(',');
            }
            builder.append(apps.get(i).packageName);
        }
        return getContentUri().buildUpon()
            .appendPath(PATH_APPS)
            .appendPath(builder.toString())
            .build();
    }

    public static Uri getContentUri(App app) {
        return getContentUri(app.packageName);
    }

    public static Uri getContentUri(String packageName) {
        return Uri.withAppendedPath(getContentUri(), packageName);
    }

    public static Uri getSearchUri(String query) {
        return getContentUri().buildUpon()
            .appendPath(PATH_SEARCH)
            .appendEncodedPath(query)
            .build();
    }

    public static Uri getSearchInstalledUri(String query) {
        return getContentUri()
            .buildUpon()
            .appendPath(PATH_SEARCH_INSTALLED)
            .appendEncodedPath(query)
            .build();
    }

    public static Uri getSearchCanUpdateUri(String query) {
        return getContentUri()
            .buildUpon()
            .appendPath(PATH_SEARCH_CAN_UPDATE)
            .appendEncodedPath(query)
            .build();
    }

    public static Uri getSearchUri(Repo repo, String query) {
        return getContentUri().buildUpon()
            .appendPath(PATH_SEARCH_REPO)
            .appendPath(repo.id + "")
            .appendEncodedPath(query)
            .build();
    }

    @Override
    protected String getTableName() {
        return DBHelper.TABLE_APP;
    }

    protected String getApkTableName() {
        return DBHelper.TABLE_APK;
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

    private AppQuerySelection queryCanUpdate() {
        final String ignoreCurrent = getTableName() + ".ignoreThisUpdate != " + getTableName() + ".suggestedVercode ";
        final String ignoreAll = getTableName() + ".ignoreAllUpdates != 1 ";
        final String ignore = " ( " + ignoreCurrent + " AND " + ignoreAll + " ) ";
        final String where = ignore + " AND " + getTableName() + "." + DataColumns.SUGGESTED_VERSION_CODE + " > installed.versionCode";
        return new AppQuerySelection(where).requireNaturalInstalledTable();
    }

    private AppQuerySelection queryRepo(long repoId) {
        final String selection = getApkTableName() + ".repo = ? ";
        final String[] args = {String.valueOf(repoId)};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryInstalled() {
        return new AppQuerySelection().requireNaturalInstalledTable();
    }

    private AppQuerySelection querySearch(String query) {
        final String[] columns = {
                getTableName() + ".id",
                getTableName() + ".name",
                getTableName() + ".summary",
                getTableName() + ".description",
        };

        // Remove duplicates, surround in % for wildcard searching
        final Set<String> keywordSet = new HashSet<>(Arrays.asList(query.split("\\s")));
        final String[] keywords = new String[keywordSet.size()];
        int iKeyword = 0;
        for (final String keyword : keywordSet) {
            keywords[iKeyword] = "%" + keyword + "%";
            iKeyword++;
        }

        // Build selection string and fill out keyword arguments
        final StringBuilder selection = new StringBuilder();
        final String[] selectionKeywords = new String[columns.length * keywords.length];
        iKeyword = 0;
        boolean firstColumn = true;
        for (final String column : columns) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                selection.append("OR ");
            }
            selection.append('(');
            boolean firstKeyword = true;
            for (final String keyword : keywords) {
                if (firstKeyword) {
                    firstKeyword = false;
                } else {
                    selection.append(" AND ");
                }
                selection.append(column).append(" like ?");
                selectionKeywords[iKeyword] = keyword;
                iKeyword++;
            }
            selection.append(") ");
        }
        return new AppQuerySelection(selection.toString(), selectionKeywords);
    }

    protected AppQuerySelection querySingle(String packageName) {
        final String selection = getTableName() + ".id = ?";
        final String[] args = {packageName};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryIgnored() {
        final String table = getTableName();
        final String selection = table + ".ignoreAllUpdates = 1 OR " +
                table + ".ignoreThisUpdate >= " + table + ".suggestedVercode";
        return new AppQuerySelection(selection);
    }

    private AppQuerySelection queryExcludeSwap() {
        // fdroid_repo will have null fields if the LEFT JOIN didn't resolve, e.g. due to there
        // being no apks for the app in the result set. In that case, we can't tell if it is from
        // a swap repo or not.
        final String selection = " fdroid_repo.isSwap = 0 OR fdroid_repo.isSwap is null ";
        return new AppQuerySelection(selection);
    }

    private AppQuerySelection queryNewlyAdded() {
        final String selection = getTableName() + ".added > ?";
        final String[] args = {Utils.formatDate(Preferences.get().calcMaxHistory(), "")};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryRecentlyUpdated() {
        final String app = getTableName();
        final String selection = app + ".added != " + app + ".lastUpdated AND " + app + ".lastUpdated > ?";
        final String[] args = {Utils.formatDate(Preferences.get().calcMaxHistory(), "")};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryCategory(String category) {
        // TODO: In the future, add a new table for categories,
        // so we can join onto it.
        final String selection =
                getTableName() + ".categories = ? OR " +    // Only category e.g. "internet"
                getTableName() + ".categories LIKE ? OR " + // First category e.g. "internet,%"
                getTableName() + ".categories LIKE ? OR " + // Last category e.g. "%,internet"
                getTableName() + ".categories LIKE ? ";     // One of many categories e.g. "%,internet,%"
        final String[] args = {
            category,
            category + ",%",
            "%," + category,
            "%," + category + ",%",
        };
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryNoApks() {
        String selection = "(SELECT COUNT(*) FROM " + getApkTableName() + " WHERE " + getApkTableName() + ".id = " + getTableName() + ".id) = 0";
        return new AppQuerySelection(selection);
    }

    static AppQuerySelection queryApps(String packageNames, String idField) {
        String[] args = packageNames.split(",");
        String selection = idField + " IN (" + generateQuestionMarksForInClause(args.length) + ")";
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryApps(String packageNames) {
        return queryApps(packageNames, getTableName() + ".id");
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        AppQuerySelection selection = new AppQuerySelection(customSelection, selectionArgs);

        // Queries which are for the main list of apps should not include swap apps.
        boolean includeSwap = true;

        switch (matcher.match(uri)) {
            case CODE_LIST:
                includeSwap = false;
                break;

            case CODE_SINGLE:
                selection = selection.add(querySingle(uri.getLastPathSegment()));
                break;

            case CAN_UPDATE:
                selection = selection.add(queryCanUpdate());
                includeSwap = false;
                break;

            case REPO:
                selection = selection.add(queryRepo(Long.parseLong(uri.getLastPathSegment())));
                break;

            case INSTALLED:
                selection = selection.add(queryInstalled());
                includeSwap = false;
                break;

            case SEARCH:
                selection = selection.add(querySearch(uri.getLastPathSegment()));
                includeSwap = false;
                break;

            case SEARCH_INSTALLED:
                selection = querySearch(uri.getLastPathSegment()).add(queryInstalled());
                includeSwap = false;
                break;

            case SEARCH_CAN_UPDATE:
                selection = querySearch(uri.getLastPathSegment()).add(queryCanUpdate());
                includeSwap = false;
                break;

            case SEARCH_REPO:
                selection = selection.add(querySearch(uri.getPathSegments().get(2)));
                selection = selection.add(queryRepo(Long.parseLong(uri.getPathSegments().get(1))));
                break;

            case NO_APKS:
                selection = selection.add(queryNoApks());
                break;

            case APPS:
                selection = selection.add(queryApps(uri.getLastPathSegment()));
                break;

            case IGNORED:
                selection = selection.add(queryIgnored());
                break;

            case CATEGORY:
                selection = selection.add(queryCategory(uri.getLastPathSegment()));
                includeSwap = false;
                break;

            case RECENTLY_UPDATED:
                sortOrder = getTableName() + ".lastUpdated DESC";
                selection = selection.add(queryRecentlyUpdated());
                includeSwap = false;
                break;

            case NEWLY_ADDED:
                sortOrder = getTableName() + ".added DESC";
                selection = selection.add(queryNewlyAdded());
                includeSwap = false;
                break;

            default:
                Log.e(TAG, "Invalid URI for app content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for app content provider: " + uri);
        }

        if (!includeSwap) {
            selection = selection.add(queryExcludeSwap());
        }

        if (AppProvider.DataColumns.NAME.equals(sortOrder)) {
            sortOrder = getTableName() + "." + sortOrder + " COLLATE LOCALIZED ";
        }

        Query query = new Query();

        query.addSelection(selection);
        query.addFields(projection); // TODO: Make the order of addFields/addSelection not dependent on each other...
        query.addOrderBy(sortOrder);

        Cursor cursor = read().rawQuery(query.toString(), selection.getArgs());
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
                throw new UnsupportedOperationException("Delete not supported for " + uri + ".");

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
        return getContentUri(values.getAsString(DataColumns.PACKAGE_NAME));
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        QuerySelection query = new QuerySelection(where, whereArgs);
        switch (matcher.match(uri)) {

            case CALC_APP_DETAILS_FROM_INDEX:
                updateAppDetails();
                return 0;

            case CODE_SINGLE:
                query = query.add(querySingle(uri.getLastPathSegment()));
                break;

            default:
                throw new UnsupportedOperationException("Update not supported for " + uri + ".");

        }
        int count = write().update(getTableName(), values, query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    protected void updateAppDetails() {
        updateCompatibleFlags();
        updateSuggestedFromUpstream();
        updateSuggestedFromLatest();
        updateIconUrls(getContext(), write(), getTableName(), getApkTableName());
    }

    /**
     * For each app, we want to set the isCompatible flag to 1 if any of the apks we know
     * about are compatible, and 0 otherwise.
     *
     * Readable SQL code:
     *
     *  UPDATE fdroid_app SET compatible = (
     *      SELECT TOTAL( fdroid_apk.compatible ) > 0
     *      FROM fdroid_apk
     *      WHERE fdroid_apk.id = fdroid_app.id );
     */
    private void updateCompatibleFlags() {

        Utils.debugLog(TAG, "Calculating whether apps are compatible, based on whether any of their apks are compatible");

        final String apk = getApkTableName();
        final String app = getTableName();

        String updateSql =
                "UPDATE " + app + " SET compatible = ( " +
                " SELECT TOTAL( " + apk + ".compatible ) > 0 " +
                " FROM " + apk +
                " WHERE " + apk + ".id = " + app + ".id );";

        write().execSQL(updateSql);
    }

    /**
     * Look at the upstream version of each app, our goal is to find the apk
     * with the closest version code to that, without going over.
     * If the app is not compatible at all (i.e. no versions were compatible)
     * then we take the highest, otherwise we take the highest compatible version.
     *
     * Readable SQL code:
     *
     *   UPDATE fdroid_app
     *   SET suggestedVercode = (
     *       SELECT MAX(fdroid_apk.vercode)
     *       FROM fdroid_apk
     *       WHERE
     *           fdroid_app.id = fdroid_apk.id AND
     *           fdroid_apk.vercode <= fdroid_app.upstreamVercode AND
     *           ( fdroid_app.compatible = 0 OR fdroid_apk.compatible = 1 )
     *   )
     *   WHERE upstreamVercode > 0
     */
    private void updateSuggestedFromUpstream() {

        Utils.debugLog(TAG, "Calculating suggested versions for all apps which specify an upstream version code.");

        final String apk = getApkTableName();
        final String app = getTableName();

        final boolean unstableUpdates = Preferences.get().getUnstableUpdates();
        String restrictToStable = unstableUpdates ? "" : (apk + ".vercode <= " + app + ".upstreamVercode AND ");
        String updateSql =
                "UPDATE " + app + " SET suggestedVercode = ( " +
                " SELECT MAX( " + apk + ".vercode ) " +
                " FROM " + apk +
                " WHERE " +
                    app + ".id = " + apk + ".id AND " +
                    restrictToStable +
                    " ( " + app + ".compatible = 0 OR " + apk + ".compatible = 1 ) ) " +
                " WHERE upstreamVercode > 0 ";

        write().execSQL(updateSql);
    }

    /**
     * We set each app's suggested version to the latest available that is
     * compatible, or the latest available if none are compatible.
     *
     * If the suggested version is null, it means that we could not figure it
     * out from the upstream vercode. In such a case, fall back to the simpler
     * algorithm as if upstreamVercode was 0.
     *
     * Readable SQL code:
     *
     *  UPDATE fdroid_app SET suggestedVercode = (
     *      SELECT MAX(fdroid_apk.vercode)
     *      FROM fdroid_apk
     *      WHERE
     *          fdroid_app.id = fdroid_apk.id AND
     *          ( fdroid_app.compatible = 0 OR fdroid_apk.compatible = 1 )
     *  )
     *  WHERE upstreamVercode = 0 OR upstreamVercode IS NULL OR suggestedVercode IS NULL;
     */
    private void updateSuggestedFromLatest() {

        Utils.debugLog(TAG, "Calculating suggested versions for all apps which don't specify an upstream version code.");

        final String apk = getApkTableName();
        final String app = getTableName();

        String updateSql =
                "UPDATE " + app + " SET suggestedVercode = ( " +
                " SELECT MAX( " + apk + ".vercode ) " +
                " FROM " + apk +
                " WHERE " +
                    app + ".id = " + apk + ".id AND " +
                    " ( " + app + ".compatible = 0 OR " + apk + ".compatible = 1 ) ) " +
                " WHERE upstreamVercode = 0 OR upstreamVercode IS NULL OR suggestedVercode IS NULL ";

        write().execSQL(updateSql);
    }

    /**
     * Made static so that the {@link org.fdroid.fdroid.data.AppProvider.UpgradeHelper} can access
     * it without instantiating an {@link AppProvider}. This is also the reason it needs to accept
     * the context and database as arguments.
     */
    private static void updateIconUrls(Context context, SQLiteDatabase db, String appTable, String apkTable) {
        final String iconsDir = Utils.getIconsDir(context, 1.0);
        final String iconsDirLarge = Utils.getIconsDir(context, 1.5);
        String repoVersion = Integer.toString(Repo.VERSION_DENSITY_SPECIFIC_ICONS);
        Utils.debugLog(TAG, "Updating icon paths for apps belonging to repos with version >= "
                + repoVersion);
        Utils.debugLog(TAG, "Using icons dir '" + iconsDir + "'");
        Utils.debugLog(TAG, "Using large icons dir '" + iconsDirLarge + "'");
        String query = getIconUpdateQuery(appTable, apkTable);
        final String[] params = {
            repoVersion, iconsDir, Utils.FALLBACK_ICONS_DIR,
            repoVersion, iconsDirLarge, Utils.FALLBACK_ICONS_DIR,
        };
        db.execSQL(query, params);
    }

    /**
     * Returns a query which requires two parameters to be bound. These are (in order):
     *  1) The repo version that introduced density specific icons
     *  2) The dir to density specific icons for the current device.
     */
    private static String getIconUpdateQuery(String app, String apk) {

        final String repo = DBHelper.TABLE_REPO;

        final String iconUrlQuery =
                " SELECT " +

                // Concatenate (using the "||" operator) the address, the
                // icons directory (bound to the ? as the second parameter
                // when executing the query) and the icon path.
                " ( " +
                    repo + ".address " +
                    " || " +

                    // If the repo has the relevant version, then use a more
                    // intelligent icons dir, otherwise revert to the default
                    // one
                    " CASE WHEN " + repo + ".version >= ? THEN ? ELSE ? END " +

                    " || " +
                    app + ".icon " +
                ") " +
                " FROM " +
                apk +
                " JOIN " + repo + " ON (" + repo + "._id = " + apk + ".repo) " +
                " WHERE " +
                app + ".id = " + apk + ".id AND " +
                apk + ".vercode = ( " +

                    // We only want the latest apk here. Ideally, we should
                    // instead join onto apk.suggestedVercode, but as per
                    // https://gitlab.com/fdroid/fdroidclient/issues/1 there
                    // may be some situations where suggestedVercode isn't
                    // set.
                    // TODO: If we can guarantee that suggestedVercode is set,
                    // then join onto that instead. This will save from doing
                    // a futher sub query for each app.
                    " SELECT MAX(inner_apk.vercode)  " +
                    " FROM " + apk + " as inner_apk " +
                    " WHERE inner_apk.id = " + apk + ".id ) " +
                " AND " + apk + ".repo = fdroid_repo._id ";

        return
            " UPDATE " + app + " SET " +
            " iconUrl = ( " +
                iconUrlQuery +
            " ), " +
            " iconUrlLarge = ( " +
                iconUrlQuery +
            " ) ";
    }

}
