package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.*;

public class ApkProvider extends FDroidProvider {

    /**
     * SQLite has a maximum of 999 parameters in a query. Each apk we add
     * requires two (id and vercode) so we can only query half of that. Then,
     * we may want to add additional constraints, so we give our self some
     * room by saying only 450 apks can be queried at once.
     */
    public static final int MAX_APKS_TO_QUERY = 450;

    public static final class Helper {

        private Helper() {}

        public static void update(Context context, Apk apk,
                                  String id, int versionCode) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getContentUri(id, versionCode);
            resolver.update(uri, apk.toContentValues(), null, null);
        }

        public static void update(Context context, Apk apk) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getContentUri(apk.id, apk.vercode);
            resolver.update(uri, apk.toContentValues(), null, null);
        }

        /**
         * This doesn't do anything other than call "insert" on the content
         * resolver, but I thought I'd put it here in the interests of having
         * each of the CRUD methods available in the helper class.
         */
        public static void insert(Context context, ContentValues values) {
            ContentResolver resolver = context.getContentResolver();
            resolver.insert(getContentUri(), values);
        }

        public static void insert(Context context, Apk apk) {
            insert(context, apk.toContentValues());
        }

        public static List<Apk> all(Context context) {
            return all(context, DataColumns.ALL);
        }

        public static List<Apk> all(Context context, String[] projection) {

            ContentResolver resolver = context.getContentResolver();
            Uri uri = ApkProvider.getContentUri();
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            return cursorToList(cursor);
        }

        private static List<Apk> cursorToList(Cursor cursor) {
            List<Apk> apks = new ArrayList<Apk>();
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    apks.add(new Apk(cursor));
                    cursor.moveToNext();
                }
                cursor.close();
            }
            return apks;
        }

        public static void deleteApksByRepo(Context context, Repo repo) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getContentUri();
            String[] args = { Long.toString(repo.getId()) };
            String selection = DataColumns.REPO_ID + " = ?";
            int count = resolver.delete(uri, selection, args);
        }

        public static void deleteApksByApp(Context context, App app) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getContentUri();
            String[] args = { app.id };
            String selection = DataColumns.APK_ID + " = ?";
            resolver.delete(uri, selection, args);
        }

        public static Apk find(Context context, String id, int versionCode) {
            return find(context, id, versionCode, DataColumns.ALL);
        }

        public static Apk find(Context context, String id, int versionCode, String[] projection) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getContentUri(id, versionCode);
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                return new Apk(cursor);
            } else {
                return null;
            }
        }

        public static void delete(Context context, String id, int versionCode) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = getContentUri(id, versionCode);
            resolver.delete(uri, null, null);
        }

        public static List<Apk> findByApp(ContentResolver resolver, String appId) {
            return findByApp(resolver, appId, ApkProvider.DataColumns.ALL);
        }

        public static List<Apk> findByApp(ContentResolver resolver,
                                          String appId, String[] projection) {
            Uri uri = getAppUri(appId);
            String sort = ApkProvider.DataColumns.VERSION_CODE + " DESC";
            Cursor cursor = resolver.query(uri, projection, null, null, sort);
            return cursorToList(cursor);
        }

        /**
         * Returns apks in the database, which have the same id and version as
         * one of the apks in the "apks" argument.
         */
        public static List<Apk> knownApks(ContentResolver resolver,
                                             List<Apk> apks, String[] fields) {
            Uri uri = getContentUri(apks);
            Cursor cursor = resolver.query(uri, fields, null, null, null);
            return cursorToList(cursor);
        }
    }

    public interface DataColumns extends BaseColumns {

        public static String APK_ID = "id";
        public static String VERSION         = "version";
        public static String REPO_ID         = "repo";
        public static String HASH            = "hash";
        public static String VERSION_CODE    = "vercode";
        public static String NAME            = "apkName";
        public static String SIZE            = "size";
        public static String SIGNATURE       = "sig";
        public static String SOURCE_NAME     = "srcname";
        public static String MIN_SDK_VERSION = "minSdkVersion";
        public static String PERMISSIONS     = "permissions";
        public static String FEATURES        = "features";
        public static String NATIVE_CODE     = "nativecode";
        public static String HASH_TYPE       = "hashType";
        public static String ADDED_DATE      = "added";
        public static String IS_COMPATIBLE   = "compatible";
        public static String INCOMPATIBLE_REASONS = "incompatibleReasons";
        public static String REPO_VERSION    = "repoVersion";
        public static String REPO_ADDRESS    = "repoAddress";

        public static String[] ALL = {
            _ID, APK_ID, VERSION, REPO_ID, HASH, VERSION_CODE, NAME, SIZE,
            SIGNATURE, SOURCE_NAME, MIN_SDK_VERSION, PERMISSIONS, FEATURES,
            NATIVE_CODE, HASH_TYPE, ADDED_DATE, IS_COMPATIBLE,
            REPO_VERSION, REPO_ADDRESS, INCOMPATIBLE_REASONS
        };
    }

    private static final int CODE_APP = CODE_SINGLE + 1;
    private static final int CODE_REPO = CODE_APP + 1;
    private static final int CODE_APKS = CODE_REPO + 1;

    private static final String PROVIDER_NAME = "ApkProvider";
    private static final String PATH_APK  = "apk";
    private static final String PATH_APKS = "apks";
    private static final String PATH_APP  = "app";
    private static final String PATH_REPO = "repo";

    private static final UriMatcher matcher = new UriMatcher(-1);

    public static Map<String,String> REPO_FIELDS = new HashMap<String,String>();

    static {
        REPO_FIELDS.put(DataColumns.REPO_VERSION, RepoProvider.DataColumns.VERSION);
        REPO_FIELDS.put(DataColumns.REPO_ADDRESS, RepoProvider.DataColumns.ADDRESS);

        matcher.addURI(getAuthority(), PATH_REPO + "/#", CODE_REPO);
        matcher.addURI(getAuthority(), PATH_APK + "/#/*", CODE_SINGLE);
        matcher.addURI(getAuthority(), PATH_APKS + "/*", CODE_APKS);
        matcher.addURI(getAuthority(), PATH_APP + "/*", CODE_APP);
        matcher.addURI(getAuthority(), null, CODE_LIST);
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + getAuthority());
    }

    public static Uri getAppUri(String appId) {
        return getContentUri()
            .buildUpon()
            .appendPath(PATH_APP)
            .appendPath(appId)
            .build();
    }

    public static Uri getRepoUri(long repoId) {
        return getContentUri()
            .buildUpon()
            .appendPath(PATH_REPO)
            .appendPath(Long.toString(repoId))
            .build();
    }

    public static Uri getContentUri(Apk apk) {
        return getContentUri(apk.id, apk.vercode);
    }

    public static Uri getContentUri(String id, int versionCode) {
        return getContentUri()
            .buildUpon()
            .appendPath(PATH_APK)
            .appendPath(Integer.toString(versionCode))
            .appendPath(id)
            .build();
    }

    public static Uri getContentUri(List<Apk> apks) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < apks.size(); i ++) {
            if (i != 0) {
                builder.append(',');
            }
            Apk a = apks.get(i);
            builder.append(a.id).append(':').append(a.vercode);
        }
        return getContentUri().buildUpon()
                .appendPath(PATH_APKS)
                .appendPath(builder.toString())
                .build();
    }

    @Override
    protected String getTableName() {
        return DBHelper.TABLE_APK;
    }

    @Override
    protected String getProviderName() {
        return PROVIDER_NAME;
    }

    protected UriMatcher getMatcher() {
        return matcher;
    }

    private static class QueryBuilder {

        private StringBuilder fields = new StringBuilder();
        private StringBuilder tables = new StringBuilder(DBHelper.TABLE_APK + " AS apk");
        private String selection = null;
        private String orderBy = null;

        private boolean repoTableRequired = false;

        public void addField(String field) {
            if (REPO_FIELDS.containsKey(field)) {
                addRepoField(REPO_FIELDS.get(field), field);
            } else if (field.equals(DataColumns._ID)) {
                appendField("rowid", "apk", "_id");
            } else if (field.startsWith("COUNT")) {
                appendField(field);
            } else {
                appendField(field, "apk");
            }
        }

        public void addRepoField(String field, String alias) {
            if (!repoTableRequired) {
                repoTableRequired = true;
                tables.append(" LEFT JOIN ");
                tables.append(DBHelper.TABLE_REPO);
                tables.append(" AS repo ON (apk.repo = repo._id) ");
            }
            appendField(field, "repo", alias);
        }

        private void appendField(String field) {
            appendField(field, null, null);
        }

        private void appendField(String field, String tableAlias) {
            appendField(field, tableAlias, null);
        }

        private void appendField(String field, String tableAlias,
                                 String fieldAlias) {
            if (fields.length() != 0) {
                fields.append(',');
            }

            if (tableAlias != null) {
                fields.append(tableAlias).append('.');
            }

            fields.append(field);

            if (fieldAlias != null) {
                fields.append(" AS ").append(fieldAlias);
            }
        }

        public void addSelection(String selection) {
            this.selection = selection;
        }

        public void addOrderBy(String orderBy) {
            this.orderBy = orderBy;
        }

        public String toString() {

            StringBuilder suffix = new StringBuilder();
            if (selection != null) {
                suffix.append(" WHERE ").append(selection);
            }

            if (orderBy != null) {
                suffix.append(" ORDER BY ").append(orderBy);
            }

            return "SELECT " + fields + " FROM " + tables + suffix;
        }
    }

    private QuerySelection queryApp(String appId) {
        String selection = " id = ? ";
        String[] args = new String[] { appId };
        return new QuerySelection(selection, args);
    }

    private QuerySelection querySingle(Uri uri) {
        String selection = " vercode = ? and id = ? ";
        String[] args = new String[] {
            // First (0th) path segment is the word "apk",
            // and we are not interested in it.
            uri.getPathSegments().get(1),
            uri.getPathSegments().get(2)
        };
        return new QuerySelection(selection, args);
    }

    private QuerySelection queryRepo(long repoId) {
        String selection = " repo = ? ";
        String[] args = new String[]{ Long.toString(repoId) };
        return new QuerySelection(selection, args);
    }

    private QuerySelection queryApks(String apkKeys) {
        String[] apkDetails = apkKeys.split(",");
        String[] args = new String[apkDetails.length * 2];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < apkDetails.length; i ++) {
            String[] parts = apkDetails[i].split(":");
            String id = parts[0];
            String verCode = parts[1];
            args[i * 2] = id;
            args[i * 2 + 1] = verCode;
            if (i != 0) {
                sb.append(" OR ");
            }
            sb.append(" ( id = ? AND vercode = ? ) ");
        }
        return new QuerySelection(sb.toString(), args);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        QuerySelection query = new QuerySelection(selection, selectionArgs);

        switch (matcher.match(uri)) {
            case CODE_LIST:
                break;

            case CODE_SINGLE:
                query = query.add(querySingle(uri));
                break;

            case CODE_APP:
                query = query.add(queryApp(uri.getLastPathSegment()));
                break;

            case CODE_APKS:
                query = query.add(queryApks(uri.getLastPathSegment()));
                break;

            case CODE_REPO:
                query = query.add(queryRepo(Long.parseLong(uri.getLastPathSegment())));
                break;

            default:
                Log.e("FDroid", "Invalid URI for apk content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
        }

        QueryBuilder queryBuilder = new QueryBuilder();
        for (String field : projection) {
            queryBuilder.addField(field);
        }
        queryBuilder.addSelection(query.getSelection());
        queryBuilder.addOrderBy(sortOrder);

        Cursor cursor = read().rawQuery(queryBuilder.toString(), query.getArgs());
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    private static void removeRepoFields(ContentValues values) {
        for (Map.Entry<String,String> repoField : REPO_FIELDS.entrySet()) {
            String field = repoField.getKey();
            if (values.containsKey(field)) {
                Log.i("FDroid", "Cannot insert/update '" + field + "' field " +
                        "on apk table, as it belongs to the repo table. " +
                        "This field will be ignored.");
                values.remove(field);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        removeRepoFields(values);
        long id = write().insertOrThrow(getTableName(), null, values);
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return getContentUri(
            values.getAsString(DataColumns.APK_ID),
            values.getAsInteger(DataColumns.VERSION_CODE));

    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        QuerySelection query = new QuerySelection(where, whereArgs);

        switch (matcher.match(uri)) {
            case CODE_LIST:
                // Don't support deleting of multiple apks yet.
                return 0;

            case CODE_REPO:
                query = query.add(queryRepo(Long.parseLong(uri.getLastPathSegment())));
                break;

            case CODE_SINGLE:
                query = query.add(querySingle(uri));
                break;

            default:
                Log.e("FDroid", "Invalid URI for apk content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
        }

        int rowsAffected = write().delete(getTableName(), query.getSelection(), query.getArgs());
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;

    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        QuerySelection query = new QuerySelection(where, whereArgs);

        switch (matcher.match(uri)) {
            case CODE_LIST:
                return 0;

            case CODE_SINGLE:
                query = query.add(querySingle(uri));
                break;
        }

        removeRepoFields(values);
        int numRows = write().update(getTableName(), values, query.getSelection(), query.getArgs());
        if (!isApplyingBatch()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return numRows;

    }

}
