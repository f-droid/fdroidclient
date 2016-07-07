package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.AppTable;
import org.fdroid.fdroid.data.Schema.AppTable.Cols;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.fdroid.fdroid.data.Schema.RepoTable;

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
            final String[] projection = {Cols._COUNT};
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
            return all(resolver, Cols.ALL);
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
            final String[] projection = {Cols.CATEGORIES};
            final Cursor cursor = resolver.query(uri, projection, null, null, null);
            final Set<String> categorySet = new HashSet<>();
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        final String categoriesString = cursor.getString(0);
                        String[] categoriesList = Utils.parseCommaSeparatedString(categoriesString);
                        if (categoriesList != null) {
                            Collections.addAll(categorySet, categoriesList);
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
            return findByPackageName(resolver, packageName, Cols.ALL);
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
            AppProvider.updateIconUrls(context, db, AppTable.NAME, ApkTable.NAME);
        }

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
            final String repo = RepoTable.NAME;

            return app +
                " LEFT JOIN " + apk + " ON (" + apk + "." + ApkTable.Cols.PACKAGE_NAME + " = " + app + "." + Cols.PACKAGE_NAME + ") " +
                " LEFT JOIN " + repo + " ON (" + apk + "." + ApkTable.Cols.REPO_ID + " = " + repo + "." + RepoTable.Cols._ID + ") ";
        }

        @Override
        protected boolean isDistinct() {
            return fieldCount() == 1 && categoryFieldAdded;
        }

        @Override
        protected String groupBy() {
            // If the count field has been requested, then we want to group all rows together.
            return countFieldAppended ? null : getTableName() + "." + Cols.PACKAGE_NAME;
        }

        public void addSelection(AppQuerySelection selection) {
            super.addSelection(selection);
            if (selection.naturalJoinToInstalled()) {
                naturalJoinToInstalledTable();
            }
        }

        // TODO: What if the selection requires a natural join, but we first get a left join
        // because something causes leftJoin to be caused first? Maybe throw an exception?
        public void naturalJoinToInstalledTable() {
            if (!requiresInstalledTable) {
                join(
                        InstalledAppTable.NAME,
                        "installed",
                        "installed." + InstalledAppTable.Cols.PACKAGE_NAME + " = " + getTableName() + "." + Cols.PACKAGE_NAME);
                requiresInstalledTable = true;
            }
        }

        public void leftJoinToInstalledTable() {
            if (!requiresInstalledTable) {
                leftJoin(
                        InstalledAppTable.NAME,
                        "installed",
                        "installed." + InstalledAppTable.Cols.PACKAGE_NAME + " = " + getTableName() + "." + Cols.PACKAGE_NAME);
                requiresInstalledTable = true;
            }
        }

        @Override
        public void addField(String field) {
            switch (field) {
                case Cols.SuggestedApk.VERSION_NAME:
                    addSuggestedApkVersionField();
                    break;
                case Cols.InstalledApp.VERSION_NAME:
                    addInstalledAppVersionName();
                    break;
                case Cols.InstalledApp.VERSION_CODE:
                    addInstalledAppVersionCode();
                    break;
                case Cols.InstalledApp.SIGNATURE:
                    addInstalledSig();
                    break;
                case Cols._COUNT:
                    appendCountField();
                    break;
                default:
                    if (field.equals(Cols.CATEGORIES)) {
                        categoryFieldAdded = true;
                    }
                    appendField(field, getTableName());
                    break;
            }
        }

        private void appendCountField() {
            countFieldAppended = true;
            appendField("COUNT( DISTINCT " + getTableName() + "." + Cols.PACKAGE_NAME + " ) AS " + Cols._COUNT);
        }

        private void addSuggestedApkVersionField() {
            addSuggestedApkField(
                    ApkTable.Cols.VERSION_NAME,
                    Cols.SuggestedApk.VERSION_NAME);
        }

        private void addSuggestedApkField(String fieldName, String alias) {
            if (!isSuggestedApkTableAdded) {
                isSuggestedApkTableAdded = true;
                leftJoin(
                        getApkTableName(),
                        "suggestedApk",
                        getTableName() + "." + Cols.SUGGESTED_VERSION_CODE + " = suggestedApk." + ApkTable.Cols.VERSION_CODE + " AND " + getTableName() + "." + Cols.PACKAGE_NAME + " = suggestedApk." + ApkTable.Cols.PACKAGE_NAME);
            }
            appendField(fieldName, "suggestedApk", alias);
        }

        private void addInstalledAppVersionName() {
            addInstalledAppField(
                    InstalledAppTable.Cols.VERSION_NAME,
                    Cols.InstalledApp.VERSION_NAME
            );
        }

        private void addInstalledAppVersionCode() {
            addInstalledAppField(
                    InstalledAppTable.Cols.VERSION_CODE,
                    Cols.InstalledApp.VERSION_CODE
            );
        }

        private void addInstalledSig() {
            addInstalledAppField(
                    InstalledAppTable.Cols.SIGNATURE,
                    Cols.InstalledApp.SIGNATURE
            );
        }

        private void addInstalledAppField(String fieldName, String alias) {
            leftJoinToInstalledTable();
            appendField(fieldName, "installed", alias);
        }
    }

    private static final String PROVIDER_NAME = "AppProvider";

    private static final UriMatcher MATCHER = new UriMatcher(-1);

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
        MATCHER.addURI(getAuthority(), null, CODE_LIST);
        MATCHER.addURI(getAuthority(), PATH_CALC_APP_DETAILS_FROM_INDEX, CALC_APP_DETAILS_FROM_INDEX);
        MATCHER.addURI(getAuthority(), PATH_IGNORED, IGNORED);
        MATCHER.addURI(getAuthority(), PATH_RECENTLY_UPDATED, RECENTLY_UPDATED);
        MATCHER.addURI(getAuthority(), PATH_NEWLY_ADDED, NEWLY_ADDED);
        MATCHER.addURI(getAuthority(), PATH_CATEGORY + "/*", CATEGORY);
        MATCHER.addURI(getAuthority(), PATH_SEARCH + "/*", SEARCH);
        MATCHER.addURI(getAuthority(), PATH_SEARCH_INSTALLED + "/*", SEARCH_INSTALLED);
        MATCHER.addURI(getAuthority(), PATH_SEARCH_CAN_UPDATE + "/*", SEARCH_CAN_UPDATE);
        MATCHER.addURI(getAuthority(), PATH_SEARCH_REPO + "/*/*", SEARCH_REPO);
        MATCHER.addURI(getAuthority(), PATH_REPO + "/#", REPO);
        MATCHER.addURI(getAuthority(), PATH_CAN_UPDATE, CAN_UPDATE);
        MATCHER.addURI(getAuthority(), PATH_INSTALLED, INSTALLED);
        MATCHER.addURI(getAuthority(), PATH_NO_APKS, NO_APKS);
        MATCHER.addURI(getAuthority(), PATH_APPS + "/*", APPS);
        MATCHER.addURI(getAuthority(), "*", CODE_SINGLE);
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
        if (TextUtils.isEmpty(query)) {
            // Return all the things for an empty search.
            return getContentUri();
        }
        return getContentUri().buildUpon()
                .appendPath(PATH_SEARCH)
                .appendPath(query)
                .build();
    }

    public static Uri getSearchInstalledUri(String query) {
        return getContentUri()
            .buildUpon()
            .appendPath(PATH_SEARCH_INSTALLED)
            .appendPath(query)
            .build();
    }

    public static Uri getSearchCanUpdateUri(String query) {
        return getContentUri()
            .buildUpon()
            .appendPath(PATH_SEARCH_CAN_UPDATE)
            .appendPath(query)
            .build();
    }

    public static Uri getSearchUri(Repo repo, String query) {
        return getContentUri().buildUpon()
            .appendPath(PATH_SEARCH_REPO)
            .appendPath(String.valueOf(repo.id))
            .appendPath(query)
            .build();
    }

    @Override
    protected String getTableName() {
        return AppTable.NAME;
    }

    protected String getApkTableName() {
        return ApkTable.NAME;
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
        return MATCHER;
    }

    private AppQuerySelection queryCanUpdate() {
        final String app = getTableName();
        final String ignoreCurrent = app + "." + Cols.IGNORE_THISUPDATE + "!= " + app + "." + Cols.SUGGESTED_VERSION_CODE;
        final String ignoreAll = app + "." + Cols.IGNORE_ALLUPDATES + " != 1";
        final String ignore = " (" + ignoreCurrent + " AND " + ignoreAll + ") ";
        final String where = ignore + " AND " + app + "." + Cols.SUGGESTED_VERSION_CODE + " > installed." + InstalledAppTable.Cols.VERSION_CODE;
        return new AppQuerySelection(where).requireNaturalInstalledTable();
    }

    private AppQuerySelection queryRepo(long repoId) {
        final String selection = getApkTableName() + "." + ApkTable.Cols.REPO_ID + " = ? ";
        final String[] args = {String.valueOf(repoId)};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryInstalled() {
        return new AppQuerySelection().requireNaturalInstalledTable();
    }

    private AppQuerySelection querySearch(String query) {
        // Put in a Set to remove duplicates
        final Set<String> keywordSet = new HashSet<>(Arrays.asList(query.split("\\s")));

        if (keywordSet.size() == 0) {
            return new AppQuerySelection();
        }

        // Surround each keyword in % for wildcard searching
        final String[] keywords = new String[keywordSet.size()];
        int iKeyword = 0;
        for (final String keyword : keywordSet) {
            keywords[iKeyword] = "%" + keyword + "%";
            iKeyword++;
        }

        final String app = getTableName();
        final String[] columns = {
                app + "." + Cols.PACKAGE_NAME,
                app + "." + Cols.NAME,
                app + "." + Cols.SUMMARY,
                app + "." + Cols.DESCRIPTION,
        };

        // Build selection string and fill out keyword arguments
        final StringBuilder selection = new StringBuilder();
        final String[] selectionKeywords = new String[columns.length * keywords.length];
        iKeyword = 0;
        boolean firstColumn = true;
        for (final String column : columns) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                selection.append(" OR ");
            }
            selection.append('(');
            boolean firstKeyword = true;
            for (final String keyword : keywords) {
                if (firstKeyword) {
                    firstKeyword = false;
                } else {
                    selection.append(" AND ");
                }
                selection.append(column).append(" LIKE ?");
                selectionKeywords[iKeyword] = keyword;
                iKeyword++;
            }
            selection.append(") ");
        }
        return new AppQuerySelection(selection.toString(), selectionKeywords);
    }

    protected AppQuerySelection querySingle(String packageName) {
        final String selection = getTableName() + "." + Cols.PACKAGE_NAME + " = ?";
        final String[] args = {packageName};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryIgnored() {
        final String table = getTableName();
        final String selection = table + "." + Cols.IGNORE_ALLUPDATES + " = 1 OR " +
                table + "." + Cols.IGNORE_THISUPDATE + " >= " + table + "." + Cols.SUGGESTED_VERSION_CODE;
        return new AppQuerySelection(selection);
    }

    private AppQuerySelection queryExcludeSwap() {
        // fdroid_repo will have null fields if the LEFT JOIN didn't resolve, e.g. due to there
        // being no apks for the app in the result set. In that case, we can't tell if it is from
        // a swap repo or not.
        final String isSwap = RepoTable.NAME + "." + RepoTable.Cols.IS_SWAP;
        final String selection = isSwap + " = 0 OR " + isSwap + " IS NULL";
        return new AppQuerySelection(selection);
    }

    private AppQuerySelection queryNewlyAdded() {
        final String selection = getTableName() + "." + Cols.ADDED + " > ?";
        final String[] args = {Utils.formatDate(Preferences.get().calcMaxHistory(), "")};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryRecentlyUpdated() {
        final String app = getTableName();
        final String lastUpdated = app + "." + Cols.LAST_UPDATED;
        final String selection = app + "." + Cols.ADDED + " != " + lastUpdated + " AND " + lastUpdated + " > ?";
        final String[] args = {Utils.formatDate(Preferences.get().calcMaxHistory(), "")};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryCategory(String category) {
        // TODO: In the future, add a new table for categories,
        // so we can join onto it.
        final String app = getTableName();
        final String selection =
                app + "." + Cols.CATEGORIES + " = ? OR " +    // Only category e.g. "internet"
                app + "." + Cols.CATEGORIES + " LIKE ? OR " + // First category e.g. "internet,%"
                app + "." + Cols.CATEGORIES + " LIKE ? OR " + // Last category e.g. "%,internet"
                app + "." + Cols.CATEGORIES + " LIKE ? ";     // One of many categories e.g. "%,internet,%"
        final String[] args = {
            category,
            category + ",%",
            "%," + category,
            "%," + category + ",%",
        };
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryNoApks() {
        final String apk = getApkTableName();
        final String app = getTableName();
        String selection = "(SELECT COUNT(*) FROM " + apk + " WHERE " + apk + "." + ApkTable.Cols.PACKAGE_NAME + " = " + app + "." + Cols.PACKAGE_NAME + ") = 0";
        return new AppQuerySelection(selection);
    }

    static AppQuerySelection queryApps(String packageNames, String packageNameField) {
        String[] args = packageNames.split(",");
        String selection = packageNameField + " IN (" + generateQuestionMarksForInClause(args.length) + ")";
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryApps(String packageNames) {
        return queryApps(packageNames, getTableName() + "." + Cols.PACKAGE_NAME);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        AppQuerySelection selection = new AppQuerySelection(customSelection, selectionArgs);

        // Queries which are for the main list of apps should not include swap apps.
        boolean includeSwap = true;

        switch (MATCHER.match(uri)) {
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
                sortOrder = getTableName() + "." + Cols.LAST_UPDATED + " DESC";
                selection = selection.add(queryRecentlyUpdated());
                includeSwap = false;
                break;

            case NEWLY_ADDED:
                sortOrder = getTableName() + "." + Cols.ADDED + " DESC";
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

        if (Cols.NAME.equals(sortOrder)) {
            sortOrder = getTableName() + "." + sortOrder + " COLLATE LOCALIZED ";
        }

        Query query = new Query();
        query.addSelection(selection);
        query.addFields(projection); // TODO: Make the order of addFields/addSelection not dependent on each other...
        query.addOrderBy(sortOrder);

        Cursor cursor = LoggingQuery.query(db(), query.toString(), query.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        QuerySelection query = new QuerySelection(where, whereArgs);
        switch (MATCHER.match(uri)) {

            case NO_APKS:
                query = query.add(queryNoApks());
                break;

            default:
                throw new UnsupportedOperationException("Delete not supported for " + uri + ".");

        }

        int count = db().delete(getTableName(), query.getSelection(), query.getArgs());
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        db().insertOrThrow(getTableName(), null, values);
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return getContentUri(values.getAsString(Cols.PACKAGE_NAME));
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        QuerySelection query = new QuerySelection(where, whereArgs);
        switch (MATCHER.match(uri)) {

            case CALC_APP_DETAILS_FROM_INDEX:
                updateAppDetails();
                return 0;

            case CODE_SINGLE:
                query = query.add(querySingle(uri.getLastPathSegment()));
                break;

            default:
                throw new UnsupportedOperationException("Update not supported for " + uri + ".");

        }
        int count = db().update(getTableName(), values, query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    protected void updateAppDetails() {
        updateCompatibleFlags();
        updateSuggestedFromUpstream();
        updateSuggestedFromLatest();
        updateIconUrls(getContext(), db(), getTableName(), getApkTableName());
    }

    /**
     * For each app, we want to set the isCompatible flag to 1 if any of the apks we know
     * about are compatible, and 0 otherwise.
     */
    private void updateCompatibleFlags() {
        Utils.debugLog(TAG, "Calculating whether apps are compatible, based on whether any of their apks are compatible");

        final String apk = getApkTableName();
        final String app = getTableName();

        String updateSql =
                "UPDATE " + app + " SET " + Cols.IS_COMPATIBLE + " = ( " +
                " SELECT TOTAL( " + apk + "." + ApkTable.Cols.IS_COMPATIBLE + ") > 0 " +
                " FROM " + apk +
                " WHERE " + apk + "." + ApkTable.Cols.PACKAGE_NAME + " = " + app + "." + Cols.PACKAGE_NAME + " );";

        db().execSQL(updateSql);
    }

    /**
     * Look at the upstream version of each app, our goal is to find the apk
     * with the closest version code to that, without going over.
     * If the app is not compatible at all (i.e. no versions were compatible)
     * then we take the highest, otherwise we take the highest compatible version.
     */
    private void updateSuggestedFromUpstream() {
        Utils.debugLog(TAG, "Calculating suggested versions for all apps which specify an upstream version code.");

        final String apk = getApkTableName();
        final String app = getTableName();

        final boolean unstableUpdates = Preferences.get().getUnstableUpdates();
        String restrictToStable = unstableUpdates ? "" : (apk + "." + ApkTable.Cols.VERSION_CODE + " <= " + app + "." + Cols.UPSTREAM_VERSION_CODE + " AND ");
        String updateSql =
                "UPDATE " + app + " SET " + Cols.SUGGESTED_VERSION_CODE + " = ( " +
                " SELECT MAX( " + apk + "." + ApkTable.Cols.VERSION_CODE + " ) " +
                " FROM " + apk +
                " WHERE " +
                    app + "." + Cols.PACKAGE_NAME + " = " + apk + "." + ApkTable.Cols.PACKAGE_NAME + " AND " +
                    restrictToStable +
                    " ( " + app + "." + Cols.IS_COMPATIBLE + " = 0 OR " + apk + "." + Cols.IS_COMPATIBLE + " = 1 ) ) " +
                " WHERE " + Cols.UPSTREAM_VERSION_CODE + " > 0 ";

        db().execSQL(updateSql);
    }

    /**
     * We set each app's suggested version to the latest available that is
     * compatible, or the latest available if none are compatible.
     *
     * If the suggested version is null, it means that we could not figure it
     * out from the upstream vercode. In such a case, fall back to the simpler
     * algorithm as if upstreamVercode was 0.
     */
    private void updateSuggestedFromLatest() {
        Utils.debugLog(TAG, "Calculating suggested versions for all apps which don't specify an upstream version code.");

        final String apk = getApkTableName();
        final String app = getTableName();

        String updateSql =
                "UPDATE " + app + " SET " + Cols.SUGGESTED_VERSION_CODE + " = ( " +
                " SELECT MAX( " + apk + "." + ApkTable.Cols.VERSION_CODE + " ) " +
                " FROM " + apk +
                " WHERE " +
                    app + "." + Cols.PACKAGE_NAME + " = " + apk + "." + ApkTable.Cols.PACKAGE_NAME + " AND " +
                    " ( " + app + "." + Cols.IS_COMPATIBLE + " = 0 OR " + apk + "." + ApkTable.Cols.IS_COMPATIBLE + " = 1 ) ) " +
                " WHERE " + Cols.UPSTREAM_VERSION_CODE + " = 0 OR " + Cols.UPSTREAM_VERSION_CODE + " IS NULL OR " + Cols.SUGGESTED_VERSION_CODE + " IS NULL ";

        db().execSQL(updateSql);
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
        Utils.debugLog(TAG, "Updating icon paths for apps belonging to repos with version >= " + repoVersion);
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

        final String repo = RepoTable.NAME;

        final String iconUrlQuery =
                "SELECT " +

                // Concatenate (using the "||" operator) the address, the
                // icons directory (bound to the ? as the second parameter
                // when executing the query) and the icon path.
                "( " +
                    repo + "." + RepoTable.Cols.ADDRESS +
                    " || " +

                    // If the repo has the relevant version, then use a more
                    // intelligent icons dir, otherwise revert to the default
                    // one
                    " CASE WHEN " + repo + "." + RepoTable.Cols.VERSION + " >= ? THEN ? ELSE ? END " +

                    " || " +
                    app + "." + Cols.ICON +
                ") " +
                " FROM " +
                apk +
                " JOIN " + repo + " ON (" + repo + "." + RepoTable.Cols._ID + " = " + apk + "." + ApkTable.Cols.REPO_ID + ") " +
                " WHERE " +
                app + "." + Cols.PACKAGE_NAME + " = " + apk + "." + ApkTable.Cols.PACKAGE_NAME + " AND " +
                apk + "." + ApkTable.Cols.VERSION_CODE + " = " + app + "." + Cols.SUGGESTED_VERSION_CODE;

        return "UPDATE " + app + " SET "
            + Cols.ICON_URL + " = ( " + iconUrlQuery + " ), "
            + Cols.ICON_URL_LARGE + " = ( " + iconUrlQuery + " )";
    }

}
