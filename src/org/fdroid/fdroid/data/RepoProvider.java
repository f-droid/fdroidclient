package org.fdroid.fdroid.data;

import android.content.*;
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

    public static final class Helper {

        public static final String TAG = "RepoProvider.Helper";

        private Helper() {}

        public static Repo findById(Context context, long repoId) {
            return findById(context, repoId, DataColumns.ALL);
        }

        public static Repo findById(Context context, long repoId,
                                    String[] projection) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = RepoProvider.getContentUri(repoId);
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            Repo repo = null;
            if (cursor != null) {
                cursor.moveToFirst();
                repo = new Repo(cursor);
            }
            return repo;
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
            String[] args = { fieldValue };
            Cursor cursor = resolver.query(
                    uri, projection, fieldName + " = ?", args, null );
            return cursorToList(cursor);
        }

        private static List<Repo> cursorToList(Cursor cursor) {
            List<Repo> repos = new ArrayList<Repo>();
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    repos.add(new Repo(cursor));
                    cursor.moveToNext();
                }
                cursor.close();
            }
            return repos;
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

            Uri uri = getContentUri(repo.getId());
            String[] args = { Long.toString(repo.getId()) };
            resolver.update(uri, values, DataColumns._ID + " = ?", args );
            repo.setValues(values);
        }

        /**
         * This doesn't do anything other than call "insert" on the content
         * resolver, but I thought I'd put it here in the interests of having
         * each of the CRUD methods available in the helper class.
         */
        public static void insert(Context context,
                                  ContentValues values) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = RepoProvider.getContentUri();
            resolver.insert(uri, values);
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
            Log.d("FDroid", "Removed " + apkCount + " apks from repo " + repo.name);

            Uri appUri = AppProvider.getNoApksUri();
            int appCount = resolver.delete(appUri, null, null);
            Log.d("Log", "Removed " + appCount + " apps with no apks.");

            app.invalidateAllApps();
        }

        public static int countAppsForRepo(Context context, long repoId) {
            ContentResolver resolver = context.getContentResolver();
            String[] projection = { "COUNT(distinct id)" };
            String selection = "repo = ?";
            String[] args = { Long.toString(repoId) };
            Uri apkUri = ApkProvider.getContentUri();
            Cursor result = resolver.query(apkUri, projection, selection, args, null);
            if (result != null && result.getCount() > 0) {
                result.moveToFirst();
                return result.getInt(0);
            } else {
                return 0;
            }
        }
    }

    public interface DataColumns extends BaseColumns {
        public static String ADDRESS      = "address";
        public static String NAME         = "name";
        public static String DESCRIPTION  = "description";
        public static String IN_USE       = "inuse";
        public static String PRIORITY     = "priority";
        public static String PUBLIC_KEY   = "pubkey";
        public static String FINGERPRINT  = "fingerprint";
        public static String MAX_AGE      = "maxage";
        public static String LAST_ETAG    = "lastetag";
        public static String LAST_UPDATED = "lastUpdated";
        public static String VERSION      = "version";

        public static String[] ALL = {
            _ID, ADDRESS, NAME, DESCRIPTION, IN_USE, PRIORITY, PUBLIC_KEY,
            FINGERPRINT, MAX_AGE, LAST_UPDATED, LAST_ETAG, VERSION
        };
    }

    private static final String PROVIDER_NAME = "RepoProvider";

    private static final UriMatcher matcher = new UriMatcher(-1);

    static {
        matcher.addURI(AUTHORITY + "." + PROVIDER_NAME, null, CODE_LIST);
        matcher.addURI(AUTHORITY + "." + PROVIDER_NAME, "#", CODE_SINGLE);
    }

    public static Uri getContentUri() {
        return Uri.parse("content://" + AUTHORITY + "." + PROVIDER_NAME);
    }

    public static Uri getContentUri(long repoId) {
        return ContentUris.withAppendedId(getContentUri(), repoId);
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

        switch (matcher.match(uri)) {
            case CODE_LIST:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = "_ID ASC";
                }
                break;

            case CODE_SINGLE:
                selection = ( selection == null ? "" : selection + " AND " ) +
                        DataColumns._ID + " = " + uri.getLastPathSegment();
                break;

            default:
                Log.e("FDroid", "Invalid URI for repo content provider: " + uri);
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
            String address = values.getAsString(DataColumns.ADDRESS);
            values.put(DataColumns.NAME, Repo.addressToName(address));
        }

        long id = write().insertOrThrow(getTableName(), null, values);
        Log.i("FDroid", "Inserted repo. Notifying provider change: '" + uri + "'.");
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
                Log.e("FDroid", "Invalid URI for repo content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for repo content provider: " + uri);
        }

        int rowsAffected = write().delete(getTableName(), where, whereArgs);
        Log.i("FDroid", "Deleted repos. Notifying provider change: '" + uri + "'.");
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int numRows = write().update(getTableName(), values, where, whereArgs);
        Log.i("FDroid", "Updated repo. Notifying provider change: '" + uri + "'.");
        getContext().getContentResolver().notifyChange(uri, null);
        return numRows;
    }

}
