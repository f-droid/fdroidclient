package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.RepoTable;
import org.fdroid.fdroid.data.Schema.RepoTable.Cols;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RepoProvider extends FDroidProvider {

    private static final String TAG = "RepoProvider";

    public static final class Helper {

        private static final String TAG = "RepoProvider.Helper";

        private Helper() {
        }

        /**
         * Find by the content URI of a repo ({@link RepoProvider#getContentUri(long)}).
         */
        public static Repo get(Context context, Uri uri) {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(uri, Cols.ALL, null, null, null);
            return cursorToRepo(cursor);
        }

        public static Repo findById(Context context, long repoId) {
            return findById(context, repoId, Cols.ALL);
        }

        public static Repo findById(Context context, long repoId,
                                    String[] projection) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = RepoProvider.getContentUri(repoId);
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            return cursorToRepo(cursor);
        }

        /**
         * This method decides what repo a URL belongs to by iteratively removing path fragments and
         * checking if it belongs to a repo or not. It will match the most specific repository which
         * could serve the file at the given URL.
         * <p>
         * For any given HTTP resource requested by F-Droid, it should belong to a repository.
         * Whether that resource is an index.jar, an icon, or a .apk file, they all belong to a
         * repository. Therefore, that repository must exist in the database. The way to find out
         * which repository a particular URL came from requires some consideration:
         * <li>Repositories can exist at particular paths on a server (e.g. /fdroid/repo)
         * <li>Individual files can exist at a more specific path on the repo (e.g.
         * /fdroid/repo/icons/org.fdroid.fdroid.png)</li>
         * <p>
         * So for a given URL "/fdroid/repo/icons/org.fdroid.fdroid.png" we don't actually know
         * whether it is for the file "org.fdroid.fdroid.png" at repository "/fdroid/repo/icons" or
         * the file "icons/org.fdroid.fdroid.png" at the repository at "/fdroid/repo".
         */
        @Nullable
        public static Repo findByUrl(Context context, Uri uri, String[] projection) {
            Uri withoutQuery = uri.buildUpon().query(null).build();
            Repo repo = findByAddress(context, withoutQuery.toString(), projection);

            // Take a copy of this, because the result of getPathSegments() is an AbstractList
            // which doesn't support the remove() operation.
            List<String> pathSegments = new ArrayList<>(withoutQuery.getPathSegments());

            boolean haveTriedWithoutPath = false;
            while (repo == null && !haveTriedWithoutPath) {
                if (pathSegments.size() == 0) {
                    haveTriedWithoutPath = true;
                } else {
                    pathSegments.remove(pathSegments.size() - 1);
                    withoutQuery = withoutQuery.buildUpon().path(TextUtils.join("/", pathSegments)).build();
                }
                repo = findByAddress(context, withoutQuery.toString(), projection);
            }
            return repo;
        }

        public static Repo findByAddress(Context context, String address) {
            return findByAddress(context, address, Cols.ALL);
        }

        public static Repo findByAddress(Context context,
                                         String address, String[] projection) {
            List<Repo> repos = findBy(
                    context, Cols.ADDRESS, address, projection);
            return repos.size() > 0 ? repos.get(0) : null;
        }

        public static List<Repo> all(Context context) {
            return all(context, Cols.ALL);
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
            final String[] args = {fieldValue};
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

        /**
         * Updates the repo metadata in the database.  All data comes from the
         * index file except {@link Repo#id}, which is generated by the database.
         * That makes for an two cycle workflow, where first this must be called
         * to fetch the {@code Repo.id} from the database, then it is called a
         * second time to actually set the repo metadata.
         */
        public static void update(Context context, Repo repo, ContentValues values) {
            ContentResolver resolver = context.getContentResolver();

            // Change the name to the new address. Next time we update the repo
            // index file, it will populate the name field with the proper
            // name, but the best we can do is guess right now.
            if (values.containsKey(Cols.ADDRESS) &&
                    !values.containsKey(Cols.NAME)) {
                String name = Repo.addressToName(values.getAsString(Cols.ADDRESS));
                values.put(Cols.NAME, name);
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
            if (values.containsKey(Cols.SIGNING_CERT)) {
                String publicKey = values.getAsString(Cols.SIGNING_CERT);
                String calcedFingerprint = Utils.calcFingerprint(publicKey);
                if (values.containsKey(Cols.FINGERPRINT)) {
                    String fingerprint = values.getAsString(Cols.FINGERPRINT);
                    if (!TextUtils.isEmpty(publicKey)) {
                        if (TextUtils.isEmpty(fingerprint)) {
                            values.put(Cols.FINGERPRINT, calcedFingerprint);
                        } else if (!fingerprint.equals(calcedFingerprint)) {
                            // TODO the UI should represent this error!
                            Log.e(TAG, "The stored and calculated fingerprints do not match!");
                            Log.e(TAG, "Stored: " + fingerprint);
                            Log.e(TAG, "Calculated: " + calcedFingerprint);
                        }
                    }
                } else if (!TextUtils.isEmpty(publicKey)) {
                    // no fingerprint in 'values', so put one there
                    values.put(Cols.FINGERPRINT, calcedFingerprint);
                }
            }

            if (values.containsKey(Cols.IN_USE)) {
                Integer inUse = values.getAsInteger(Cols.IN_USE);
                if (inUse != null && inUse == 0) {
                    values.put(Cols.LAST_ETAG, (String) null);
                }
            }

            final Uri uri = getContentUri(repo.getId());
            final String[] args = {Long.toString(repo.getId())};
            resolver.update(uri, values, Cols._ID + " = ?", args);
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

        public static void purgeApps(Context context, Repo repo) {
            Uri apkUri = ApkProvider.getRepoUri(repo.getId());
            ContentResolver resolver = context.getContentResolver();
            int apkCount = resolver.delete(apkUri, null, null);
            Utils.debugLog(TAG, "Removed " + apkCount + " apks from repo " + repo.name);

            Uri appUri = AppProvider.getRepoUri(repo);
            int appCount = resolver.delete(appUri, null, null);
            Utils.debugLog(TAG, "Removed " + appCount + " apps from repo " + repo.address + ".");

            AppUpdateStatusManager.getInstance(context).removeAllByRepo(repo);

            AppProvider.Helper.recalculatePreferredMetadata(context);
        }

        public static int countAppsForRepo(Context context, long repoId) {
            ContentResolver resolver = context.getContentResolver();
            final String[] projection = {Schema.ApkTable.Cols._COUNT_DISTINCT};
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

        @Nullable
        public static Date lastUpdate(Context context) {
            ContentResolver resolver = context.getContentResolver();
            final String[] projection = {Cols.LAST_UPDATED};
            final String selection = Cols.IN_USE + " = 1";
            Cursor cursor = resolver.query(getContentUri(), projection,
                    selection, null, Cols.LAST_UPDATED + " DESC");

            Date lastUpdate = null;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    lastUpdate = Utils.parseDate(cursor.getString(0), null);
                }
                cursor.close();
            }

            return lastUpdate;
        }

        public static int countEnabledRepos(Context context) {
            ContentResolver resolver = context.getContentResolver();
            final String[] projection = {Cols._ID};
            final String selection = Cols.IN_USE + " = 1";
            Cursor cursor = resolver.query(getContentUri(), projection, selection, null, null);

            int count = 0;
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }

            return count;
        }

        /**
         * Helper method to ensure that next time the user asks for a repository update, we will
         * fetch the metadata and update regardless of whether the metadata has changed or not.
         * This is useful for when we change languages, because we need to ask the user to fetch
         * the metadata again, so that we can extract the correctly-localized metadata.
         */
        public static void clearEtags(Context context) {
            ContentValues values = new ContentValues(1);
            values.put(Cols.LAST_ETAG, (String) null);
            context.getContentResolver().update(getContentUri(), values, null, null);
        }
    }

    private static final String PROVIDER_NAME = "RepoProvider";
    private static final String PATH_ALL_EXCEPT_SWAP = "allExceptSwap";

    private static final int CODE_ALL_EXCEPT_SWAP = CODE_SINGLE + 1;

    private static final UriMatcher MATCHER = new UriMatcher(-1);

    static {
        MATCHER.addURI(AUTHORITY + "." + PROVIDER_NAME, null, CODE_LIST);
        MATCHER.addURI(AUTHORITY + "." + PROVIDER_NAME, PATH_ALL_EXCEPT_SWAP, CODE_ALL_EXCEPT_SWAP);
        MATCHER.addURI(AUTHORITY + "." + PROVIDER_NAME, "#", CODE_SINGLE);
    }

    public static String getAuthority() {
        return AUTHORITY + "." + PROVIDER_NAME;
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
        return RepoTable.NAME;
    }

    @Override
    protected String getProviderName() {
        return "RepoProvider";
    }

    @Override
    protected UriMatcher getMatcher() {
        return MATCHER;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
                        String selection, String[] selectionArgs, String sortOrder) {

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = Cols.PRIORITY + " ASC";
        }

        switch (MATCHER.match(uri)) {
            case CODE_LIST:
                // Do nothing (don't restrict query)
                break;

            case CODE_SINGLE:
                selection = (selection == null ? "" : selection + " AND ") +
                        Cols._ID + " = " + uri.getLastPathSegment();
                break;

            case CODE_ALL_EXCEPT_SWAP:
                selection = "COALESCE(" + Cols.IS_SWAP + ", 0) = 0 ";
                break;

            default:
                Log.e(TAG, "Invalid URI for repo content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for repo content provider: " + uri);
        }

        Cursor cursor = db().query(getTableName(), projection,
                selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        // Don't let people specify arbitrary priorities. Instead, we are responsible
        // for making sure that newly created repositories by default have the highest priority.
        values.put(Cols.PRIORITY, getMaxPriority() + 1);

        if (!values.containsKey(Cols.ADDRESS)) {
            throw new UnsupportedOperationException("Cannot add repo without an address.");
        }

        // The following fields have NOT NULL constraints in the DB, so need
        // to be present.

        if (!values.containsKey(Cols.IN_USE)) {
            values.put(Cols.IN_USE, 1);
        }

        if (!values.containsKey(Cols.MAX_AGE)) {
            values.put(Cols.MAX_AGE, 0);
        }

        if (!values.containsKey(Cols.VERSION)) {
            values.put(Cols.VERSION, 0);
        }

        if (!values.containsKey(Cols.NAME) || values.get(Cols.NAME) == null) {
            final String address = values.getAsString(Cols.ADDRESS);
            values.put(Cols.NAME, Repo.addressToName(address));
        }

        long id = db().insertOrThrow(getTableName(), null, values);
        Utils.debugLog(TAG, "Inserted repo. Notifying provider change: '" + uri + "'.");
        getContext().getContentResolver().notifyChange(uri, null);
        return getContentUri(id);
    }

    private int getMaxPriority() {
        Cursor cursor = db().query(RepoTable.NAME, new String[]{"MAX(" + Cols.PRIORITY + ")"},
                "COALESCE(" + Cols.IS_SWAP + ", 0) = 0", null, null, null, null);
        cursor.moveToFirst();
        int max = cursor.getInt(0);
        cursor.close();
        return max;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        QuerySelection selection = new QuerySelection(where, whereArgs);
        switch (MATCHER.match(uri)) {
            case CODE_LIST:
                // Don't support deleting of multiple repos.
                return 0;

            case CODE_SINGLE:
                selection = selection.add(Cols._ID + " = ?", new String[]{uri.getLastPathSegment()});
                break;

            default:
                Log.e(TAG, "Invalid URI for repo content provider: " + uri);
                throw new UnsupportedOperationException("Invalid URI for repo content provider: " + uri);
        }

        int rowsAffected = db().delete(getTableName(), selection.getSelection(), selection.getArgs());
        Utils.debugLog(TAG, "Deleted repo. Notifying provider change: '" + uri + "'.");
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        // When the priority of a repo changes, we need to update the "preferred metadata" foreign
        // key in the package table to point to the best possible record in the app metadata table.
        // The full list of times when we need to recalculate the preferred metadata includes:
        //  * After the priority of a repo changes
        //  * After a repo is disabled
        //  * After a repo is enabled
        //  * After an update is performed
        // This code only checks for the priority changing. All other occasions we can't do the
        // recalculation right now, because we likely haven't added/removed the relevant apps
        // from the metadata table yet. Usually the repo details are updated, then a request is
        // made to do the heavier work (e.g. a repo update to get new list of apps from server).
        // After the heavier work is complete, then that process can request the preferred metadata
        // to be recalculated.
        boolean priorityChanged = false;
        if (values.containsKey(Cols.PRIORITY)) {
            Cursor priorityCursor = db().query(getTableName(), new String[]{Cols.PRIORITY},
                    where, whereArgs, null, null, null);
            if (priorityCursor.getCount() > 0) {
                priorityCursor.moveToFirst();
                int oldPriority = priorityCursor.getInt(priorityCursor.getColumnIndex(Cols.PRIORITY));
                priorityChanged = oldPriority != values.getAsInteger(Cols.PRIORITY);
            }
            priorityCursor.close();
        }

        int numRows = db().update(getTableName(), values, where, whereArgs);

        if (priorityChanged) {
            AppProvider.Helper.recalculatePreferredMetadata(getContext());
        }

        Utils.debugLog(TAG, "Updated repo. Notifying provider change: '" + uri + "'.");
        getContext().getContentResolver().notifyChange(uri, null);
        return numRows;
    }
}
