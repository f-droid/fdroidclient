package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.AntiFeatureTable;
import org.fdroid.fdroid.data.Schema.ApkAntiFeatureJoinTable;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.ApkTable.Cols;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.PackageTable;
import org.fdroid.fdroid.data.Schema.RepoTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApkProvider extends FDroidProvider {

    private static final String TAG = "ApkProvider";

    /**
     * SQLite has a maximum of 999 parameters in a query. Each apk we add
     * requires two (packageName and vercode) so we can only query half of that. Then,
     * we may want to add additional constraints, so we give our self some
     * room by saying only 450 apks can be queried at once.
     */
    static final int MAX_APKS_TO_QUERY = 450;

    public static final class Helper {

        private Helper() {
        }

        public static void update(Context context, Apk apk) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getApkFromRepoUri(apk);
            resolver.update(uri, apk.toContentValues(), null, null);
        }

        public static Uri getApkFromRepoUri(Apk apk) {
            return getContentUri()
                    .buildUpon()
                    .appendPath(PATH_APK_FROM_REPO)
                    .appendPath(Long.toString(apk.appId))
                    .appendPath(Integer.toString(apk.versionCode))
                    .build();
        }

        public static List<Apk> cursorToList(Cursor cursor) {
            int knownApkCount = cursor != null ? cursor.getCount() : 0;
            List<Apk> apks = new ArrayList<>(knownApkCount);
            if (cursor != null) {
                if (knownApkCount > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        apks.add(new Apk(cursor));
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            return apks;
        }

        public static int deleteApksByRepo(Context context, Repo repo) {
            ContentResolver resolver = context.getContentResolver();
            final Uri uri = getRepoUri(repo.getId());
            return resolver.delete(uri, null, null);
        }

        /**
         * Find an app which is closest to the version code suggested by the server, with some caveates:
         * <ul>
         * <li>If installed, limit to apks signed by the same signer as the installed apk.</li>
         * <li>Otherwise, limit to apks signed by the "preferred" signer (see {@link App#preferredSigner}).</li>
         * </ul>
         */
        public static Apk findSuggestedApk(Context context, App app) {
            return findApkFromAnyRepo(context, app.packageName, app.suggestedVersionCode,
                    app.getMostAppropriateSignature());
        }

        public static Apk findApkFromAnyRepo(Context context, String packageName, int versionCode) {
            return findApkFromAnyRepo(context, packageName, versionCode, null, Cols.ALL);
        }

        public static Apk findApkFromAnyRepo(Context context, String packageName, int versionCode,
                                             String signature) {
            return findApkFromAnyRepo(context, packageName, versionCode, signature, Cols.ALL);
        }

        public static Apk findApkFromAnyRepo(Context context, String packageName, int versionCode,
                                             @Nullable String signature, String[] projection) {
            final Uri uri = getApkFromAnyRepoUri(packageName, versionCode, signature);
            return findByUri(context, uri, projection);
        }

        public static Apk findByUri(Context context, Uri uri, String[] projection) {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            Apk apk = null;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    apk = new Apk(cursor);
                }
                cursor.close();
            }
            return apk;
        }

        public static List<Apk> findByPackageName(Context context, String packageName) {
            ContentResolver resolver = context.getContentResolver();
            final Uri uri = getAppUri(packageName);
            final String sort = "apk." + Cols.VERSION_CODE + " DESC";
            Cursor cursor = resolver.query(uri, Cols.ALL, null, null, sort);
            return cursorToList(cursor);
        }

        public static List<Apk> findByRepo(Context context, Repo repo, String[] fields) {
            ContentResolver resolver = context.getContentResolver();
            final Uri uri = getRepoUri(repo.getId());
            Cursor cursor = resolver.query(uri, fields, null, null, null);
            return cursorToList(cursor);
        }

        @NonNull
        public static List<Apk> findAppVersionsByRepo(Context context, App app, Repo repo) {
            ContentResolver resolver = context.getContentResolver();
            final Uri uri = getRepoUri(repo.getId(), app.packageName);
            Cursor cursor = resolver.query(uri, Cols.ALL, null, null, null);
            return cursorToList(cursor);
        }

        private static Apk cursorToApk(Cursor cursor) {
            Apk apk = null;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    apk = new Apk(cursor);
                }
                cursor.close();
            }
            return apk;
        }

        public static Apk get(Context context, Uri uri) {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(uri, Cols.ALL, null, null, null);
            return cursorToApk(cursor);
        }

        @NonNull
        public static List<Apk> findApksByHash(Context context, String apkHash) {
            if (apkHash == null) {
                return Collections.emptyList();
            }
            ContentResolver resolver = context.getContentResolver();
            final Uri uri = getContentUri();
            String selection = " apk." + Cols.HASH + " = ? ";
            String[] selectionArgs = new String[]{apkHash};
            Cursor cursor = resolver.query(uri, Cols.ALL, selection, selectionArgs, null);
            return cursorToList(cursor);
        }
    }

    private static final int CODE_PACKAGE = CODE_SINGLE + 1;
    private static final int CODE_REPO = CODE_PACKAGE + 1;
    private static final int CODE_APKS = CODE_REPO + 1;
    private static final int CODE_APK_ROW_ID = CODE_APKS + 1;
    static final int CODE_APK_FROM_ANY_REPO = CODE_APK_ROW_ID + 1;
    static final int CODE_APK_FROM_REPO = CODE_APK_FROM_ANY_REPO + 1;
    private static final int CODE_REPO_APP = CODE_APK_FROM_REPO + 1;

    private static final String PROVIDER_NAME = "ApkProvider";
    protected static final String PATH_APK_FROM_ANY_REPO = "apk-any-repo";
    protected static final String PATH_APK_FROM_REPO = "apk-from-repo";
    protected static final String PATH_REPO_APP = "repo-app";
    private static final String PATH_APKS = "apks";
    private static final String PATH_APP = "app";
    private static final String PATH_REPO = "repo";
    private static final String PATH_APK_ROW_ID = "apk-rowId";

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    private static final Map<String, String> REPO_FIELDS = new HashMap<>();
    private static final Map<String, String> PACKAGE_FIELDS = new HashMap<>();

    static {
        REPO_FIELDS.put(Cols.Repo.VERSION, RepoTable.Cols.VERSION);
        REPO_FIELDS.put(Cols.Repo.ADDRESS, RepoTable.Cols.ADDRESS);
        PACKAGE_FIELDS.put(Cols.Package.PACKAGE_NAME, PackageTable.Cols.PACKAGE_NAME);

        MATCHER.addURI(getAuthority(), PATH_REPO + "/#", CODE_REPO);
        MATCHER.addURI(getAuthority(), PATH_REPO_APP + "/#/*", CODE_REPO_APP);
        MATCHER.addURI(getAuthority(), PATH_APK_FROM_ANY_REPO + "/#/*/*", CODE_APK_FROM_ANY_REPO);
        MATCHER.addURI(getAuthority(), PATH_APK_FROM_ANY_REPO + "/#/*", CODE_APK_FROM_ANY_REPO);
        MATCHER.addURI(getAuthority(), PATH_APK_FROM_REPO + "/#/#", CODE_APK_FROM_REPO);
        MATCHER.addURI(getAuthority(), PATH_APKS + "/*", CODE_APKS);
        MATCHER.addURI(getAuthority(), PATH_APP + "/*", CODE_PACKAGE);
        MATCHER.addURI(getAuthority(), PATH_APK_ROW_ID + "/#", CODE_APK_ROW_ID);
        MATCHER.addURI(getAuthority(), null, CODE_LIST);
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    private Uri getApkUri(long apkRowId) {
        return getContentUri().buildUpon()
                .appendPath(PATH_APK_ROW_ID)
                .appendPath(Long.toString(apkRowId))
                .build();
    }

    public static Uri getAppUri(String packageName) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_APP)
                .appendPath(packageName)
                .build();
    }

    public static Uri getRepoUri(long repoId) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_REPO)
                .appendPath(Long.toString(repoId))
                .build();
    }

    public static Uri getRepoUri(long repoId, String packageName) {
        return getContentUri()
                .buildUpon()
                .appendPath(PATH_REPO_APP)
                .appendPath(Long.toString(repoId))
                .appendPath(packageName)
                .build();
    }

    public static Uri getApkFromAnyRepoUri(Apk apk) {
        return getApkFromAnyRepoUri(apk.packageName, apk.versionCode, null);
    }

    public static Uri getApkFromAnyRepoUri(String packageName, int versionCode, @Nullable String signature) {
        Uri.Builder builder = getContentUri()
                .buildUpon()
                .appendPath(PATH_APK_FROM_ANY_REPO)
                .appendPath(Integer.toString(versionCode))
                .appendPath(packageName);

        if (signature != null) {
            builder.appendPath(signature);
        }

        return builder.build();
    }

    @Override
    protected String getTableName() {
        return ApkTable.NAME;
    }

    protected String getApkAntiFeatureJoinTableName() {
        return ApkAntiFeatureJoinTable.NAME;
    }

    protected String getAppTableName() {
        return AppMetadataTable.NAME;
    }

    @Override
    protected String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    protected UriMatcher getMatcher() {
        return MATCHER;
    }

    private class Query extends QueryBuilder {

        private boolean repoTableRequired;
        private boolean antiFeaturesRequested;

        /**
         * If the query includes anti features, then we group by apk id. This
         * is because joining onto the anti-features table will result in
         * multiple result rows for each apk (potentially), so we will
         * {@code GROUP_CONCAT} each of the anti-features into a single comma-
         * separated list for each apk. If we are _not_ including anti-
         * features, then don't group by apk, because when doing a COUNT(*)
         * this will result in the wrong result.
         */
        @Override
        protected String groupBy() {
            return antiFeaturesRequested ? "apk." + Cols.ROW_ID : null;
        }

        @Override
        protected String getRequiredTables() {
            final String apk = getTableName();
            final String app = getAppTableName();
            final String pkg = PackageTable.NAME;

            return apk + " AS apk " +
                    " LEFT JOIN " + app + " AS app ON (app." + AppMetadataTable.Cols.ROW_ID + " = apk." + Cols.APP_ID + ")" + // NOPMD NOCHECKSTYLE LineLength
                    " LEFT JOIN " + pkg + " AS pkg ON (pkg." + PackageTable.Cols.ROW_ID + " = app." + AppMetadataTable.Cols.PACKAGE_ID + ")"; // NOPMD NOCHECKSTYLE LineLength
        }

        @Override
        public void addField(String field) {
            if (PACKAGE_FIELDS.containsKey(field)) {
                addPackageField(PACKAGE_FIELDS.get(field), field);
            } else if (REPO_FIELDS.containsKey(field)) {
                addRepoField(REPO_FIELDS.get(field), field);
            } else if (Cols.AntiFeatures.ANTI_FEATURES.equals(field)) {
                antiFeaturesRequested = true;
                addAntiFeatures();
            } else if (field.equals(Cols._ID)) {
                appendField("rowid", "apk", "_id");
            } else if (field.equals(Cols._COUNT)) {
                appendField("COUNT(*) AS " + Cols._COUNT);
            } else if (field.equals(Cols._COUNT_DISTINCT)) {
                appendField("COUNT(DISTINCT apk." + Cols.APP_ID + ") AS " + Cols._COUNT_DISTINCT);
            } else {
                appendField(field, "apk");
            }
        }

        private void addPackageField(String field, String alias) {
            appendField(field, "pkg", alias);
        }

        private void addRepoField(String field, String alias) {
            if (!repoTableRequired) {
                repoTableRequired = true;
                leftJoin(RepoTable.NAME, "repo", "apk." + Cols.REPO_ID + " = repo." + RepoTable.Cols._ID);
            }
            appendField(field, "repo", alias);
        }

        private void addAntiFeatures() {
            String apkAntiFeature = "apkAntiFeatureJoin";
            String antiFeature = "antiFeature";

            leftJoin(getApkAntiFeatureJoinTableName(), apkAntiFeature,
                    "apk." + Cols.ROW_ID + " = " + apkAntiFeature + "." + ApkAntiFeatureJoinTable.Cols.APK_ID);

            leftJoin(AntiFeatureTable.NAME, antiFeature,
                    apkAntiFeature + "." + ApkAntiFeatureJoinTable.Cols.ANTI_FEATURE_ID + " = "
                            + antiFeature + "." + AntiFeatureTable.Cols.ROW_ID);

            appendField("group_concat(" + antiFeature + "." + AntiFeatureTable.Cols.NAME + ") as "
                    + Cols.AntiFeatures.ANTI_FEATURES);
        }
    }

    private QuerySelection queryPackage(String packageName) {
        final String selection = "pkg." + PackageTable.Cols.PACKAGE_NAME + " = ?";
        final String[] args = {packageName};
        return new QuerySelection(selection, args);
    }

    private QuerySelection querySingleFromAnyRepo(Uri uri) {
        return querySingleFromAnyRepo(uri, true);
    }

    private QuerySelection querySingleFromAnyRepo(Uri uri, boolean includeAlias) {
        String alias = includeAlias ? "apk." : "";

        String selection =
                alias + Cols.VERSION_CODE + " = ? AND " +
                        alias + Cols.APP_ID + " IN (" + getMetadataIdFromPackageNameQuery() + ")";

        List<String> pathSegments = uri.getPathSegments();
        List<String> args = new ArrayList<>(3);
        args.add(pathSegments.get(1)); // 0th path segment is the word "apk" and we are not interested in it.
        args.add(pathSegments.get(2));

        if (pathSegments.size() >= 4) {
            selection += " AND " + alias + Cols.SIGNATURE + " = ? ";
            args.add(pathSegments.get(3));
        }

        return new QuerySelection(selection, args);
    }

    private QuerySelection querySingle(long apkRowId) {
        return querySingle(apkRowId, true);
    }

    private QuerySelection querySingle(long apkRowId, boolean includeAlias) {
        String alias = includeAlias ? "apk." : "";
        final String selection = alias + Cols.ROW_ID + " = ?";
        final String[] args = {Long.toString(apkRowId)};
        return new QuerySelection(selection, args);
    }

    /**
     * Doesn't prefix column names with table alias. This is so that it can be used in UPDATE
     * queries. Note that this lack of table alias prefixes means this can't be used for general
     * constraints in a regular select query within {@link ApkProvider} as the queries specify
     * aliases for the apk table.
     */
    private QuerySelection querySingleWithAppId(Uri uri) {
        List<String> path = uri.getPathSegments();
        String appId = path.get(1);
        String versionCode = path.get(2);
        final String selection = Cols.APP_ID + " = ? AND " + Cols.VERSION_CODE + " = ? ";
        final String[] args = {appId, versionCode};
        return new QuerySelection(selection, args);
    }

    protected QuerySelection queryRepo(long repoId) {
        return queryRepo(repoId, true);
    }

    protected QuerySelection queryRepo(long repoId, boolean includeAlias) {
        String alias = includeAlias ? "apk." : "";
        final String selection = alias + Cols.REPO_ID + " = ? ";
        final String[] args = {Long.toString(repoId)};
        return new QuerySelection(selection, args);
    }

    protected QuerySelection queryApks(String apkKeys) {
        return queryApks(apkKeys, true);
    }

    protected QuerySelection queryApks(String apkKeys, boolean includeAlias) {
        final String[] apkDetails = apkKeys.split(",");
        if (apkDetails.length > MAX_APKS_TO_QUERY) {
            throw new IllegalArgumentException(
                    "Cannot query more than " + MAX_APKS_TO_QUERY + ". " +
                            "You tried to query " + apkDetails.length);
        }
        String alias = includeAlias ? "apk." : "";
        final String[] args = new String[apkDetails.length * 2];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < apkDetails.length; i++) {
            String[] parts = apkDetails[i].split(":");
            String appId = parts[0];
            String versionCode = parts[1];
            args[i * 2] = appId;
            args[i * 2 + 1] = versionCode;
            if (i != 0) {
                sb.append(" OR ");
            }

            sb.append(" ( ")
                    .append(Cols.APP_ID)
                    .append(" = ? ")
                    .append(" AND ")
                    .append(alias)
                    .append(Cols.VERSION_CODE)
                    .append(" = ? ) ");
        }

        return new QuerySelection(sb.toString(), args);
    }

    private String getMetadataIdFromPackageNameQuery() {
        return "SELECT m." + AppMetadataTable.Cols.ROW_ID + " " +
                "FROM " + AppMetadataTable.NAME + " AS m " +
                "JOIN " + PackageTable.NAME + " AS p ON ( " +
                "  m." + AppMetadataTable.Cols.PACKAGE_ID + " = p." + PackageTable.Cols.ROW_ID + " ) " +
                "WHERE p." + PackageTable.Cols.PACKAGE_NAME + " = ?";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        QuerySelection query = new QuerySelection(selection, selectionArgs);

        switch (MATCHER.match(uri)) {
            case CODE_REPO_APP:
                List<String> uriSegments = uri.getPathSegments();
                Long repoId = Long.parseLong(uriSegments.get(1));
                String packageName = uriSegments.get(2);
                query = query.add(queryRepo(repoId)).add(queryPackage(packageName));
                break;

            case CODE_LIST:
                break;

            case CODE_APK_FROM_ANY_REPO:
                query = query.add(querySingleFromAnyRepo(uri));
                break;

            case CODE_APK_ROW_ID:
                query = query.add(querySingle(Long.parseLong(uri.getLastPathSegment())));
                break;

            case CODE_PACKAGE:
                query = query.add(queryPackage(uri.getLastPathSegment()));
                break;

            case CODE_APKS:
                query = query.add(queryApks(uri.getLastPathSegment()));
                break;

            case CODE_REPO:
                query = query.add(queryRepo(Long.parseLong(uri.getLastPathSegment())));
                break;

            default:
                Log.e(TAG, "Invalid URI for apk content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
        }

        Query queryBuilder = new Query();
        for (final String field : projection) {
            queryBuilder.addField(field);
        }
        queryBuilder.addSelection(query);
        queryBuilder.addOrderBy(sortOrder);

        Cursor cursor = LoggingQuery.query(db(), queryBuilder.toString(), queryBuilder.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    private static void removeFieldsFromOtherTables(ContentValues values) {
        for (Map.Entry<String, String> repoField : REPO_FIELDS.entrySet()) {
            final String field = repoField.getKey();
            if (values.containsKey(field)) {
                values.remove(field);
            }
        }

        for (Map.Entry<String, String> appField : PACKAGE_FIELDS.entrySet()) {
            final String field = appField.getKey();
            if (values.containsKey(field)) {
                values.remove(field);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        boolean saveAntiFeatures = false;
        String[] antiFeatures = null;
        if (values.containsKey(Cols.AntiFeatures.ANTI_FEATURES)) {
            saveAntiFeatures = true;
            String antiFeaturesString = values.getAsString(Cols.AntiFeatures.ANTI_FEATURES);
            antiFeatures = Utils.parseCommaSeparatedString(antiFeaturesString);
            values.remove(Cols.AntiFeatures.ANTI_FEATURES);
        }

        removeFieldsFromOtherTables(values);
        validateFields(Cols.ALL, values);
        long newId = db().insertOrThrow(getTableName(), null, values);

        if (saveAntiFeatures) {
            ensureAntiFeatures(antiFeatures, newId);
        }

        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return getApkUri(newId);
    }

    protected void ensureAntiFeatures(String[] antiFeatures, long apkId) {
        db().delete(getApkAntiFeatureJoinTableName(),
                ApkAntiFeatureJoinTable.Cols.APK_ID + " = ?",
                new String[]{Long.toString(apkId)});
        if (antiFeatures != null) {
            Set<String> antiFeatureSet = new HashSet<>();
            for (String antiFeatureName : antiFeatures) {

                // There is nothing stopping a server repeating a category name in the metadata of
                // an app. In order to prevent unique constraint violations, only insert once into
                // the join table.
                if (antiFeatureSet.contains(antiFeatureName)) {
                    continue;
                }

                antiFeatureSet.add(antiFeatureName);

                long antiFeatureId = ensureAntiFeature(antiFeatureName);
                ContentValues categoryValues = new ContentValues(2);
                categoryValues.put(ApkAntiFeatureJoinTable.Cols.APK_ID, apkId);
                categoryValues.put(ApkAntiFeatureJoinTable.Cols.ANTI_FEATURE_ID, antiFeatureId);
                db().insert(getApkAntiFeatureJoinTableName(), null, categoryValues);
            }
        }
    }

    protected long ensureAntiFeature(String antiFeatureName) {
        long antiFeatureId = 0;
        Cursor cursor = db().query(AntiFeatureTable.NAME, new String[]{AntiFeatureTable.Cols.ROW_ID},
                AntiFeatureTable.Cols.NAME + " = ?", new String[]{antiFeatureName}, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                antiFeatureId = cursor.getLong(0);
            }
            cursor.close();
        }

        if (antiFeatureId <= 0) {
            ContentValues values = new ContentValues(1);
            values.put(AntiFeatureTable.Cols.NAME, antiFeatureName);
            antiFeatureId = db().insert(AntiFeatureTable.NAME, null, values);
        }

        return antiFeatureId;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        QuerySelection query = new QuerySelection(where, whereArgs);

        switch (MATCHER.match(uri)) {

            case CODE_REPO:
                query = query.add(queryRepo(Long.parseLong(uri.getLastPathSegment()), false));
                break;

            case CODE_APKS:
                query = query.add(queryApks(uri.getLastPathSegment(), false));
                break;

            default:
                Log.e(TAG, "Invalid URI for apk content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
        }

        int rowsAffected = db().delete(getTableName(), query.getSelection(), query.getArgs());
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;

    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if (MATCHER.match(uri) != CODE_APK_FROM_REPO) {
            throw new UnsupportedOperationException("Cannot update anything other than a single apk.");
        }

        boolean saveAntiFeatures = false;
        String[] antiFeatures = null;
        if (values.containsKey(Cols.AntiFeatures.ANTI_FEATURES)) {
            saveAntiFeatures = true;
            String antiFeaturesString = values.getAsString(Cols.AntiFeatures.ANTI_FEATURES);
            antiFeatures = Utils.parseCommaSeparatedString(antiFeaturesString);
            values.remove(Cols.AntiFeatures.ANTI_FEATURES);
        }

        validateFields(Cols.ALL, values);
        removeFieldsFromOtherTables(values);

        QuerySelection query = new QuerySelection(where, whereArgs);
        query = query.add(querySingleWithAppId(uri));

        int numRows = db().update(getTableName(), values, query.getSelection(), query.getArgs());

        if (saveAntiFeatures) {
            // Get the database ID of the row we just updated, so that we can join relevant anti features to it.
            Cursor result = db().query(getTableName(), new String[]{Cols.ROW_ID},
                    query.getSelection(), query.getArgs(), null, null, null);
            if (result != null) {
                result.moveToFirst();
                long apkId = result.getLong(0);
                ensureAntiFeatures(antiFeatures, apkId);
                result.close();
            }
        }

        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return numRows;
    }

}
