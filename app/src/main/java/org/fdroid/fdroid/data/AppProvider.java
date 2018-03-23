package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.ApkAntiFeatureJoinTable;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.fdroid.fdroid.data.Schema.AppPrefsTable;
import org.fdroid.fdroid.data.Schema.CatJoinTable;
import org.fdroid.fdroid.data.Schema.CategoryTable;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.fdroid.fdroid.data.Schema.PackageTable;
import org.fdroid.fdroid.data.Schema.RepoTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Each app has a bunch of metadata that it associates with a package name (such as org.fdroid.fdroid).
 * Multiple repositories can host the same package, and provide different metadata for that app.
 *
 * As such, it is usually the case that you are interested in an {@link App} which has its metadata
 * provided by "the repo with the best priority", rather than "specific repo X". This is important
 * when asking for an apk, whereby the preferable way is likely using:
 *
 *  * {@link AppProvider.Helper#findHighestPriorityMetadata(ContentResolver, String)}
 *
 * rather than:
 *
 *  * {@link AppProvider.Helper#findSpecificApp(ContentResolver, String, long, String[])}
 *
 * The same can be said of retrieving a list of {@link App} objects, where the metadata for each app
 * in the result set should be populated from the repository with the best priority.
 */
@SuppressWarnings("LineLength")
public class AppProvider extends FDroidProvider {

    private static final String TAG = "AppProvider";

    public static final class Helper {

        private Helper() { }

        public static List<App> all(ContentResolver resolver) {
            return all(resolver, Cols.ALL);
        }

        public static List<App> all(ContentResolver resolver, String[] projection) {
            final Uri uri = AppProvider.getContentUri();
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            return cursorToList(cursor);
        }

        static List<App> cursorToList(Cursor cursor) {
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

        public static App findHighestPriorityMetadata(ContentResolver resolver, String packageName, String[] cols) {
            final Uri uri = getHighestPriorityMetadataUri(packageName);
            return cursorToApp(resolver.query(uri, cols, null, null, null));
        }

        public static App findHighestPriorityMetadata(ContentResolver resolver, String packageName) {
            return findHighestPriorityMetadata(resolver, packageName, Cols.ALL);
        }

        /**
         * Returns an {@link App} with metadata provided by a specific {@code repoId}. Keep in mind
         * that most of the time we don't care which repo provides the metadata for a particular app,
         * as long as it is the repo with the best priority. In those cases, you should instead use
         * {@link AppProvider.Helper#findHighestPriorityMetadata(ContentResolver, String)}.
         */
        public static App findSpecificApp(ContentResolver resolver, String packageName, long repoId,
                                          String[] projection) {
            final Uri uri = getSpecificAppUri(packageName, repoId);
            return cursorToApp(resolver.query(uri, projection, null, null, null));
        }

        public static App findSpecificApp(ContentResolver resolver, String packageName, long repoId) {
            return findSpecificApp(resolver, packageName, repoId, Cols.ALL);
        }

        private static App cursorToApp(Cursor cursor) {
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

        public static void calcSuggestedApk(Context context, String packageName) {
            Uri uri = Uri.withAppendedPath(calcSuggestedApksUri(), packageName);
            context.getContentResolver().update(uri, null, null, null);
        }

        public static void calcSuggestedApks(Context context) {
            context.getContentResolver().update(calcSuggestedApksUri(), null, null, null);
        }

        public static List<App> findCanUpdate(Context context, String[] projection) {
            return cursorToList(context.getContentResolver().query(AppProvider.getCanUpdateUri(), projection, null, null, null));
        }

        public static void recalculatePreferredMetadata(Context context) {
            Uri uri = Uri.withAppendedPath(AppProvider.getContentUri(), PATH_CALC_PREFERRED_METADATA);
            context.getContentResolver().query(uri, null, null, null, null);
        }

        public static List<App> findInstalledAppsWithKnownVulns(Context context) {
            Uri uri = getInstalledWithKnownVulnsUri();
            Cursor cursor = context.getContentResolver().query(uri, Cols.ALL, null, null, null);
            return cursorToList(cursor);
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
    protected static class AppQuerySelection extends QuerySelection {

        private boolean naturalJoinToInstalled;
        private boolean naturalJoinApks;
        private boolean naturalJoinAntiFeatures;
        private boolean leftJoinPrefs;

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

        public boolean naturalJoinToInstalled() {
            return naturalJoinToInstalled;
        }

        public boolean naturalJoinToApks() {
            return naturalJoinApks;
        }

        public boolean naturalJoinAntiFeatures() {
            return naturalJoinAntiFeatures;
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

        /**
         * Note that this has large performance implications, so should only be used if you are already limiting
         * the result set based on other, more drastic conditions first.
         * See https://gitlab.com/fdroid/fdroidclient/issues/1143 for the investigation which identified these
         * performance implications.
         */
        public AppQuerySelection requireNaturalJoinApks() {
            naturalJoinApks = true;
            return this;
        }

        public AppQuerySelection requireNatrualJoinAntiFeatures() {
            naturalJoinAntiFeatures = true;
            return this;
        }

        public boolean leftJoinToPrefs() {
            return leftJoinPrefs;
        }

        public AppQuerySelection requireLeftJoinPrefs() {
            leftJoinPrefs = true;
            return this;
        }

        public AppQuerySelection add(AppQuerySelection query) {
            QuerySelection both = super.add(query);
            AppQuerySelection bothWithJoin = new AppQuerySelection(both.getSelection(), both.getArgs());
            if (this.naturalJoinToInstalled() || query.naturalJoinToInstalled()) {
                bothWithJoin.requireNaturalInstalledTable();
            }

            if (this.naturalJoinToApks() || query.naturalJoinToApks()) {
                bothWithJoin.requireNaturalJoinApks();
            }

            if (this.leftJoinToPrefs() || query.leftJoinToPrefs()) {
                bothWithJoin.requireLeftJoinPrefs();
            }

            if (this.naturalJoinAntiFeatures() || query.naturalJoinAntiFeatures()) {
                bothWithJoin.requireNatrualJoinAntiFeatures();
            }

            return bothWithJoin;
        }

    }

    protected class Query extends QueryBuilder {

        private boolean isSuggestedApkTableAdded;
        private boolean requiresInstalledTable;
        private boolean requiresApkTable;
        private boolean requiresAntiFeatures;
        private boolean requiresLeftJoinToPrefs;
        private boolean countFieldAppended;

        @Override
        protected String getRequiredTables() {
            final String pkg  = PackageTable.NAME;
            final String app  = getTableName();
            final String repo = RepoTable.NAME;
            final String cat  = CategoryTable.NAME;
            final String catJoin = getCatJoinTableName();

            return pkg +
                " JOIN " + app + " ON (" + app + "." + Cols.PACKAGE_ID + " = " + pkg + "." + PackageTable.Cols.ROW_ID + ") " +
                " JOIN " + repo + " ON (" + app + "." + Cols.REPO_ID + " = " + repo + "." + RepoTable.Cols._ID + ") " +
                " LEFT JOIN " + catJoin + " ON (" + app + "." + Cols.ROW_ID + " = " + catJoin + "." + CatJoinTable.Cols.APP_METADATA_ID + ") " +
                " LEFT JOIN " + cat + " ON (" + cat + "." + CategoryTable.Cols.ROW_ID + " = " + catJoin + "." + CatJoinTable.Cols.CATEGORY_ID + ") ";
        }

        @Override
        protected String groupBy() {
            // If the count field has been requested, then we want to group all rows together. Otherwise
            // we will only group all the rows belonging to a single app together.
            return countFieldAppended ? null : getTableName() + "." + Cols.ROW_ID;
        }

        public void addSelection(AppQuerySelection selection) {
            super.addSelection(selection);
            if (selection.naturalJoinToInstalled()) {
                naturalJoinToInstalledTable();
            }
            if (selection.naturalJoinToApks()) {
                naturalJoinToApkTable();
            }
            if (selection.leftJoinToPrefs()) {
                leftJoinToPrefs();
            }
            if (selection.naturalJoinAntiFeatures()) {
                naturalJoinAntiFeatures();
            }
        }

        // TODO: What if the selection requires a natural join, but we first get a left join
        // because something causes leftJoin to be caused first? Maybe throw an exception?
        public void naturalJoinToInstalledTable() {
            if (!requiresInstalledTable) {
                join(
                        InstalledAppTable.NAME,
                        "installed",
                        "installed." + InstalledAppTable.Cols.PACKAGE_ID + " = " + PackageTable.NAME + "." + PackageTable.Cols.ROW_ID);
                requiresInstalledTable = true;
            }
        }

        public void naturalJoinToApkTable() {
            if (!requiresApkTable) {
                join(
                        getApkTableName(),
                        getApkTableName(),
                        getApkTableName() + "." + ApkTable.Cols.APP_ID + " = " + getTableName() + "." + Cols.ROW_ID
                );
                requiresApkTable = true;
            }
        }

        public void leftJoinToPrefs() {
            if (!requiresLeftJoinToPrefs) {
                leftJoin(
                        AppPrefsTable.NAME,
                        "prefs",
                        "prefs." + AppPrefsTable.Cols.PACKAGE_NAME + " = " + PackageTable.NAME + "." + PackageTable.Cols.PACKAGE_NAME);
                requiresLeftJoinToPrefs = true;
            }
        }

        public void leftJoinToInstalledTable() {
            if (!requiresInstalledTable) {
                leftJoin(
                        InstalledAppTable.NAME,
                        "installed",
                        "installed." + InstalledAppTable.Cols.PACKAGE_ID + " = " + PackageTable.NAME + "." + PackageTable.Cols.ROW_ID);
                requiresInstalledTable = true;
            }
        }

        public void naturalJoinAntiFeatures() {
            if (!requiresAntiFeatures) {
                join(
                        getApkAntiFeatureJoinTableName(),
                        "apkAntiFeature",
                        "apkAntiFeature." + ApkAntiFeatureJoinTable.Cols.APK_ID + " = " + getApkTableName() + "." + ApkTable.Cols.ROW_ID);

                join(
                        Schema.AntiFeatureTable.NAME,
                        "antiFeature",
                        "antiFeature." + Schema.AntiFeatureTable.Cols.ROW_ID + " = " + "apkAntiFeature." + ApkAntiFeatureJoinTable.Cols.ANTI_FEATURE_ID);

                requiresAntiFeatures = true;
            }
        }

        @Override
        public void addField(String field) {
            switch (field) {
                case Cols.Package.PACKAGE_NAME:
                    appendField(PackageTable.Cols.PACKAGE_NAME, PackageTable.NAME, Cols.Package.PACKAGE_NAME);
                    break;
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
                    appendField(field, getTableName());
                    break;
            }
        }

        private void appendCountField() {
            countFieldAppended = true;
            appendField("COUNT( DISTINCT " + getTableName() + "." + Cols.ROW_ID + " ) AS " + Cols._COUNT);
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
                        getTableName() + "." + Cols.SUGGESTED_VERSION_CODE + " = suggestedApk." + ApkTable.Cols.VERSION_CODE + " AND " + getTableName() + "." + Cols.ROW_ID + " = suggestedApk." + ApkTable.Cols.APP_ID);
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
    private static final String PATH_SEARCH_REPO = "searchRepo";
    protected static final String PATH_APPS = "apps";
    protected static final String PATH_SPECIFIC_APP = "app";
    private static final String PATH_RECENTLY_UPDATED = "recentlyUpdated";
    private static final String PATH_CATEGORY = "category";
    private static final String PATH_REPO = "repo";
    private static final String PATH_HIGHEST_PRIORITY = "highestPriority";
    private static final String PATH_CALC_PREFERRED_METADATA = "calcPreferredMetadata";
    private static final String PATH_CALC_SUGGESTED_APKS = "calcNonRepoDetailsFromIndex";
    private static final String PATH_TOP_FROM_CATEGORY = "topFromCategory";
    private static final String PATH_INSTALLED_WITH_KNOWN_VULNS = "installedWithKnownVulns";

    private static final int CAN_UPDATE = CODE_SINGLE + 1;
    private static final int INSTALLED = CAN_UPDATE + 1;
    private static final int SEARCH_TEXT = INSTALLED + 1;
    private static final int SEARCH_TEXT_AND_CATEGORIES = SEARCH_TEXT + 1;
    private static final int RECENTLY_UPDATED = SEARCH_TEXT_AND_CATEGORIES + 1;
    private static final int CATEGORY = RECENTLY_UPDATED + 1;
    private static final int CALC_SUGGESTED_APKS = CATEGORY + 1;
    private static final int REPO = CALC_SUGGESTED_APKS + 1;
    private static final int SEARCH_REPO = REPO + 1;
    private static final int HIGHEST_PRIORITY = SEARCH_REPO + 1;
    private static final int CALC_PREFERRED_METADATA = HIGHEST_PRIORITY + 1;
    private static final int TOP_FROM_CATEGORY = CALC_PREFERRED_METADATA + 1;
    private static final int INSTALLED_WITH_KNOWN_VULNS = TOP_FROM_CATEGORY + 1;

    static {
        MATCHER.addURI(getAuthority(), null, CODE_LIST);
        MATCHER.addURI(getAuthority(), PATH_CALC_SUGGESTED_APKS, CALC_SUGGESTED_APKS);
        MATCHER.addURI(getAuthority(), PATH_CALC_SUGGESTED_APKS + "/*", CALC_SUGGESTED_APKS);
        MATCHER.addURI(getAuthority(), PATH_RECENTLY_UPDATED, RECENTLY_UPDATED);
        MATCHER.addURI(getAuthority(), PATH_CATEGORY + "/*", CATEGORY);
        MATCHER.addURI(getAuthority(), PATH_SEARCH + "/*/*", SEARCH_TEXT_AND_CATEGORIES);
        MATCHER.addURI(getAuthority(), PATH_SEARCH + "/*", SEARCH_TEXT);
        MATCHER.addURI(getAuthority(), PATH_SEARCH_REPO + "/*/*", SEARCH_REPO);
        MATCHER.addURI(getAuthority(), PATH_REPO + "/#", REPO);
        MATCHER.addURI(getAuthority(), PATH_CAN_UPDATE, CAN_UPDATE);
        MATCHER.addURI(getAuthority(), PATH_INSTALLED, INSTALLED);
        MATCHER.addURI(getAuthority(), PATH_HIGHEST_PRIORITY + "/*", HIGHEST_PRIORITY);
        MATCHER.addURI(getAuthority(), PATH_SPECIFIC_APP + "/#/*", CODE_SINGLE);
        MATCHER.addURI(getAuthority(), PATH_CALC_PREFERRED_METADATA, CALC_PREFERRED_METADATA);
        MATCHER.addURI(getAuthority(), PATH_TOP_FROM_CATEGORY + "/#/*", TOP_FROM_CATEGORY);
        MATCHER.addURI(getAuthority(), PATH_INSTALLED_WITH_KNOWN_VULNS, INSTALLED_WITH_KNOWN_VULNS);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getRecentlyUpdatedUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_RECENTLY_UPDATED);
    }

    private static Uri calcSuggestedApksUri() {
        return Uri.withAppendedPath(getContentUri(), PATH_CALC_SUGGESTED_APKS);
    }

    public static Uri getCategoryUri(String category) {
        return getContentUri().buildUpon()
                .appendPath(PATH_CATEGORY)
                .appendPath(category)
                .build();
    }

    public static Uri getInstalledWithKnownVulnsUri() {
        return getContentUri().buildUpon()
                .appendPath(PATH_INSTALLED_WITH_KNOWN_VULNS)
                .build();
    }

    public static Uri getTopFromCategoryUri(String category, int limit) {
        return getContentUri().buildUpon()
                .appendPath(PATH_TOP_FROM_CATEGORY)
                .appendPath(Integer.toString(limit))
                .appendPath(category)
                .build();
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

    /**
     * @see AppProvider.Helper#findSpecificApp(ContentResolver, String, long, String[]) for details
     * of why you should usually prefer {@link AppProvider#getHighestPriorityMetadataUri(String)} to
     * this method.
     */
    public static Uri getSpecificAppUri(String packageName, long repoId) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_SPECIFIC_APP)
                .appendPath(Long.toString(repoId))
                .appendPath(packageName)
                .build();
    }

    public static Uri getHighestPriorityMetadataUri(String packageName) {
        return getContentUri().buildUpon()
                .appendPath(PATH_HIGHEST_PRIORITY)
                .appendPath(packageName)
                .build();
    }

    public static Uri getSearchUri(String query, @Nullable String category) {
        if (TextUtils.isEmpty(query) && TextUtils.isEmpty(category)) {
            // Return all the things for an empty search.
            return getContentUri();
        } else if (TextUtils.isEmpty(query)) {
            return getCategoryUri(category);
        }

        Uri.Builder builder = getContentUri().buildUpon()
                .appendPath(PATH_SEARCH)
                .appendPath(query);

        if (!TextUtils.isEmpty(category)) {
            builder.appendPath(category);
        }

        return builder.build();
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
        return AppMetadataTable.NAME;
    }

    protected String getCatJoinTableName() {
        return CatJoinTable.NAME;
    }

    protected String getApkTableName() {
        return ApkTable.NAME;
    }

    protected String getApkAntiFeatureJoinTableName() {
        return ApkAntiFeatureJoinTable.NAME;
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

        // Need to use COALESCE because the prefs join may not resolve any rows, which means the
        // ignore* fields will be NULL. In that case, we want to instead use a default value of 0.
        final String ignoreCurrent = " COALESCE(prefs." + AppPrefsTable.Cols.IGNORE_THIS_UPDATE + ", 0) != " + app + "." + Cols.SUGGESTED_VERSION_CODE;
        final String ignoreAll = "COALESCE(prefs." + AppPrefsTable.Cols.IGNORE_ALL_UPDATES + ", 0) != 1";

        final String ignore = " (" + ignoreCurrent + " AND " + ignoreAll + ") ";
        final String where = ignore + " AND " + app + "." + Cols.SUGGESTED_VERSION_CODE + " > installed." + InstalledAppTable.Cols.VERSION_CODE;

        return new AppQuerySelection(where).requireNaturalInstalledTable().requireLeftJoinPrefs();
    }

    private AppQuerySelection queryRepo(long repoId) {
        final String selection = getTableName() + "." + Cols.REPO_ID + " = ? ";
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
                PackageTable.NAME + "." + PackageTable.Cols.PACKAGE_NAME,
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

    protected AppQuerySelection querySingle(String packageName, long repoId) {
        final String selection = getTableName() + "." + Cols.REPO_ID + " = ? ";
        final String[] args = {Long.toString(repoId)};
        return new AppQuerySelection(selection, args).add(queryPackageName(packageName));
    }

    /**
     * Same as {@link AppProvider#querySingle(String, long)} except it is used for the purpose
     * of an UPDATE query rather than a SELECT query. This means that it must use a subquery to get
     * the {@link Cols.Package#PACKAGE_ID} rather than the join which is already in place for that
     * table. The reason is because UPDATE queries cannot include joins in SQLite.
     */
    protected AppQuerySelection querySingleForUpdate(String packageName, long repoId) {
        final String selection = Cols.PACKAGE_ID + " = (" + getPackageIdFromPackageNameQuery() +
                ") AND " + Cols.REPO_ID + " = ? ";
        final String[] args = {packageName, Long.toString(repoId)};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryExcludeSwap() {
        // fdroid_repo will have null fields if the LEFT JOIN didn't resolve, e.g. due to there
        // being no apks for the app in the result set. In that case, we can't tell if it is from
        // a swap repo or not.
        final String isSwap = RepoTable.NAME + "." + RepoTable.Cols.IS_SWAP;
        final String selection = "COALESCE(" + isSwap + ", 0) = 0";
        return new AppQuerySelection(selection);
    }

    /**
     * Ensures that for each app metadata row with the same package name, only the one from the repo
     * with the best priority is represented in the result set. While possible to calculate this
     * dynamically each time the query is run, we precalculate it during repo updates for performance.
     */
    private AppQuerySelection queryHighestPriority() {
        final String selection = PackageTable.NAME + "." + PackageTable.Cols.PREFERRED_METADATA + " = " + getTableName() + "." + Cols.ROW_ID;
        return new AppQuerySelection(selection);
    }

    private AppQuerySelection queryPackageName(String packageName) {
        final String selection = PackageTable.NAME + "." + PackageTable.Cols.PACKAGE_NAME + " = ? ";
        final String[] args = {packageName};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryCategory(String category) {
        if (TextUtils.isEmpty(category)) {
            return new AppQuerySelection();
        }

        // Note, the COLLATE NOCASE only works for ASCII columns. The "ICU extension" for SQLite
        // provides proper case management for Unicode characters, but is not something provided
        // by Android.
        final String selection = CategoryTable.NAME + "." + CategoryTable.Cols.NAME + " = ? COLLATE NOCASE ";
        final String[] args = {category};
        return new AppQuerySelection(selection, args);
    }

    private AppQuerySelection queryInstalledWithKnownVulns() {
        String apk = getApkTableName();

        // Include the hash in this check because otherwise apps with any vulnerable version will
        // get returned, rather than just the installed version.
        String compareHash = apk + "." + ApkTable.Cols.HASH + " = installed." + InstalledAppTable.Cols.HASH;
        String knownVuln = " antiFeature." + Schema.AntiFeatureTable.Cols.NAME + " = 'KnownVuln' ";
        String notIgnored = " COALESCE(prefs." + AppPrefsTable.Cols.IGNORE_VULNERABILITIES + ", 0) = 0 ";

        String selection = knownVuln + " AND " + compareHash + " AND " + notIgnored;

        return new AppQuerySelection(selection)
                .requireNaturalInstalledTable()
                .requireNaturalJoinApks()
                .requireNatrualJoinAntiFeatures()
                .requireLeftJoinPrefs();
    }

    static AppQuerySelection queryPackageNames(String packageNames, String packageNameField) {
        String[] args = packageNames.split(",");
        String selection = packageNameField + " IN (" + generateQuestionMarksForInClause(args.length) + ")";
        return new AppQuerySelection(selection, args);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String customSelection, String[] selectionArgs, String sortOrder) {
        AppQuerySelection selection = new AppQuerySelection(customSelection, selectionArgs);

        // Queries which are for the main list of apps should not include swap apps.
        boolean includeSwap = true;

        // It is usually the case that we ask for app(s) for which we don't care what repo is
        // responsible for providing them. In that case, we need to populate the metadata with
        // that form the repo with the highest priority.
        // Whenever we know which repo it is coming from, then it is important that we don't
        // delegate to the repo with the highest priority, but rather the specific repo we are
        // querying from.
        boolean repoIsKnown = false;

        int limit = 0;

        List<String> pathSegments = uri.getPathSegments();
        switch (MATCHER.match(uri)) {
            case CALC_PREFERRED_METADATA:
                updatePreferredMetadata();
                return null;

            case CODE_LIST:
                includeSwap = false;
                break;

            case CODE_SINGLE:
                long repoId = Long.parseLong(pathSegments.get(1));
                String packageName = pathSegments.get(2);
                selection = selection.add(querySingle(packageName, repoId));
                repoIsKnown = true;
                break;

            case CAN_UPDATE:
                selection = selection.add(queryCanUpdate());
                includeSwap = false;
                break;

            case REPO:
                selection = selection.add(queryRepo(Long.parseLong(uri.getLastPathSegment())));
                repoIsKnown = true;
                break;

            case INSTALLED:
                selection = selection.add(queryInstalled());
                sortOrder = Cols.NAME;
                includeSwap = false;
                break;

            case SEARCH_TEXT:
                selection = selection.add(querySearch(pathSegments.get(1)));
                includeSwap = false;
                break;

            case SEARCH_TEXT_AND_CATEGORIES:
                selection = selection
                        .add(querySearch(pathSegments.get(1)))
                        .add(queryCategory(pathSegments.get(2)));
                includeSwap = false;
                break;

            case SEARCH_REPO:
                selection = selection
                        .add(querySearch(pathSegments.get(2)))
                        .add(queryRepo(Long.parseLong(pathSegments.get(1))));
                repoIsKnown = true;
                break;

            case CATEGORY:
                selection = selection.add(queryCategory(uri.getLastPathSegment()));
                includeSwap = false;
                break;

            case TOP_FROM_CATEGORY:
                selection = selection.add(queryCategory(pathSegments.get(2)));
                limit = Integer.parseInt(pathSegments.get(1));
                sortOrder = getTableName() + "." + Cols.LAST_UPDATED + " DESC";
                includeSwap = false;
                break;

            case INSTALLED_WITH_KNOWN_VULNS:
                selection = selection.add(queryInstalledWithKnownVulns());
                includeSwap = false;
                break;

            case RECENTLY_UPDATED:
                String table = getTableName();
                String isNew = table + "." + Cols.LAST_UPDATED + " <= " + table + "." + Cols.ADDED + " DESC";
                String hasFeatureGraphic = table + "." + Cols.FEATURE_GRAPHIC + " IS NULL ASC ";
                String lastUpdated = table + "." + Cols.LAST_UPDATED + " DESC";
                sortOrder = lastUpdated + ", " + isNew + ", " + hasFeatureGraphic;

                // There seems no reason to limit the number of apps on the front page, but it helps
                // if it loads quickly, as it is the default view shown every time F-Droid is opened.
                // 200 is an arbitrary number which hopefully gives the user enough to scroll through
                // if they are bored.
                limit = 200;

                includeSwap = false;
                break;

            case HIGHEST_PRIORITY:
                selection = selection.add(queryPackageName(uri.getLastPathSegment()));
                includeSwap = false;
                break;

            default:
                Log.e(TAG, "Invalid URI for app content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for app content provider: " + uri);
        }

        if (!repoIsKnown) {
            selection = selection.add(queryHighestPriority());
        }

        return runQuery(uri, selection, projection, includeSwap, sortOrder, limit);
    }

    /**
     * Helper method used by both the genuine {@link AppProvider} and the temporary version used
     * by the repo updater ({@link TempAppProvider}).
     */
    protected Cursor runQuery(Uri uri, AppQuerySelection selection, String[] projection, boolean includeSwap, String sortOrder, int limit) {
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
        query.addLimit(limit);

        Cursor cursor = LoggingQuery.query(db(), query.toString(), query.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if (MATCHER.match(uri) != REPO) {
            throw new UnsupportedOperationException("Delete not supported for " + uri + ".");
        }

        long repoId = Long.parseLong(uri.getLastPathSegment());

        final String catJoin = getCatJoinTableName();
        final String app = getTableName();
        String query = "DELETE FROM " + catJoin + " WHERE " + CatJoinTable.Cols.APP_METADATA_ID + " IN " +
                "(SELECT " + Cols.ROW_ID + " FROM " + app + " WHERE " + app + "." + Cols.REPO_ID + " = ?)";
        db().execSQL(query, new String[] {String.valueOf(repoId)});

        AppQuerySelection selection = new AppQuerySelection(where, whereArgs).add(queryRepo(repoId));
        int result = db().delete(getTableName(), selection.getSelection(), selection.getArgs());

        getContext().getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
        getContext().getContentResolver().notifyChange(AppProvider.getContentUri(), null);
        getContext().getContentResolver().notifyChange(CategoryProvider.getContentUri(), null);

        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long packageId = PackageProvider.Helper.ensureExists(getContext(), values.getAsString(Cols.Package.PACKAGE_NAME));
        values.remove(Cols.Package.PACKAGE_NAME);
        values.put(Cols.PACKAGE_ID, packageId);

        if (!values.containsKey(Cols.DESCRIPTION) || values.getAsString(Cols.DESCRIPTION) == null) {
            // the current structure assumes that description is always present and non-null
            values.put(Cols.DESCRIPTION, "");
        }

        // Trim these to avoid unwanted newlines in the UI
        values.put(Cols.SUMMARY, values.getAsString(Cols.SUMMARY).trim());
        values.put(Cols.NAME, values.getAsString(Cols.NAME).trim());

        String[] categories = null;
        boolean saveCategories = false;
        if (values.containsKey(Cols.ForWriting.Categories.CATEGORIES)) {
            // Hold onto these categories, so that after we have an ID to reference the newly inserted
            // app metadata we can then specify its categories.
            saveCategories = true;
            categories = Utils.parseCommaSeparatedString(values.getAsString(Cols.ForWriting.Categories.CATEGORIES));
            values.remove(Cols.ForWriting.Categories.CATEGORIES);
        }

        long appMetadataId = db().insertOrThrow(getTableName(), null, values);
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        if (saveCategories) {
            ensureCategories(categories, appMetadataId);
        }

        return getSpecificAppUri(values.getAsString(PackageTable.Cols.PACKAGE_NAME), values.getAsLong(Cols.REPO_ID));
    }

    protected void ensureCategories(String[] categories, long appMetadataId) {
        db().delete(getCatJoinTableName(), CatJoinTable.Cols.APP_METADATA_ID + " = ?", new String[] {Long.toString(appMetadataId)});
        if (categories != null) {
            Set<String> categoriesSet = new HashSet<>();
            for (String categoryName : categories) {

                // There is nothing stopping a server repeating a category name in the metadata of
                // an app. In order to prevent unique constraint violations, only insert once into
                // the join table.
                if (categoriesSet.contains(categoryName)) {
                    continue;
                }

                categoriesSet.add(categoryName);
                long categoryId = CategoryProvider.Helper.ensureExists(getContext(), categoryName);
                ContentValues categoryValues = new ContentValues(2);
                categoryValues.put(CatJoinTable.Cols.APP_METADATA_ID, appMetadataId);
                categoryValues.put(CatJoinTable.Cols.CATEGORY_ID, categoryId);
                db().insert(getCatJoinTableName(), null, categoryValues);
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if (MATCHER.match(uri) != CALC_SUGGESTED_APKS) {
            throw new UnsupportedOperationException("Update not supported for " + uri + ".");
        }

        List<String> segments = uri.getPathSegments();
        if (segments.size() > 1) {
            String packageName = segments.get(1);
            updateSuggestedApk(packageName);
        } else {
            updateSuggestedApks();
        }
        getContext().getContentResolver().notifyChange(getCanUpdateUri(), null);
        return 0;
    }

    protected void updateAllAppDetails() {
        updatePreferredMetadata();
        updateCompatibleFlags();
        updateSuggestedFromUpstream(null);
        updateSuggestedFromLatest(null);
        updateIconUrls();
    }

    /**
     * If the repo hasn't changed, then there are many things which we shouldn't waste time updating
     * (compared to {@link AppProvider#updateAllAppDetails()}:
     *
     * + The "preferred metadata", as that is calculated based on repo with highest priority, and
     *   only takes into account the package name, not specific versions, when figuring this out.
     *
     * + Compatible flags. These were calculated earlier, whether or not an app was suggested or not.
     *
     * + Icon URLs. While technically these do change when the suggested version changes, it is not
     *   important enough to spend a significant amount of time to calculate. In the future maybe,
     *   but that effort should instead go into implementing an intent service.
     *
     * In the future, this problem of taking a long time should be fixed by implementing an
     * {@link android.app.IntentService} as described in https://gitlab.com/fdroid/fdroidclient/issues/520.
     */
    protected void updateSuggestedApks() {
        updateSuggestedFromUpstream(null);
        updateSuggestedFromLatest(null);
    }

    protected void updateSuggestedApk(String packageName) {
        updateSuggestedFromUpstream(packageName);
        updateSuggestedFromLatest(packageName);
    }

    private void updatePreferredMetadata() {
        Utils.debugLog(TAG, "Deciding on which metadata should take priority for each package.");

        final String app = getTableName();

        final String highestPriority =
                "SELECT MAX(r." + RepoTable.Cols.PRIORITY + ") " +
                "FROM " + RepoTable.NAME + " AS r " +
                "JOIN " + getTableName() + " AS m ON (m." + Cols.REPO_ID + " = r." + RepoTable.Cols._ID + ") " +
                "WHERE m." + Cols.PACKAGE_ID + " = " + "metadata." + Cols.PACKAGE_ID;

        String updateSql =
                "UPDATE " + PackageTable.NAME + " " +
                "SET " + PackageTable.Cols.PREFERRED_METADATA + " = ( " +
                " SELECT metadata." + Cols.ROW_ID +
                " FROM " + app + " AS metadata " +
                " JOIN " + RepoTable.NAME + " AS repo ON (metadata." + Cols.REPO_ID + " = repo." + RepoTable.Cols._ID + ") " +
                " WHERE metadata." + Cols.PACKAGE_ID + " = " + PackageTable.NAME + "." + PackageTable.Cols.ROW_ID +
                " AND repo." + RepoTable.Cols.PRIORITY + " = (" + highestPriority + ")" +
                ");";

        db().execSQL(updateSql);
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
                " WHERE " + apk + "." + ApkTable.Cols.APP_ID + " = " + app + "." + Cols.ROW_ID + " );";

        db().execSQL(updateSql);
    }

    /**
     * Look at the upstream version of each app, our goal is to find the apk
     * with the closest version code to that, without going over.
     * If the app is not compatible at all (i.e. no versions were compatible)
     * then we take the highest, otherwise we take the highest compatible version.
     * If the app is installed, then all apks signed by a different certificate are
     * ignored for the purpose of this calculation.
     *
     * @see #updateSuggestedFromLatest(String)
     */
    private void updateSuggestedFromUpstream(@Nullable String packageName) {
        Utils.debugLog(TAG, "Calculating suggested versions for all NON-INSTALLED apps which specify an upstream version code.");

        final String apk = getApkTableName();
        final String app = getTableName();
        final String installed = InstalledAppTable.NAME;

        final boolean unstableUpdates = Preferences.get().getUnstableUpdates();
        String restrictToStable = unstableUpdates ? "" : (apk + "." + ApkTable.Cols.VERSION_CODE + " <= " + app + "." + Cols.UPSTREAM_VERSION_CODE + " AND ");

        String restrictToApp = "";
        String[] args = null;

        if (packageName != null) {
            restrictToApp = " AND " + app + "." + Cols.PACKAGE_ID + " = (" + getPackageIdFromPackageNameQuery() + ") ";
            args = new String[]{packageName};
        }

        // The join onto `appForThisApk` is to ensure that the MAX(apk.versionCode) is chosen from
        // all apps regardless of repo. If we joined directly onto the outer `app` table we are
        // in the process of updating, then it would be limited to only apks from the same repo.
        // By adding the extra join, and then joining based on the packageId of this inner app table
        // and the app table we are updating, we take into account all apks for this app.

        // The check apk.sig = COALESCE(installed.sig, apk.sig) would ideally be better written as:
        //   `installedSig IS NULL OR installedSig = apk.sig`
        // however that would require a separate sub query for each `installedSig` which is more
        // expensive. Using a COALESCE is a less expressive way to write the same thing with only
        // a single subquery.
        // Also note that the `installedSig IS NULL` is not because there is a `NULL` entry in the
        // installed table (this is impossible), but rather because the subselect above returned
        // zero rows.
        String updateSql =
                "UPDATE " + app + " SET " + Cols.SUGGESTED_VERSION_CODE + " = ( " +
                " SELECT MAX( " + apk + "." + ApkTable.Cols.VERSION_CODE + " ) " +
                " FROM " + apk +
                "   JOIN " + app + " AS appForThisApk ON (appForThisApk." + Cols.ROW_ID + " = " + apk + "." + ApkTable.Cols.APP_ID + ") " +
                        "   LEFT JOIN " + installed + " ON (" + installed + "." + InstalledAppTable.Cols.PACKAGE_ID + " = " + app + "." + Cols.PACKAGE_ID + ") " +
                " WHERE " +
                    app + "." + Cols.PACKAGE_ID + " = appForThisApk." + Cols.PACKAGE_ID + " AND " +
                    apk + "." + ApkTable.Cols.SIGNATURE + " IS COALESCE(" + installed + "." + InstalledAppTable.Cols.SIGNATURE + ", " + apk + "." + ApkTable.Cols.SIGNATURE + ") AND " +
                    restrictToStable +
                    " ( " + app + "." + Cols.IS_COMPATIBLE + " = 0 OR " + apk + "." + Cols.IS_COMPATIBLE + " = 1 ) ) " +
                " WHERE " + Cols.UPSTREAM_VERSION_CODE + " > 0 " + restrictToApp;

        LoggingQuery.execSQL(db(), updateSql, args);
    }

    /**
     * We set each app's suggested version to the latest available that is
     * compatible, or the latest available if none are compatible.
     *
     * If the suggested version is null, it means that we could not figure it
     * out from the upstream vercode. In such a case, fall back to the simpler
     * algorithm as if upstreamVercode was 0.
     *
     * @see #updateSuggestedFromUpstream(String)
     */
    private void updateSuggestedFromLatest(@Nullable String packageName) {
        Utils.debugLog(TAG, "Calculating suggested versions for all apps which don't specify an upstream version code.");

        final String apk = getApkTableName();
        final String app = getTableName();
        final String installed = InstalledAppTable.NAME;

        final String restrictToApps;
        final String[] args;

        if (packageName == null) {
            restrictToApps = " COALESCE(" + Cols.UPSTREAM_VERSION_CODE + ", 0) = 0 OR " + Cols.SUGGESTED_VERSION_CODE + " IS NULL ";
            args = null;
        } else {
            // Don't update an app with an upstream version code, because that would have been updated
            // by updateSuggestedFromUpdate(packageName).
            restrictToApps = " COALESCE(" + Cols.UPSTREAM_VERSION_CODE + ", 0) = 0 AND " + app + "." + Cols.PACKAGE_ID + " = (" + getPackageIdFromPackageNameQuery() + ") ";
            args = new String[]{packageName};
        }

        String updateSql =
                "UPDATE " + app + " SET " + Cols.SUGGESTED_VERSION_CODE + " = ( " +
                " SELECT MAX( " + apk + "." + ApkTable.Cols.VERSION_CODE + " ) " +
                " FROM " + apk +
                "   JOIN " + app + " AS appForThisApk ON (appForThisApk." + Cols.ROW_ID + " = " + apk + "." + ApkTable.Cols.APP_ID + ") " +
                "   LEFT JOIN " + installed + " ON (" + installed + "." + InstalledAppTable.Cols.PACKAGE_ID + " = " + app + "." + Cols.PACKAGE_ID + ") " +
                " WHERE " +
                    app + "." + Cols.PACKAGE_ID + " = appForThisApk." + Cols.PACKAGE_ID + " AND " +
                    apk + "." + ApkTable.Cols.SIGNATURE + " IS COALESCE(" + installed + "." + InstalledAppTable.Cols.SIGNATURE + ", " + apk + "." + ApkTable.Cols.SIGNATURE + ") AND " +
                    " ( " + app + "." + Cols.IS_COMPATIBLE + " = 0 OR " + apk + "." + ApkTable.Cols.IS_COMPATIBLE + " = 1 ) ) " +
                " WHERE " + restrictToApps;

        LoggingQuery.execSQL(db(), updateSql, args);
    }

    private void updateIconUrls() {
        final String appTable = getTableName();
        final String apkTable = getApkTableName();
        final String iconsDir = Utils.getIconsDir(getContext(), 1.0);
        String repoVersion = Integer.toString(Repo.VERSION_DENSITY_SPECIFIC_ICONS);
        Utils.debugLog(TAG, "Updating icon paths for apps belonging to repos with version >= " + repoVersion);
        Utils.debugLog(TAG, "Using icons dir '" + iconsDir + "'");
        String query = getIconUpdateQuery(appTable, apkTable);
        final String[] params = {
            repoVersion, iconsDir, Utils.FALLBACK_ICONS_DIR,
        };
        db().execSQL(query, params);
    }

    /**
     * Returns a query which requires two parameters to be bdeatound. These are (in order):
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
                app + "." + Cols.ROW_ID + " = " + apk + "." + ApkTable.Cols.APP_ID + " AND " +
                apk + "." + ApkTable.Cols.VERSION_CODE + " = " + app + "." + Cols.SUGGESTED_VERSION_CODE;

        return "UPDATE " + app + " SET "
            + Cols.ICON_URL + " = ( " + iconUrlQuery + " )";
    }

}
