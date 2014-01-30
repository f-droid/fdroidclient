package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import org.fdroid.fdroid.DB;

import java.util.*;

public class ApkProvider extends FDroidProvider {

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
            resolver.delete(uri, selection + " = ?", args);
        }

        public static void deleteApksByApp(Context context, DB.App app) {
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
        public static String REPO_VERSION    = "repoVersion";
        public static String REPO_ADDRESS    = "repoAddress";

        public static String[] ALL = {
            _ID, APK_ID, VERSION, REPO_ID, HASH, VERSION_CODE, NAME, SIZE,
            SIGNATURE, SOURCE_NAME, MIN_SDK_VERSION, PERMISSIONS, FEATURES,
            NATIVE_CODE, HASH_TYPE, ADDED_DATE, IS_COMPATIBLE,

            REPO_VERSION, REPO_ADDRESS
        };
    }

    private static final String PROVIDER_NAME = "ApkProvider";

    private static final UriMatcher matcher = new UriMatcher(-1);

    public static Map<String,String> REPO_FIELDS = new HashMap<String,String>();

    static {
        REPO_FIELDS.put(DataColumns.REPO_VERSION, RepoProvider.DataColumns.VERSION);
        REPO_FIELDS.put(DataColumns.REPO_ADDRESS, RepoProvider.DataColumns.ADDRESS);

        matcher.addURI(AUTHORITY + "." + PROVIDER_NAME, null, CODE_LIST);
        matcher.addURI(AUTHORITY + "." + PROVIDER_NAME, "/*/#", CODE_SINGLE);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + AUTHORITY + "." + PROVIDER_NAME);
    }

    public static Uri getContentUri(String id, int versionCode) {
        return getContentUri()
            .buildUpon()
            .appendPath(Integer.toString(versionCode))
            .appendPath(id)
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

    private String appendPrimaryKeyToSelection(String selection) {
        return (selection == null ? "" : selection + " AND ") + " id = ? and vercode = ?";
    }

    private String[] appendPrimaryKeyToArgs(Uri uri, String[] selectionArgs) {
        List<String> args = new ArrayList<String>(selectionArgs.length + 2);
        for (String arg : args) {
            args.add(arg);
        }
        args.addAll(uri.getPathSegments());
        return (String[])args.toArray();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        switch (matcher.match(uri)) {
            case CODE_LIST:
                break;

            case CODE_SINGLE:
                selection = appendPrimaryKeyToSelection(selection);
                selectionArgs = appendPrimaryKeyToArgs(uri, selectionArgs);
                break;

            default:
                Log.e("FDroid", "Invalid URI for apk content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
        }

        QueryBuilder query = new QueryBuilder();
        for (String field : projection) {
            query.addField(field);
        }
        query.addSelection(selection);
        query.addOrderBy(sortOrder);

        Cursor cursor = read().rawQuery(query.toString(), selectionArgs);
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
        getContext().getContentResolver().notifyChange(uri, null);
        return getContentUri(
            values.getAsString(DataColumns.APK_ID),
            values.getAsInteger(DataColumns.VERSION_CODE));

    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        switch (matcher.match(uri)) {
            case CODE_LIST:
                // Don't support deleting of multiple apks yet.
                return 0;

            case CODE_SINGLE:
                where = appendPrimaryKeyToSelection(where);
                whereArgs = appendPrimaryKeyToArgs(uri, whereArgs);
                break;

            default:
                Log.e("FDroid", "Invalid URI for apk content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for apk content provider: " + uri);
        }

        int rowsAffected = write().delete(getTableName(), where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;

    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        switch (matcher.match(uri)) {
            case CODE_LIST:
                return 0;

            case CODE_SINGLE:
                where = appendPrimaryKeyToSelection(where);
                whereArgs = appendPrimaryKeyToArgs(uri, whereArgs);
                break;
        }

        removeRepoFields(values);
        int numRows = write().update(getTableName(), values, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return numRows;

    }

}
