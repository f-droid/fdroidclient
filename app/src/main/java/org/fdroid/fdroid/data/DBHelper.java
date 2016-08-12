package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.AppPrefsTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.fdroid.fdroid.data.Schema.RepoTable;

import java.util.ArrayList;
import java.util.List;

class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";

    private static final String DATABASE_NAME = "fdroid";

    private static final String CREATE_TABLE_REPO = "create table "
            + RepoTable.NAME + " ("
            + RepoTable.Cols._ID + " integer primary key, "
            + RepoTable.Cols.ADDRESS + " text not null, "
            + RepoTable.Cols.NAME + " text, "
            + RepoTable.Cols.DESCRIPTION + " text, "
            + RepoTable.Cols.IN_USE + " integer not null, "
            + RepoTable.Cols.PRIORITY + " integer not null, "
            + RepoTable.Cols.SIGNING_CERT + " text, "
            + RepoTable.Cols.FINGERPRINT + " text, "
            + RepoTable.Cols.MAX_AGE + " integer not null default 0, "
            + RepoTable.Cols.VERSION + " integer not null default 0, "
            + RepoTable.Cols.LAST_ETAG + " text, "
            + RepoTable.Cols.LAST_UPDATED + " string,"
            + RepoTable.Cols.IS_SWAP + " integer boolean default 0,"
            + RepoTable.Cols.USERNAME + " string, "
            + RepoTable.Cols.PASSWORD + " string,"
            + RepoTable.Cols.TIMESTAMP + " integer not null default 0"
            + ");";

    private static final String CREATE_TABLE_APK =
            "CREATE TABLE " + ApkTable.NAME + " ( "
            + ApkTable.Cols.APP_ID + " integer not null, "
            + ApkTable.Cols.VERSION_NAME + " text not null, "
            + ApkTable.Cols.REPO_ID + " integer not null, "
            + ApkTable.Cols.HASH + " text not null, "
            + ApkTable.Cols.VERSION_CODE + " int not null,"
            + ApkTable.Cols.NAME + " text not null, "
            + ApkTable.Cols.SIZE + " int not null, "
            + ApkTable.Cols.SIGNATURE + " string, "
            + ApkTable.Cols.SOURCE_NAME + " string, "
            + ApkTable.Cols.MIN_SDK_VERSION + " integer, "
            + ApkTable.Cols.TARGET_SDK_VERSION + " integer, "
            + ApkTable.Cols.MAX_SDK_VERSION + " integer, "
            + ApkTable.Cols.PERMISSIONS + " string, "
            + ApkTable.Cols.FEATURES + " string, "
            + ApkTable.Cols.NATIVE_CODE + " string, "
            + ApkTable.Cols.HASH_TYPE + " string, "
            + ApkTable.Cols.ADDED_DATE + " string, "
            + ApkTable.Cols.IS_COMPATIBLE + " int not null, "
            + ApkTable.Cols.INCOMPATIBLE_REASONS + " text, "
            + "PRIMARY KEY (" + ApkTable.Cols.APP_ID + ", " + ApkTable.Cols.VERSION_CODE + ", " + ApkTable.Cols.REPO_ID + ")"
            + ");";

    private static final String CREATE_TABLE_APP = "CREATE TABLE " + AppMetadataTable.NAME
            + " ( "
            + AppMetadataTable.Cols.PACKAGE_NAME + " text not null, "
            + AppMetadataTable.Cols.NAME + " text not null, "
            + AppMetadataTable.Cols.SUMMARY + " text not null, "
            + AppMetadataTable.Cols.ICON + " text, "
            + AppMetadataTable.Cols.DESCRIPTION + " text not null, "
            + AppMetadataTable.Cols.LICENSE + " text not null, "
            + AppMetadataTable.Cols.AUTHOR + " text, "
            + AppMetadataTable.Cols.EMAIL + " text, "
            + AppMetadataTable.Cols.WEB_URL + " text, "
            + AppMetadataTable.Cols.TRACKER_URL + " text, "
            + AppMetadataTable.Cols.SOURCE_URL + " text, "
            + AppMetadataTable.Cols.CHANGELOG_URL + " text, "
            + AppMetadataTable.Cols.SUGGESTED_VERSION_CODE + " text,"
            + AppMetadataTable.Cols.UPSTREAM_VERSION_NAME + " text,"
            + AppMetadataTable.Cols.UPSTREAM_VERSION_CODE + " integer,"
            + AppMetadataTable.Cols.ANTI_FEATURES + " string,"
            + AppMetadataTable.Cols.DONATE_URL + " string,"
            + AppMetadataTable.Cols.BITCOIN_ADDR + " string,"
            + AppMetadataTable.Cols.LITECOIN_ADDR + " string,"
            + AppMetadataTable.Cols.FLATTR_ID + " string,"
            + AppMetadataTable.Cols.REQUIREMENTS + " string,"
            + AppMetadataTable.Cols.CATEGORIES + " string,"
            + AppMetadataTable.Cols.ADDED + " string,"
            + AppMetadataTable.Cols.LAST_UPDATED + " string,"
            + AppMetadataTable.Cols.IS_COMPATIBLE + " int not null,"
            + AppMetadataTable.Cols.ICON_URL + " text, "
            + AppMetadataTable.Cols.ICON_URL_LARGE + " text, "
            + "primary key(" + AppMetadataTable.Cols.PACKAGE_NAME + "));";

    private static final String CREATE_TABLE_APP_PREFS = "CREATE TABLE " + AppPrefsTable.NAME
            + " ( "
            + AppPrefsTable.Cols.PACKAGE_NAME + " TEXT, "
            + AppPrefsTable.Cols.IGNORE_THIS_UPDATE + " INT BOOLEAN NOT NULL, "
            + AppPrefsTable.Cols.IGNORE_ALL_UPDATES + " INT NOT NULL "
            + " );";

    private static final String CREATE_TABLE_INSTALLED_APP = "CREATE TABLE " + InstalledAppTable.NAME
            + " ( "
            + InstalledAppTable.Cols.PACKAGE_NAME + " TEXT NOT NULL PRIMARY KEY, "
            + InstalledAppTable.Cols.VERSION_CODE + " INT NOT NULL, "
            + InstalledAppTable.Cols.VERSION_NAME + " TEXT NOT NULL, "
            + InstalledAppTable.Cols.APPLICATION_LABEL + " TEXT NOT NULL, "
            + InstalledAppTable.Cols.SIGNATURE + " TEXT NOT NULL, "
            + InstalledAppTable.Cols.LAST_UPDATE_TIME + " INTEGER NOT NULL DEFAULT 0, "
            + InstalledAppTable.Cols.HASH_TYPE + " TEXT NOT NULL, "
            + InstalledAppTable.Cols.HASH + " TEXT NOT NULL"
            + " );";
    private static final String DROP_TABLE_INSTALLED_APP = "DROP TABLE " + InstalledAppTable.NAME + ";";

    private static final int DB_VERSION = 61;

    private final Context context;

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
        this.context = context;
    }

    private void populateRepoNames(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 37) {
            return;
        }
        Utils.debugLog(TAG, "Populating repo names from the url");
        final String[] columns = {RepoTable.Cols.ADDRESS, RepoTable.Cols._ID};
        Cursor cursor = db.query(RepoTable.NAME, columns,
                RepoTable.Cols.NAME + " IS NULL OR " + RepoTable.Cols.NAME + " = ''", null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    String address = cursor.getString(0);
                    long id = cursor.getInt(1);
                    ContentValues values = new ContentValues(1);
                    String name = Repo.addressToName(address);
                    values.put(RepoTable.Cols.NAME, name);
                    final String[] args = {Long.toString(id)};
                    Utils.debugLog(TAG, "Setting repo name to '" + name + "' for repo " + address);
                    db.update(RepoTable.NAME, values, RepoTable.Cols._ID + " = ?", args);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
    }

    private void renameRepoId(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 36 || columnExists(db, RepoTable.NAME, RepoTable.Cols._ID)) {
            return;
        }

        Utils.debugLog(TAG, "Renaming " + RepoTable.NAME + ".id to " + RepoTable.Cols._ID);
        db.beginTransaction();

        try {
            // http://stackoverflow.com/questions/805363/how-do-i-rename-a-column-in-a-sqlite-database-table#805508
            String tempTableName = RepoTable.NAME + "__temp__";
            db.execSQL("ALTER TABLE " + RepoTable.NAME + " RENAME TO " + tempTableName + ";");

            // I realise this is available in the CREATE_TABLE_REPO above,
            // however I have a feeling that it will need to be the same as the
            // current structure of the table as of DBVersion 36, or else we may
            // get into strife. For example, if there was a field that
            // got removed, then it will break the "insert select"
            // statement. Therefore, I've put a copy of CREATE_TABLE_REPO
            // here that is the same as it was at DBVersion 36.
            String createTableDdl = "create table " + RepoTable.NAME + " ("
                    + RepoTable.Cols._ID + " integer not null primary key, "
                    + RepoTable.Cols.ADDRESS + " text not null, "
                    + RepoTable.Cols.NAME + " text, "
                    + RepoTable.Cols.DESCRIPTION + " text, "
                    + RepoTable.Cols.IN_USE + " integer not null, "
                    + RepoTable.Cols.PRIORITY + " integer not null, "
                    + RepoTable.Cols.SIGNING_CERT + " text, "
                    + RepoTable.Cols.FINGERPRINT + " text, "
                    + RepoTable.Cols.MAX_AGE + " integer not null default 0, "
                    + RepoTable.Cols.VERSION + " integer not null default 0, "
                    + RepoTable.Cols.LAST_ETAG + " text, "
                    + RepoTable.Cols.LAST_UPDATED + " string);";

            db.execSQL(createTableDdl);

            String nonIdFields = TextUtils.join(", ", new String[] {
                    RepoTable.Cols.ADDRESS,
                    RepoTable.Cols.NAME,
                    RepoTable.Cols.DESCRIPTION,
                    RepoTable.Cols.IN_USE,
                    RepoTable.Cols.PRIORITY,
                    RepoTable.Cols.SIGNING_CERT,
                    RepoTable.Cols.FINGERPRINT,
                    RepoTable.Cols.MAX_AGE,
                    RepoTable.Cols.VERSION,
                    RepoTable.Cols.LAST_ETAG,
                    RepoTable.Cols.LAST_UPDATED,
            });

            String insertSql = "INSERT INTO " + RepoTable.NAME +
                    "(" + RepoTable.Cols._ID + ", " + nonIdFields + " ) " +
                    "SELECT id, " + nonIdFields + " FROM " + tempTableName + ";";

            db.execSQL(insertSql);
            db.execSQL("DROP TABLE " + tempTableName + ";");
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error renaming id to " + RepoTable.Cols._ID, e);
        }
        db.endTransaction();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE_APP);
        db.execSQL(CREATE_TABLE_APK);
        db.execSQL(CREATE_TABLE_INSTALLED_APP);
        db.execSQL(CREATE_TABLE_REPO);
        db.execSQL(CREATE_TABLE_APP_PREFS);
        ensureIndexes(db);

        insertRepo(
                db,
                context.getString(R.string.fdroid_repo_name),
                context.getString(R.string.fdroid_repo_address),
                context.getString(R.string.fdroid_repo_description),
                context.getString(R.string.fdroid_repo_pubkey),
                context.getResources().getInteger(R.integer.fdroid_repo_version),
                context.getResources().getInteger(R.integer.fdroid_repo_inuse),
                context.getResources().getInteger(R.integer.fdroid_repo_priority)
        );

        insertRepo(
                db,
                context.getString(R.string.fdroid_archive_name),
                context.getString(R.string.fdroid_archive_address),
                context.getString(R.string.fdroid_archive_description),
                context.getString(R.string.fdroid_archive_pubkey),
                context.getResources().getInteger(R.integer.fdroid_archive_version),
                context.getResources().getInteger(R.integer.fdroid_archive_inuse),
                context.getResources().getInteger(R.integer.fdroid_archive_priority)
        );

        insertRepo(
                db,
                context.getString(R.string.guardianproject_repo_name),
                context.getString(R.string.guardianproject_repo_address),
                context.getString(R.string.guardianproject_repo_description),
                context.getString(R.string.guardianproject_repo_pubkey),
                context.getResources().getInteger(R.integer.guardianproject_repo_version),
                context.getResources().getInteger(R.integer.guardianproject_repo_inuse),
                context.getResources().getInteger(R.integer.guardianproject_repo_priority)
        );

        insertRepo(
                db,
                context.getString(R.string.guardianproject_archive_name),
                context.getString(R.string.guardianproject_archive_address),
                context.getString(R.string.guardianproject_archive_description),
                context.getString(R.string.guardianproject_archive_pubkey),
                context.getResources().getInteger(R.integer.guardianproject_archive_version),
                context.getResources().getInteger(R.integer.guardianproject_archive_inuse),
                context.getResources().getInteger(R.integer.guardianproject_archive_priority)
        );
    }

    private void insertRepo(SQLiteDatabase db, String name, String address,
            String description, String pubKey, int version, int inUse,
            int priority) {

        ContentValues values = new ContentValues();
        values.put(RepoTable.Cols.ADDRESS, address);
        values.put(RepoTable.Cols.NAME, name);
        values.put(RepoTable.Cols.DESCRIPTION, description);
        values.put(RepoTable.Cols.SIGNING_CERT, pubKey);
        values.put(RepoTable.Cols.FINGERPRINT, Utils.calcFingerprint(pubKey));
        values.put(RepoTable.Cols.MAX_AGE, 0);
        values.put(RepoTable.Cols.VERSION, version);
        values.put(RepoTable.Cols.IN_USE, inUse);
        values.put(RepoTable.Cols.PRIORITY, priority);
        values.put(RepoTable.Cols.LAST_ETAG, (String) null);
        values.put(RepoTable.Cols.TIMESTAMP, 0);

        Utils.debugLog(TAG, "Adding repository " + name);
        db.insert(RepoTable.NAME, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Utils.debugLog(TAG, "Upgrading database from v" + oldVersion + " v" + newVersion);

        migrateRepoTable(db, oldVersion);

        // The other tables are transient and can just be reset. Do this after
        // the repo table changes though, because it also clears the lastetag
        // fields which didn't always exist.
        resetTransientPre42(db, oldVersion);

        addNameAndDescriptionToRepo(db, oldVersion);
        addFingerprintToRepo(db, oldVersion);
        addMaxAgeToRepo(db, oldVersion);
        addVersionToRepo(db, oldVersion);
        addLastUpdatedToRepo(db, oldVersion);
        renameRepoId(db, oldVersion);
        populateRepoNames(db, oldVersion);
        addIsSwapToRepo(db, oldVersion);
        addChangelogToApp(db, oldVersion);
        addIconUrlLargeToApp(db, oldVersion);
        updateIconUrlLarge(db, oldVersion);
        addCredentialsToRepo(db, oldVersion);
        addAuthorToApp(db, oldVersion);
        useMaxValueInMaxSdkVersion(db, oldVersion);
        requireTimestampInRepos(db, oldVersion);
        recreateInstalledAppTable(db, oldVersion);
        addTargetSdkVersionToApk(db, oldVersion);
        migrateAppPrimaryKeyToRowId(db, oldVersion);
        removeApkPackageNameColumn(db, oldVersion);
        addAppPrefsTable(db, oldVersion);
        lowerCaseApkHashes(db, oldVersion);
    }

    private void lowerCaseApkHashes(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 61) {
            return;
        }
        Utils.debugLog(TAG, "Lowercasing all APK hashes");
        db.execSQL("UPDATE " + InstalledAppTable.NAME + " SET " + InstalledAppTable.Cols.HASH
                + " = lower(" + InstalledAppTable.Cols.HASH + ")");
    }

    private void addAppPrefsTable(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 60) {
            return;
        }

        Utils.debugLog(TAG, "Creating app preferences table");
        db.execSQL(CREATE_TABLE_APP_PREFS);

        Utils.debugLog(TAG, "Migrating app preferences to separate table");
        db.execSQL(
                "INSERT INTO " + AppPrefsTable.NAME + " ("
                + AppPrefsTable.Cols.PACKAGE_NAME + ", "
                + AppPrefsTable.Cols.IGNORE_THIS_UPDATE + ", "
                + AppPrefsTable.Cols.IGNORE_ALL_UPDATES
                + ") SELECT "
                + AppMetadataTable.Cols.PACKAGE_NAME + ", "
                + "ignoreThisUpdate, "
                + "ignoreAllUpdates "
                + "FROM " + AppMetadataTable.NAME + " "
                + "WHERE ignoreThisUpdate > 0 OR ignoreAllUpdates > 0"
        );

        resetTransient(db);
    }

    /**
     * Ordinarily, if a column is no longer used, we'd err on the side of just leaving it in the
     * database but stop referring to it in Java. However because it forms part of the primary
     * key of this table, we need to change the primary key to something which _is_ used. Thus,
     * this function will rename the old table, create the new table, and then insert all of the
     * data from the old into the new with the new primary key.
     */
    private void removeApkPackageNameColumn(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 59) {

            Utils.debugLog(TAG, "Changing primary key of " + ApkTable.NAME + " from package + vercode to app + vercode + repo");
            db.beginTransaction();

            try {
                // http://stackoverflow.com/questions/805363/how-do-i-rename-a-column-in-a-sqlite-database-table#805508
                String tempTableName = ApkTable.NAME + "__temp__";
                db.execSQL("ALTER TABLE " + ApkTable.NAME + " RENAME TO " + tempTableName + ";");

                String createTableDdl = "CREATE TABLE " + ApkTable.NAME + " ( "
                        + ApkTable.Cols.APP_ID + " integer not null, "
                        + ApkTable.Cols.VERSION_NAME + " text not null, "
                        + ApkTable.Cols.REPO_ID + " integer not null, "
                        + ApkTable.Cols.HASH + " text not null, "
                        + ApkTable.Cols.VERSION_CODE + " int not null,"
                        + ApkTable.Cols.NAME + " text not null, "
                        + ApkTable.Cols.SIZE + " int not null, "
                        + ApkTable.Cols.SIGNATURE + " string, "
                        + ApkTable.Cols.SOURCE_NAME + " string, "
                        + ApkTable.Cols.MIN_SDK_VERSION + " integer, "
                        + ApkTable.Cols.TARGET_SDK_VERSION + " integer, "
                        + ApkTable.Cols.MAX_SDK_VERSION + " integer, "
                        + ApkTable.Cols.PERMISSIONS + " string, "
                        + ApkTable.Cols.FEATURES + " string, "
                        + ApkTable.Cols.NATIVE_CODE + " string, "
                        + ApkTable.Cols.HASH_TYPE + " string, "
                        + ApkTable.Cols.ADDED_DATE + " string, "
                        + ApkTable.Cols.IS_COMPATIBLE + " int not null, "
                        + ApkTable.Cols.INCOMPATIBLE_REASONS + " text, "
                        + "PRIMARY KEY (" + ApkTable.Cols.APP_ID + ", " + ApkTable.Cols.VERSION_CODE + ", " + ApkTable.Cols.REPO_ID + ")"
                        + ");";

                db.execSQL(createTableDdl);

                String nonPackageNameFields = TextUtils.join(", ", new String[] {
                        ApkTable.Cols.APP_ID,
                        ApkTable.Cols.VERSION_NAME,
                        ApkTable.Cols.REPO_ID,
                        ApkTable.Cols.HASH,
                        ApkTable.Cols.VERSION_CODE,
                        ApkTable.Cols.NAME,
                        ApkTable.Cols.SIZE,
                        ApkTable.Cols.SIGNATURE,
                        ApkTable.Cols.SOURCE_NAME,
                        ApkTable.Cols.MIN_SDK_VERSION,
                        ApkTable.Cols.TARGET_SDK_VERSION,
                        ApkTable.Cols.MAX_SDK_VERSION,
                        ApkTable.Cols.PERMISSIONS,
                        ApkTable.Cols.FEATURES,
                        ApkTable.Cols.NATIVE_CODE,
                        ApkTable.Cols.HASH_TYPE,
                        ApkTable.Cols.ADDED_DATE,
                        ApkTable.Cols.IS_COMPATIBLE,
                        ApkTable.Cols.INCOMPATIBLE_REASONS,
                });

                String insertSql = "INSERT INTO " + ApkTable.NAME +
                        "(" + nonPackageNameFields + " ) " +
                        "SELECT " + nonPackageNameFields + " FROM " + tempTableName + ";";

                db.execSQL(insertSql);
                db.execSQL("DROP TABLE " + tempTableName + ";");

                // Now that the old table has been dropped, we can create indexes again.
                // Attempting this before dropping the old table will not work, because the
                // indexes exist on the _old_ table, and so are unable to be added (with the
                // same name) to the _new_ table.
                ensureIndexes(db);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private void migrateAppPrimaryKeyToRowId(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 58 && !columnExists(db, ApkTable.NAME, ApkTable.Cols.APP_ID)) {
            db.beginTransaction();
            try {
                final String alter = "ALTER TABLE " + ApkTable.NAME + " ADD COLUMN " + ApkTable.Cols.APP_ID + " NUMERIC";
                Log.i(TAG, "Adding appId foreign key to " + ApkTable.NAME);
                Utils.debugLog(TAG, alter);
                db.execSQL(alter);

                // Hard coded the string literal ".id" as ApkTable.Cols.PACKAGE_NAME was removed in
                // the subsequent migration (DB_VERSION 59)
                final String update = "UPDATE " + ApkTable.NAME + " SET " + ApkTable.Cols.APP_ID + " = ( " +
                        "SELECT app." + AppMetadataTable.Cols.ROW_ID + " " +
                        "FROM " + AppMetadataTable.NAME + " AS app " +
                        "WHERE " + ApkTable.NAME + ".id = app." + AppMetadataTable.Cols.PACKAGE_NAME + ")";
                Log.i(TAG, "Updating foreign key from " + ApkTable.NAME + " to " + AppMetadataTable.NAME + " to use numeric foreign key.");
                Utils.debugLog(TAG, update);
                db.execSQL(update);
                ensureIndexes(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }


    /**
     * Migrate repo list to new structure. (No way to change primary
     * key in sqlite - table must be recreated).
     */
    private void migrateRepoTable(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 20) {
            return;
        }
        List<Repo> oldrepos = new ArrayList<>();
        Cursor cursor = db.query(RepoTable.NAME,
                new String[] {RepoTable.Cols.ADDRESS, RepoTable.Cols.IN_USE, RepoTable.Cols.SIGNING_CERT},
                null, null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    Repo repo = new Repo();
                    repo.address = cursor.getString(0);
                    repo.inuse = cursor.getInt(1) == 1;
                    repo.signingCertificate = cursor.getString(2);
                    oldrepos.add(repo);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        db.execSQL("drop table " + RepoTable.NAME);
        db.execSQL(CREATE_TABLE_REPO);
        for (final Repo repo : oldrepos) {
            ContentValues values = new ContentValues();
            values.put(RepoTable.Cols.ADDRESS, repo.address);
            values.put(RepoTable.Cols.IN_USE, repo.inuse);
            values.put(RepoTable.Cols.PRIORITY, 10);
            values.put(RepoTable.Cols.SIGNING_CERT, repo.signingCertificate);
            values.put(RepoTable.Cols.LAST_ETAG, (String) null);
            db.insert(RepoTable.NAME, null, values);
        }
    }

    private void insertNameAndDescription(SQLiteDatabase db,
            int addressResId, int nameResId, int descriptionResId) {
        ContentValues values = new ContentValues();
        values.clear();
        values.put(RepoTable.Cols.NAME, context.getString(nameResId));
        values.put(RepoTable.Cols.DESCRIPTION, context.getString(descriptionResId));
        db.update(RepoTable.NAME, values, RepoTable.Cols.ADDRESS + " = ?", new String[] {
                context.getString(addressResId),
        });
    }

    /**
     * Add a name and description to the repo table, and updates the two
     * default repos with values from strings.xml.
     */
    private void addNameAndDescriptionToRepo(SQLiteDatabase db, int oldVersion) {
        boolean nameExists = columnExists(db, RepoTable.NAME, RepoTable.Cols.NAME);
        boolean descriptionExists = columnExists(db, RepoTable.NAME, RepoTable.Cols.DESCRIPTION);
        if (oldVersion >= 21 || (nameExists && descriptionExists)) {
            return;
        }
        if (!nameExists) {
            db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.NAME + " text");
        }
        if (!descriptionExists) {
            db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.DESCRIPTION + " text");
        }
        insertNameAndDescription(db, R.string.fdroid_repo_address,
                R.string.fdroid_repo_name, R.string.fdroid_repo_description);
        insertNameAndDescription(db, R.string.fdroid_archive_address,
                R.string.fdroid_archive_name, R.string.fdroid_archive_description);
        insertNameAndDescription(db, R.string.guardianproject_repo_address,
                R.string.guardianproject_repo_name, R.string.guardianproject_repo_description);
        insertNameAndDescription(db, R.string.guardianproject_archive_address,
                R.string.guardianproject_archive_name, R.string.guardianproject_archive_description);

    }

    /**
     * Add a fingerprint field to repos. For any field with a public key,
     * calculate its fingerprint and save it to the database.
     */
    private void addFingerprintToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 44) {
            return;
        }
        if (!columnExists(db, RepoTable.NAME, RepoTable.Cols.FINGERPRINT)) {
            db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.FINGERPRINT + " text");
        }
        List<Repo> oldrepos = new ArrayList<>();
        Cursor cursor = db.query(RepoTable.NAME,
                new String[] {RepoTable.Cols.ADDRESS, RepoTable.Cols.SIGNING_CERT},
                null, null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    Repo repo = new Repo();
                    repo.address = cursor.getString(0);
                    repo.signingCertificate = cursor.getString(1);
                    oldrepos.add(repo);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        for (final Repo repo : oldrepos) {
            ContentValues values = new ContentValues();
            values.put(RepoTable.Cols.FINGERPRINT, Utils.calcFingerprint(repo.signingCertificate));
            db.update(RepoTable.NAME, values, RepoTable.Cols.ADDRESS + " = ?", new String[] {repo.address});
        }
    }

    private void addMaxAgeToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 30 || columnExists(db, RepoTable.NAME, RepoTable.Cols.MAX_AGE)) {
            return;
        }
        db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.MAX_AGE + " integer not null default 0");
    }

    private void addVersionToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 33 || columnExists(db, RepoTable.NAME, RepoTable.Cols.VERSION)) {
            return;
        }
        db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.VERSION + " integer not null default 0");
    }

    private void addLastUpdatedToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 35 || columnExists(db, RepoTable.NAME, RepoTable.Cols.LAST_UPDATED)) {
            return;
        }
        Utils.debugLog(TAG, "Adding " + RepoTable.Cols.LAST_UPDATED + " column to " + RepoTable.NAME);
        db.execSQL("Alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.LAST_UPDATED + " string");
    }

    private void addIsSwapToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 47 || columnExists(db, RepoTable.NAME, RepoTable.Cols.IS_SWAP)) {
            return;
        }
        Utils.debugLog(TAG, "Adding " + RepoTable.Cols.IS_SWAP + " field to " + RepoTable.NAME + " table in db.");
        db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.IS_SWAP + " boolean default 0;");
    }

    private void addCredentialsToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 52) {
            return;
        }
        if (!columnExists(db, Schema.RepoTable.NAME, RepoTable.Cols.USERNAME)) {
            Utils.debugLog(TAG, "Adding " + RepoTable.Cols.USERNAME + " field to " + RepoTable.NAME + " table in db.");
            db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.USERNAME + " string;");
        }

        if (!columnExists(db, RepoTable.NAME, RepoTable.Cols.PASSWORD)) {
            Utils.debugLog(TAG, "Adding " + RepoTable.Cols.PASSWORD + " field to " + RepoTable.NAME + " table in db.");
            db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.PASSWORD + " string;");
        }
    }

    private void addChangelogToApp(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 48 || columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.CHANGELOG_URL)) {
            return;
        }
        Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.CHANGELOG_URL + " column to " + AppMetadataTable.NAME);
        db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.CHANGELOG_URL + " text");
    }

    private void addIconUrlLargeToApp(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 49 || columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.ICON_URL_LARGE)) {
            return;
        }
        Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.ICON_URL_LARGE + " columns to " + AppMetadataTable.NAME);
        db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.ICON_URL_LARGE + " text");
    }

    private void updateIconUrlLarge(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 50) {
            return;
        }
        Utils.debugLog(TAG, "Recalculating app icon URLs so that the newly added large icons will get updated.");
        AppProvider.UpgradeHelper.updateIconUrls(context, db);
        clearRepoEtags(db);
    }

    private void addAuthorToApp(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 53) {
            return;
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.AUTHOR)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.AUTHOR + " column to " + AppMetadataTable.NAME);
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.AUTHOR + " text");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.EMAIL)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.EMAIL + " column to " + AppMetadataTable.NAME);
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.EMAIL + " text");
        }
    }

    private void useMaxValueInMaxSdkVersion(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 54) {
            return;
        }
        Utils.debugLog(TAG, "Converting " + ApkTable.Cols.MAX_SDK_VERSION + " value 0 to " + Byte.MAX_VALUE);
        ContentValues values = new ContentValues();
        values.put(ApkTable.Cols.MAX_SDK_VERSION, Byte.MAX_VALUE);
        db.update(ApkTable.NAME, values, ApkTable.Cols.MAX_SDK_VERSION + " < 1", null);
    }

    /**
     * The {@code <repo timestamp="">} value was in the metadata for a long time,
     * but it was not being used in the client until now.
     */
    private void requireTimestampInRepos(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 55) {
            return;
        }
        if (!columnExists(db, RepoTable.NAME, RepoTable.Cols.TIMESTAMP)) {
            Utils.debugLog(TAG, "Adding " + RepoTable.Cols.TIMESTAMP + " column to " + RepoTable.NAME);
            db.execSQL("alter table " + RepoTable.NAME + " add column "
                    + RepoTable.Cols.TIMESTAMP + " integer not null default 0");
        }
    }

    /**
     * By clearing the etags stored in the repo table, it means that next time the user updates
     * their repos (either manually or on a scheduled task), they will update regardless of whether
     * they have changed since last update or not.
     */
    private static void clearRepoEtags(SQLiteDatabase db) {
        Utils.debugLog(TAG, "Clearing repo etags, so next update will not be skipped with \"Repos up to date\".");
        db.execSQL("update " + RepoTable.NAME + " set " + RepoTable.Cols.LAST_ETAG + " = NULL");
    }

    private void resetTransient(SQLiteDatabase db) {
        Utils.debugLog(TAG, "Removing app + apk tables so they can be recreated. Next time F-Droid updates it should trigger an index update.");
        context.getSharedPreferences("FDroid", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("triedEmptyUpdate", false)
                .apply();

        db.execSQL("DROP TABLE " + AppMetadataTable.NAME);
        db.execSQL("DROP TABLE " + ApkTable.NAME);
        db.execSQL(CREATE_TABLE_APP);
        db.execSQL(CREATE_TABLE_APK);
        clearRepoEtags(db);
        ensureIndexes(db);
    }

    private void resetTransientPre42(SQLiteDatabase db, int oldVersion) {
        // Before version 42, only transient info was stored in here. As of some time
        // just before 42 (F-Droid 0.60ish) it now has "ignore this version" info which
        // was is specified by the user. We don't want to weely-neely nuke that data.
        // and the new way to deal with changes to the table structure is to add a
        // if (oldVersion < x && !columnExists(...) and then alter the table as required.
        if (oldVersion >= 42) {
            return;
        }
        context.getSharedPreferences("FDroid", Context.MODE_PRIVATE).edit()
                .putBoolean("triedEmptyUpdate", false).apply();
        db.execSQL("drop table " + AppMetadataTable.NAME);
        db.execSQL("drop table " + ApkTable.NAME);
        clearRepoEtags(db);
        db.execSQL(CREATE_TABLE_APP);
        db.execSQL(CREATE_TABLE_APK);
        ensureIndexes(db);
    }

    private static void ensureIndexes(SQLiteDatabase db) {
        Utils.debugLog(TAG, "Ensuring indexes exist for " + AppMetadataTable.NAME);
        db.execSQL("CREATE INDEX IF NOT EXISTS app_id on " + AppMetadataTable.NAME + " (" + AppMetadataTable.Cols.PACKAGE_NAME + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS name on " + AppMetadataTable.NAME + " (" + AppMetadataTable.Cols.NAME + ");"); // Used for sorting most lists
        db.execSQL("CREATE INDEX IF NOT EXISTS added on " + AppMetadataTable.NAME + " (" + AppMetadataTable.Cols.ADDED + ");"); // Used for sorting "newly added"

        Utils.debugLog(TAG, "Ensuring indexes exist for " + ApkTable.NAME);
        db.execSQL("CREATE INDEX IF NOT EXISTS apk_vercode on " + ApkTable.NAME + " (" + ApkTable.Cols.VERSION_CODE + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS apk_appId on " + ApkTable.NAME + " (" + ApkTable.Cols.APP_ID + ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS repoId ON " + ApkTable.NAME + " (" + ApkTable.Cols.REPO_ID + ");");

        if (tableExists(db, AppPrefsTable.NAME)) {
            Utils.debugLog(TAG, "Ensuring indexes exist for " + AppPrefsTable.NAME);
            db.execSQL("CREATE INDEX IF NOT EXISTS appPrefs_packageName on " + AppPrefsTable.NAME + " (" + AppPrefsTable.Cols.PACKAGE_NAME + ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS appPrefs_packageName_ignoreAll_ignoreThis on " + AppPrefsTable.NAME + " (" +
                    AppPrefsTable.Cols.PACKAGE_NAME + ", " +
                    AppPrefsTable.Cols.IGNORE_ALL_UPDATES + ", " +
                    AppPrefsTable.Cols.IGNORE_THIS_UPDATE + ");");
        }

        Utils.debugLog(TAG, "Ensuring indexes exist for " + InstalledAppTable.NAME);
        db.execSQL("CREATE INDEX IF NOT EXISTS installedApp_appId_vercode on " + InstalledAppTable.NAME + " (" +
                InstalledAppTable.Cols.PACKAGE_NAME + ", " + InstalledAppTable.Cols.VERSION_CODE + ");");

        Utils.debugLog(TAG, "Ensuring indexes exist for " + RepoTable.NAME);
        db.execSQL("CREATE INDEX IF NOT EXISTS repo_id_isSwap on " + RepoTable.NAME + " (" +
                RepoTable.Cols._ID + ", " + RepoTable.Cols.IS_SWAP + ");");
    }

    /**
     * If any column was added or removed, just drop the table, create it again
     * and let the cache be filled from scratch by {@link InstalledAppProviderService}
     * For DB versions older than 43, this will create the {@link InstalledAppProvider}
     * table for the first time.
     */
    private void recreateInstalledAppTable(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 56) {
            return;
        }
        Utils.debugLog(TAG, "(re)creating 'installed app' database table.");
        db.execSQL(DROP_TABLE_INSTALLED_APP);
        db.execSQL(CREATE_TABLE_INSTALLED_APP);
    }

    private void addTargetSdkVersionToApk(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 57) {
            return;
        }
        Utils.debugLog(TAG, "Adding " + ApkTable.Cols.TARGET_SDK_VERSION
                + " columns to " + ApkTable.NAME);
        db.execSQL("alter table " + ApkTable.NAME + " add column "
                + ApkTable.Cols.TARGET_SDK_VERSION + " integer");
    }

    private static boolean columnExists(SQLiteDatabase db, String table, String column) {
        Cursor cursor = db.rawQuery("select * from " + table + " limit 0,1", null);
        boolean exists = cursor.getColumnIndex(column) != -1;
        cursor.close();
        return exists;
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        Cursor cursor = db.query("sqlite_master", new String[] {"name"},
                "type = 'table' AND name = ?", new String[] {table}, null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

}
