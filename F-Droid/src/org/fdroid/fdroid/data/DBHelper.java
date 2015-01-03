package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "org.fdroid.fdroid.data.DBHelper";

    public static final String DATABASE_NAME = "fdroid";

    public static final String TABLE_REPO = "fdroid_repo";

    // The TABLE_APK table stores details of all the application versions we
    // know about. Each relates directly back to an entry in TABLE_APP.
    // This information is retrieved from the repositories.
    public static final String TABLE_APK = "fdroid_apk";

    private static final String CREATE_TABLE_REPO = "create table "
            + TABLE_REPO + " (_id integer primary key, "
            + "address text not null, "
            + "name text, description text, inuse integer not null, "
            + "priority integer not null, pubkey text, fingerprint text, "
            + "maxage integer not null default 0, "
            + "version integer not null default 0, "
            + "lastetag text, lastUpdated string);";

    private static final String CREATE_TABLE_APK =
            "CREATE TABLE " + TABLE_APK + " ( "
            + "id text not null, "
            + "version text not null, "
            + "repo integer not null, "
            + "hash text not null, "
            + "vercode int not null,"
            + "apkName text not null, "
            + "size int not null, "
            + "sig string, "
            + "srcname string, "
            + "minSdkVersion integer, "
            + "maxSdkVersion integer, "
            + "permissions string, "
            + "features string, "
            + "nativecode string, "
            + "hashType string, "
            + "added string, "
            + "compatible int not null, "
            + "incompatibleReasons text, "
            + "primary key(id, vercode)"
            + ");";

    public static final String TABLE_APP = "fdroid_app";
    private static final String CREATE_TABLE_APP = "CREATE TABLE " + TABLE_APP
            + " ( "
            + "id text not null, "
            + "name text not null, "
            + "summary text not null, "
            + "icon text, "
            + "description text not null, "
            + "license text not null, "
            + "webURL text, "
            + "trackerURL text, "
            + "sourceURL text, "
            + "suggestedVercode text,"
            + "upstreamVersion text,"
            + "upstreamVercode integer,"
            + "antiFeatures string,"
            + "donateURL string,"
            + "bitcoinAddr string,"
            + "litecoinAddr string,"
            + "dogecoinAddr string,"
            + "flattrID string,"
            + "requirements string,"
            + "categories string,"
            + "added string,"
            + "lastUpdated string,"
            + "compatible int not null,"
            + "ignoreAllUpdates int not null,"
            + "ignoreThisUpdate int not null,"
            + "iconUrl text, "
            + "primary key(id));";

    public static final String TABLE_INSTALLED_APP = "fdroid_installedApp";
    private static final String CREATE_TABLE_INSTALLED_APP = "CREATE TABLE " + TABLE_INSTALLED_APP
            + " ( "
            + InstalledAppProvider.DataColumns.APP_ID + " TEXT NOT NULL PRIMARY KEY, "
            + InstalledAppProvider.DataColumns.VERSION_CODE + " INT NOT NULL, "
            + InstalledAppProvider.DataColumns.VERSION_NAME + " TEXT NOT NULL, "
            + InstalledAppProvider.DataColumns.APPLICATION_LABEL + " TEXT NOT NULL "
            + " );";

    private static final int DB_VERSION = 46;

    private Context context;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
        this.context = context;
    }

    private void populateRepoNames(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 37) {
            Log.i("FDroid", "Populating repo names from the url");
            String[] columns = { "address", "_id" };
            Cursor cursor = db.query(TABLE_REPO, columns,
                    "name IS NULL OR name = ''", null, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        String address = cursor.getString(0);
                        long id = cursor.getInt(1);
                        ContentValues values = new ContentValues(1);
                        String name = Repo.addressToName(address);
                        values.put("name", name);
                        String[] args = { Long.toString(id) };
                        Log.i("FDroid", "Setting repo name to '" + name + "' for repo " + address);
                        db.update(TABLE_REPO, values, "_id = ?", args);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
        }
    }

    private void renameRepoId(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 36 && !columnExists(db, TABLE_REPO, "_id")) {

            Log.d("FDroid", "Renaming " + TABLE_REPO + ".id to _id");
            db.beginTransaction();

            try {
                // http://stackoverflow.com/questions/805363/how-do-i-rename-a-column-in-a-sqlite-database-table#805508
                String tempTableName = TABLE_REPO + "__temp__";
                db.execSQL("ALTER TABLE " + TABLE_REPO + " RENAME TO " + tempTableName + ";" );

                // I realise this is available in the CREATE_TABLE_REPO above,
                // however I have a feeling that it will need to be the same as the
                // current structure of the table as of DBVersion 36, or else we may
                // get into strife. For example, if there was a field that
                // got removed, then it will break the "insert select"
                // statement. Therefore, I've put a copy of CREATE_TABLE_REPO
                // here that is the same as it was at DBVersion 36.
                String createTableDdl = "create table " + TABLE_REPO + " ("
                        + "_id integer not null primary key, "
                        + "address text not null, "
                        + "name text, "
                        + "description text, "
                        + "inuse integer not null, "
                        + "priority integer not null, "
                        + "pubkey text, "
                        + "fingerprint text, "
                        + "maxage integer not null default 0, "
                        + "version integer not null default 0, "
                        + "lastetag text, "
                        + "lastUpdated string);";

                db.execSQL(createTableDdl);

                String nonIdFields = "address,  name, description, inuse, priority, " +
                        "pubkey, fingerprint, maxage, version, lastetag, lastUpdated";

                String insertSql = "INSERT INTO " + TABLE_REPO +
                        "(_id, " + nonIdFields + " ) " +
                        "SELECT id, " + nonIdFields + " FROM " + tempTableName + ";";

                db.execSQL(insertSql);
                db.execSQL("DROP TABLE " + tempTableName + ";");
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e("FDroid", "Error renaming id to _id: " + e.getMessage());
            }
            db.endTransaction();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        createAppApk(db);
        createInstalledApp(db);
        db.execSQL(CREATE_TABLE_REPO);

        insertRepo(
            db,
            context.getString(R.string.fdroid_repo_name),
            context.getString(R.string.fdroid_repo_address),
            context.getString(R.string.fdroid_repo_description),
            context.getString(R.string.fdroid_repo_pubkey),
            context.getResources().getInteger(R.integer.fdroid_repo_inuse),
            context.getResources().getInteger(R.integer.fdroid_repo_priority)
        );

        insertRepo(
            db,
            context.getString(R.string.fdroid_archive_name),
            context.getString(R.string.fdroid_archive_address),
            context.getString(R.string.fdroid_archive_description),
            context.getString(R.string.fdroid_archive_pubkey),
            context.getResources().getInteger(R.integer.fdroid_archive_inuse),
            context.getResources().getInteger(R.integer.fdroid_archive_priority)
        );

        insertRepo(
            db,
            context.getString(R.string.guardianproject_repo_name),
            context.getString(R.string.guardianproject_repo_address),
            context.getString(R.string.guardianproject_repo_description),
            context.getString(R.string.guardianproject_repo_pubkey),
            context.getResources().getInteger(R.integer.guardianproject_repo_inuse),
            context.getResources().getInteger(R.integer.guardianproject_repo_priority)
        );

        insertRepo(
            db,
            context.getString(R.string.guardianproject_archive_name),
            context.getString(R.string.guardianproject_archive_address),
            context.getString(R.string.guardianproject_archive_description),
            context.getString(R.string.guardianproject_archive_pubkey),
            context.getResources().getInteger(R.integer.guardianproject_archive_inuse),
            context.getResources().getInteger(R.integer.guardianproject_archive_priority)
        );
    }

    private void insertRepo(
        SQLiteDatabase db, String name, String address, String description,
        String pubKey, int inUse, int priority) {

        ContentValues values = new ContentValues();
        values.put(RepoProvider.DataColumns.ADDRESS, address);
        values.put(RepoProvider.DataColumns.NAME, name);
        values.put(RepoProvider.DataColumns.DESCRIPTION, description);
        values.put(RepoProvider.DataColumns.PUBLIC_KEY, pubKey);
        values.put(RepoProvider.DataColumns.FINGERPRINT, Utils.calcFingerprint(pubKey));
        values.put(RepoProvider.DataColumns.MAX_AGE, 0);
        values.put(RepoProvider.DataColumns.IN_USE, inUse);
        values.put(RepoProvider.DataColumns.PRIORITY, priority);
        values.put(RepoProvider.DataColumns.LAST_ETAG, (String)null);

        Log.i("FDroid", "Adding repository " + name);
        db.insert(TABLE_REPO, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.i("FDroid", "Upgrading database from v" + oldVersion + " v"
                + newVersion);

        migrateRepoTable(db, oldVersion);

        // The other tables are transient and can just be reset. Do this after
        // the repo table changes though, because it also clears the lastetag
        // fields which didn't always exist.
        resetTransient(db, oldVersion);

        addNameAndDescriptionToRepo(db, oldVersion);
        addFingerprintToRepo(db, oldVersion);
        addMaxAgeToRepo(db, oldVersion);
        addVersionToRepo(db, oldVersion);
        addLastUpdatedToRepo(db, oldVersion);
        renameRepoId(db, oldVersion);
        populateRepoNames(db, oldVersion);
        if (oldVersion < 43) createInstalledApp(db);
        addAppLabelToInstalledCache(db, oldVersion);
    }

    /**
     * Migrate repo list to new structure. (No way to change primary
     * key in sqlite - table must be recreated).
     */
    private void migrateRepoTable(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 20) {
            List<Repo> oldrepos = new ArrayList<Repo>();
            Cursor cursor = db.query(TABLE_REPO,
                    new String[] { "address", "inuse", "pubkey" },
                    null, null, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        Repo repo = new Repo();
                        repo.address = cursor.getString(0);
                        repo.inuse = (cursor.getInt(1) == 1);
                        repo.pubkey = cursor.getString(2);
                        oldrepos.add(repo);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            db.execSQL("drop table " + TABLE_REPO);
            db.execSQL(CREATE_TABLE_REPO);
            for (final Repo repo : oldrepos) {
                ContentValues values = new ContentValues();
                values.put("address", repo.address);
                values.put("inuse", repo.inuse);
                values.put("priority", 10);
                values.put("pubkey", repo.pubkey);
                values.put("lastetag", (String) null);
                db.insert(TABLE_REPO, null, values);
            }
        }
    }

    private void insertNameAndDescription(SQLiteDatabase db,
            int addressResId, int nameResId, int descriptionResId) {
        ContentValues values = new ContentValues();
        values.clear();
        values.put("name", context.getString(nameResId));
        values.put("description", context.getString(descriptionResId));
        db.update(TABLE_REPO, values, "address = ?", new String[] {
                context.getString(addressResId)
        });
    }

    /**
     * Add a name and description to the repo table, and updates the two
     * default repos with values from strings.xml.
     */
    private void addNameAndDescriptionToRepo(SQLiteDatabase db, int oldVersion) {
        boolean nameExists = columnExists(db, TABLE_REPO, "name");
        boolean descriptionExists = columnExists(db, TABLE_REPO, "description");
        if (oldVersion < 21 && !(nameExists && descriptionExists)) {
            if (!nameExists)
                db.execSQL("alter table " + TABLE_REPO + " add column name text");
            if (!descriptionExists)
                db.execSQL("alter table " + TABLE_REPO + " add column description text");
            insertNameAndDescription(db, R.string.fdroid_repo_address,
                    R.string.fdroid_repo_name, R.string.fdroid_repo_description);
            insertNameAndDescription(db, R.string.fdroid_archive_address,
                    R.string.fdroid_archive_name, R.string.fdroid_archive_description);
            insertNameAndDescription(db, R.string.guardianproject_repo_address,
                    R.string.guardianproject_repo_name, R.string.guardianproject_repo_description);
            insertNameAndDescription(db, R.string.guardianproject_archive_address,
                    R.string.guardianproject_archive_name, R.string.guardianproject_archive_description);
        }

    }

    /**
     * Add a fingerprint field to repos. For any field with a public key,
     * calculate its fingerprint and save it to the database.
     */
    private void addFingerprintToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 44) {
            if (!columnExists(db, TABLE_REPO, "fingerprint"))
                db.execSQL("alter table " + TABLE_REPO + " add column fingerprint text");
            List<Repo> oldrepos = new ArrayList<Repo>();
            Cursor cursor = db.query(TABLE_REPO,
                    new String[] { "address", "pubkey" },
                    null, null, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        Repo repo = new Repo();
                        repo.address = cursor.getString(0);
                        repo.pubkey = cursor.getString(1);
                        oldrepos.add(repo);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            for (final Repo repo : oldrepos) {
                ContentValues values = new ContentValues();
                values.put("fingerprint", Utils.calcFingerprint(repo.pubkey));
                db.update(TABLE_REPO, values, "address = ?", new String[] { repo.address });
            }
        }
    }

    private void addMaxAgeToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 30 && !columnExists(db, TABLE_REPO, "maxage")) {
            db.execSQL("alter table " + TABLE_REPO + " add column maxage integer not null default 0");
        }
    }

    private void addVersionToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 33 && !columnExists(db, TABLE_REPO, "version")) {
            db.execSQL("alter table " + TABLE_REPO + " add column version integer not null default 0");
        }
    }

    private void addLastUpdatedToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 35 && !columnExists(db, TABLE_REPO, "lastUpdated")) {
            Log.i("FDroid", "Adding lastUpdated column to " + TABLE_REPO);
            db.execSQL("Alter table " + TABLE_REPO + " add column lastUpdated string");
        }
    }

    private void resetTransient(SQLiteDatabase db, int oldVersion) {
        // Before version 42, only transient info was stored in here. As of some time
        // just before 42 (F-Droid 0.60ish) it now has "ignore this version" info which
        // was is specified by the user. We don't want to weely-neely nuke that data.
        // and the new way to deal with changes to the table structure is to add a
        // if (oldVersion < x && !columnExists(...) and then alter the table as required.
        if (oldVersion < 42) {
            context.getSharedPreferences("FDroid", Context.MODE_PRIVATE).edit()
                    .putBoolean("triedEmptyUpdate", false).commit();
            db.execSQL("drop table " + TABLE_APP);
            db.execSQL("drop table " + TABLE_APK);
            db.execSQL("update " + TABLE_REPO + " set lastetag = NULL");
            createAppApk(db);
        }
    }

    private static void createAppApk(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_APP);
        db.execSQL("create index app_id on " + TABLE_APP + " (id);");
        db.execSQL(CREATE_TABLE_APK);
        db.execSQL("create index apk_vercode on " + TABLE_APK + " (vercode);");
        db.execSQL("create index apk_id on " + TABLE_APK + " (id);");
    }

    private void createInstalledApp(SQLiteDatabase db) {
        Log.d(TAG, "Creating 'installed app' database table.");
        db.execSQL(CREATE_TABLE_INSTALLED_APP);
    }

    private void addAppLabelToInstalledCache(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 45) {
            Log.i(TAG, "Adding applicationLabel to installed app table. " +
                    "Turns out we will need to repopulate the cache after doing this, " +
                    "so just dropping and recreating the table (instead of altering and adding a column). " +
                    "This will force the entire cache to be rebuilt, including app names.");
            db.execSQL("DROP TABLE fdroid_installedApp;");
            createInstalledApp(db);
        }
    }

    private static boolean columnExists(SQLiteDatabase db,
            String table, String column) {
        return (db.rawQuery("select * from " + table + " limit 0,1", null)
                .getColumnIndex(column) != -1);
    }

}
