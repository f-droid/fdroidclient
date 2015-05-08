package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;

import java.util.ArrayList;
import java.util.List;

public class RepoProvider extends FDroidProvider {

    private static final String TAG = "RepoProvider";

    public static final class Helper {

        private static final String TAG = "RepoProvider.Helper";

        private Helper() {}

        public static Repo findByUri(Context context, Uri uri) {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(uri, DataColumns.ALL, null, null, null);
            return cursorToRepo(cursor);
        }

        public static Repo findById(Context context, long repoId) {
            return findById(context, repoId, DataColumns.ALL);
        }

        public static Repo findById(Context context, long repoId,
                                    String[] projection) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = RepoProvider.getContentUri(repoId);
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            return cursorToRepo(cursor);
        }

        public static Repo findByAddress(Context context, String address) {
            return findByAddress(context, address, DataColumns.ALL);
        }

        public static Repo findByAddress(Context context,
                                         String address, String[] projection) {
            List<Repo> repos = findBy(
                    context, DataColumns.ADDRESS, address, projection);
            return repos.size() > 0 ? repos.get(0) : null;
        }

        public static List<Repo> all(Context context) {
            return all(context, DataColumns.ALL);
        }

        public static List<Repo> all(Context context, String[] projection) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = RepoProvider.getContentUri();
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            return cursorToList(cursor);
        }

        private static List<Repo> findBy(Context context,
                                         String fieldName,
                                         String fieldValue,
                                         String[] projection) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = RepoProvider.getContentUri();
            final String[] args = { fieldValue };
            Cursor cursor = resolver.query(
                    uri, projection, fieldName + " = ?", args, null);
            return cursorToList(cursor);
        }

        private static List<Repo> cursorToList(Cursor cursor) {
            int knownRepoCount = cursor != null ? cursor.getCount() : 0;
            List<Repo> repos = new ArrayList<>(knownRepoCount);
            if (cursor != null) {
                if (knownRepoCount > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        repos.add(new Repo(cursor));
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            return repos;
        }

        private static Repo cursorToRepo(Cursor cursor) {
            Repo repo = null;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    repo = new Repo(cursor);
                }
                cursor.close();
            }
            return repo;
        }

        public static void update(Context context, Repo repo,
                                  ContentValues values) {
            ContentResolver resolver = context.getContentResolver();

            // Change the name to the new address. Next time we update the repo
            // index file, it will populate the name field with the proper
            // name, but the best we can do is guess right now.
            if (values.containsKey(DataColumns.ADDRESS) &&
                    !values.containsKey(DataColumns.NAME)) {
                String name = Repo.addressToName(values.getAsString(DataColumns.ADDRESS));
                values.put(DataColumns.NAME, name);
            }

            /*
             * If the repo is signed and has a public key, then guarantee that
             * the fingerprint is also set. The stored fingerprint is checked
             * when a repo URI is received by FDroid to prevent bad actors from
             * overriding repo configs with other keys. So if the fingerprint is
             * not stored yet, calculate it and store it. If the fingerprint is
             * stored, then check it against the calculated fingerprint just to
             * make sure it is correct. If the fingerprint is empty, then store
             * the calculated one.
             */
            if (values.containsKey(DataColumns.PUBLIC_KEY)) {
                String publicKey = values.getAsString(DataColumns.PUBLIC_KEY);
                String calcedFingerprint = Utils.calcFingerprint(publicKey);
                if (values.containsKey(DataColumns.FINGERPRINT)) {
                    String fingerprint = values.getAsString(DataColumns.FINGERPRINT);
                    if (!TextUtils.isEmpty(publicKey)) {
                        if (TextUtils.isEmpty(fingerprint)) {
                            values.put(DataColumns.FINGERPRINT, calcedFingerprint);
                        } else if (!fingerprint.equals(calcedFingerprint)) {
                            // TODO the UI should represent this error!
                            Log.e(TAG, "The stored and calculated fingerprints do not match!");
                            Log.e(TAG, "stored: " + fingerprint);
                            Log.e(TAG, "calced: " + calcedFingerprint);
                        }
                    }
                } else if (!TextUtils.isEmpty(publicKey)) {
                    // no fingerprint in 'values', so put one there
                    values.put(DataColumns.FINGERPRINT, calcedFingerprint);
                }
            }

            if (values.containsKey(DataColumns.IN_USE)) {
                Integer inUse = values.getAsInteger(DataColumns.IN_USE);
                if (inUse != null && inUse == 0) {
                    values.put(DataColumns.LAST_ETAG, (String)null);
                }
            }

            final Uri uri = getContentUri(repo.getId());
            final String[] args = { Long.toString(repo.getId()) };
            resolver.update(uri, values, DataColumns._ID + " = ?", args);
            repo.setValues(values);
        }

        /**
         * This doesn't do anything other than call "insert" on the content
         * resolver, but I thought I'd put it here in the interests of having
         * each of the CRUD methods available in the helper class.
         */
        public static Uri insert(Context context,
                                  ContentValues values) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = RepoProvider.getContentUri();
            return resolver.insert(uri, values);
        }

        public static void remove(Context context, long repoId) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = RepoProvider.getContentUri(repoId);
            resolver.delete(uri, null, null);
        }

        public static void purgeApps(Context context, Repo repo, FDroidApp app) {
            Uri apkUri = ApkProvider.getRepoUri(repo.getId());
            ContentResolver resolver = context.getContentResolver();
            int apkCount = resolver.delete(apkUri, null, null);
            Log.d(TAG, "Removed " + apkCount + " apks from repo " + repo.name);

            Uri appUri = AppProvider.getNoApksUri();
            int appCount = resolver.delete(appUri, null, null);
            Log.d(TAG, "Removed " + appCount + " apps with no apks.");
        }

        public static int countAppsForRepo(Context context, long repoId) {
            ContentResolver resolver = context.getContentResolver();
            final String[] projection = { ApkProvider.DataColumns._COUNT_DISTINCT_ID };
            Uri apkUri = ApkProvider.getRepoUri(repoId);
            Cursor cursor = resolver.query(apkUri, projection, null, null, null);
            int count = 0;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    count = cursor.getInt(0);
                }
                cursor.close();
            }
            return count;
        }
    }

    public interface DataColumns extends BaseColumns {

        String ADDRESS      = "address";
        String NAME         = "name";
        String DESCRIPTION  = "description";
        String IN_USE       = "inuse";
        String PRIORITY     = "priority";
        String PUBLIC_KEY   = "pubkey";
        String FINGERPRINT  = "fingerprint";
        String MAX_AGE      = "maxage";
        String LAST_ETAG    = "lastetag";
        String LAST_UPDATED = "lastUpdated";
        String VERSION      = "version";
        String IS_SWAP      = "isSwap";

        String[] ALL = {
            _ID, ADDRESS, NAME, DESCRIPTION, IN_USE, PRIORITY, PUBLIC_KEY,
            FINGERPRINT, MAX_AGE, LAST_UPDATED, LAST_ETAG, VERSION, IS_SWAP
        };
    }

    private static final String PROVIDER_NAME = "RepoProvider";
    private static final String PATH_ALL_EXCEPT_SWAP = "allExceptSwap";

    private static final int CODE_ALL_EXCEPT_SWAP = CODE_SINGLE + 1;

    private static final UriMatcher matcher = new UriMatcher(-1);

    static {
        matcher.addURI(AUTHORITY + "." + PROVIDER_NAME, null, CODE_LIST);
        matcher.addURI(AUTHORITY + "." + PROVIDER_NAME, PATH_ALL_EXCEPT_SWAP, CODE_ALL_EXCEPT_SWAP);
        matcher.addURI(AUTHORITY + "." + PROVIDER_NAME, "#", CODE_SINGLE);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + AUTHORITY + "." + PROVIDER_NAME);
    }

    public static Uri getContentUri(long repoId) {
        return ContentUris.withAppendedId(getContentUri(), repoId);
    }

    public static Uri allExceptSwapUri() {
        return getContentUri().buildUpon()
                .appendPath(PATH_ALL_EXCEPT_SWAP)
                .build();
    }

    @Override
    protected String getTableName() {
        return DBHelper.TABLE_REPO;
    }

    @Override
    protected String getProviderName() {
        return "RepoProvider";
    }

    @Override
    protected UriMatcher getMatcher() {
        return matcher;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = "_ID ASC";
        }

        switch (matcher.match(uri)) {
        case CODE_LIST:
            // Do nothing (don't restrict query)
            break;

        case CODE_SINGLE:
            selection = ( selection == null ? "" : selection + " AND " ) +
                    DataColumns._ID + " = " + uri.getLastPathSegment();
            break;

        case CODE_ALL_EXCEPT_SWAP:
            selection = DataColumns.IS_SWAP + " = 0 OR " + DataColumns.IS_SWAP + " IS NULL ";
            break;

        default:
            Log.e(TAG, "Invalid URI for repo content provider: " + uri);
            throw new UnsupportedOperationException("Invalid URI for repo content provider: " + uri);
        }

        Cursor cursor = read().query(getTableName(), projection, selection,
                selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        if (!values.containsKey(DataColumns.ADDRESS)) {
            throw new UnsupportedOperationException("Cannot add repo without an address.");
        }

        // The following fields have NOT NULL constraints in the DB, so need
        // to be present.

        if (!values.containsKey(DataColumns.IN_USE)) {
            values.put(DataColumns.IN_USE, 1);
        }

        if (!values.containsKey(DataColumns.PRIORITY)) {
            values.put(DataColumns.PRIORITY, 10);
        }

        if (!values.containsKey(DataColumns.MAX_AGE)) {
            values.put(DataColumns.MAX_AGE, 0);
        }

        if (!values.containsKey(DataColumns.VERSION)) {
            values.put(DataColumns.VERSION, 0);
        }

        if (!values.containsKey(DataColumns.NAME)) {
            final String address = values.getAsString(DataColumns.ADDRESS);
            values.put(DataColumns.NAME, Repo.addressToName(address));
        }

        long id = write().insertOrThrow(getTableName(), null, values);
        Log.i(TAG, "Inserted repo. Notifying provider change: '" + uri + "'.");
        getContext().getContentResolver().notifyChange(uri, null);
        return getContentUri(id);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        switch (matcher.match(uri)) {
        case CODE_LIST:
            // Don't support deleting of multiple repos.
            return 0;

        case CODE_SINGLE:
            where = ( where == null ? "" : where + " AND " ) +
                    "_ID = " + uri.getLastPathSegment();
            break;

        default:
            Log.e(TAG, "Invalid URI for repo content provider: " + uri);
            throw new UnsupportedOperationException("Invalid URI for repo content provider: " + uri);
        }

        int rowsAffected = write().delete(getTableName(), where, whereArgs);
        Log.i(TAG, "Deleted repos. Notifying provider change: '" + uri + "'.");
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int numRows = write().update(getTableName(), values, where, whereArgs);
        Log.i(TAG, "Updated repo. Notifying provider change: '" + uri + "'.");
        getContext().getContentResolver().notifyChange(uri, null);
        return numRows;
    }

}
