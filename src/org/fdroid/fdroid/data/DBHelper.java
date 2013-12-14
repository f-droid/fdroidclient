package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.R;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "fdroid";

    private static final String CREATE_TABLE_REPO = "create table "
            + DB.TABLE_REPO + " (id integer primary key, address text not null, "
            + "name text, description text, inuse integer not null, "
            + "priority integer not null, pubkey text, fingerprint text, "
            + "maxage integer not null default 0, "
            + "lastetag text, lastUpdated string);";

    private static final String CREATE_TABLE_APK = "create table " + DB.TABLE_APK
            + " ( " + "id text not null, " + "version text not null, "
            + "repo integer not null, " + "hash text not null, "
            + "vercode int not null," + "apkName text not null, "
            + "size int not null," + "sig string," + "srcname string,"
            + "minSdkVersion integer," + "permissions string,"
            + "features string," + "nativecode string,"
            + "hashType string," + "added string,"
            + "compatible int not null," + "primary key(id,vercode));";

    private static final String CREATE_TABLE_APP = "create table " + DB.TABLE_APP
            + " ( " + "id text not null, " + "name text not null, "
            + "summary text not null, " + "icon text, "
            + "description text not null, " + "license text not null, "
            + "webURL text, " + "trackerURL text, " + "sourceURL text, "
            + "curVersion text," + "curVercode integer,"
            + "antiFeatures string," + "donateURL string,"
            + "bitcoinAddr string," + "litecoinAddr string,"
            + "dogecoinAddr string,"
            + "flattrID string," + "requirements string,"
            + "categories string," + "added string,"
            + "lastUpdated string," + "compatible int not null,"
            + "ignoreAllUpdates int not null,"
            + "ignoreThisUpdate int not null,"
            + "primary key(id));";

    private static final int DB_VERSION = 35;

    private Context context;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        createAppApk(db);

        db.execSQL(CREATE_TABLE_REPO);
        ContentValues values = new ContentValues();
        values.put("address",
                context.getString(R.string.default_repo_address));
        values.put("name",
                context.getString(R.string.default_repo_name));
        values.put("description",
                context.getString(R.string.default_repo_description));
        values.put("version", 0);
        String pubkey = context.getString(R.string.default_repo_pubkey);
        String fingerprint = DB.calcFingerprint(pubkey);
        values.put("pubkey", pubkey);
        values.put("fingerprint", fingerprint);
        values.put("maxage", 0);
        values.put("inuse", 1);
        values.put("priority", 10);
        values.put("lastetag", (String) null);
        db.insert(DB.TABLE_REPO, null, values);

        values = new ContentValues();
        values.put("address",
                context.getString(R.string.default_repo_address2));
        values.put("name",
                context.getString(R.string.default_repo_name2));
        values.put("description",
                context.getString(R.string.default_repo_description2));
        values.put("version", 0);
        // default #2 is /archive which has the same key as /repo
        values.put("pubkey", pubkey);
        values.put("fingerprint", fingerprint);
        values.put("maxage", 0);
        values.put("inuse", 0);
        values.put("priority", 20);
        values.put("lastetag", (String) null);
        db.insert(DB.TABLE_REPO, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.i("FDroid", "Upgrading database from v" + oldVersion + " v"
                + newVersion);

        migradeRepoTable(db, oldVersion);

        // The other tables are transient and can just be reset. Do this after
        // the repo table changes though, because it also clears the lastetag
        // fields which didn't always exist.
        resetTransient(db);

        addNameAndDescriptionToRepo(db, oldVersion);
        addFingerprintToRepo(db, oldVersion);
        addMaxAgeToRepo(db, oldVersion);
        addVersionToRepo(db, oldVersion);
        addLastUpdatedToRepo(db, oldVersion);
    }

    /**
     * Migrate repo list to new structure. (No way to change primary
     * key in sqlite - table must be recreated).
     */
    private void migradeRepoTable(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 20) {
            List<DB.Repo> oldrepos = new ArrayList<DB.Repo>();
            Cursor c = db.query(DB.TABLE_REPO,
                    new String[] { "address", "inuse", "pubkey" },
                    null, null, null, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                DB.Repo repo = new DB.Repo();
                repo.address = c.getString(0);
                repo.inuse = (c.getInt(1) == 1);
                repo.pubkey = c.getString(2);
                oldrepos.add(repo);
                c.moveToNext();
            }
            c.close();
            db.execSQL("drop table " + DB.TABLE_REPO);
            db.execSQL(CREATE_TABLE_REPO);
            for (DB.Repo repo : oldrepos) {
                ContentValues values = new ContentValues();
                values.put("address", repo.address);
                values.put("inuse", repo.inuse);
                values.put("priority", 10);
                values.put("pubkey", repo.pubkey);
                values.put("lastetag", (String) null);
                db.insert(DB.TABLE_REPO, null, values);
            }
        }
    }

    /**
     * Add a name and description to the repo table, and updates the two
     * default repos with values from strings.xml.
     */
    private void addNameAndDescriptionToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 21) {
            if (!columnExists(db, DB.TABLE_REPO, "name"))
                db.execSQL("alter table " + DB.TABLE_REPO + " add column name text");
            if (!columnExists(db, DB.TABLE_REPO, "description"))
                db.execSQL("alter table " + DB.TABLE_REPO + " add column description text");
            ContentValues values = new ContentValues();
            values.put("name", context.getString(R.string.default_repo_name));
            values.put("description", context.getString(R.string.default_repo_description));
            db.update(DB.TABLE_REPO, values, "address = ?", new String[]{
                    context.getString(R.string.default_repo_address)});
            values.clear();
            values.put("name", context.getString(R.string.default_repo_name2));
            values.put("description", context.getString(R.string.default_repo_description2));
            db.update(DB.TABLE_REPO, values, "address = ?", new String[] {
                context.getString(R.string.default_repo_address2) });
        }

    }

    /**
     * Add a fingerprint field to repos. For any field with a public key,
     * calculate its fingerprint and save it to the database.
     */
    private void addFingerprintToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 29) {
            if (!columnExists(db, DB.TABLE_REPO, "fingerprint"))
                db.execSQL("alter table " + DB.TABLE_REPO + " add column fingerprint text");
            List<DB.Repo> oldrepos = new ArrayList<DB.Repo>();
            Cursor c = db.query(DB.TABLE_REPO,
                    new String[] { "address", "pubkey" },
                    null, null, null, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                DB.Repo repo = new DB.Repo();
                repo.address = c.getString(0);
                repo.pubkey = c.getString(1);
                oldrepos.add(repo);
                c.moveToNext();
            }
            c.close();
            for (DB.Repo repo : oldrepos) {
                ContentValues values = new ContentValues();
                values.put("fingerprint", DB.calcFingerprint(repo.pubkey));
                db.update(DB.TABLE_REPO, values, "address = ?", new String[] { repo.address });
            }
        }
    }

    private void addMaxAgeToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 30) {
            db.execSQL("alter table " + DB.TABLE_REPO + " add column maxage integer not null default 0");
        }
    }

    private void addVersionToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 33 && !columnExists(db, DB.TABLE_REPO, "version")) {
            db.execSQL("alter table " + DB.TABLE_REPO + " add column version integer not null default 0");
        }
    }

    private void addLastUpdatedToRepo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 35 && !columnExists(db, DB.TABLE_REPO, "lastUpdated")) {
            db.execSQL("Alter table " + DB.TABLE_REPO + " add column lastUpdated string");
        }
    }

    private void resetTransient(SQLiteDatabase db) {
        context.getSharedPreferences("FDroid", Context.MODE_PRIVATE).edit()
                .putBoolean("triedEmptyUpdate", false).commit();
        db.execSQL("drop table " + DB.TABLE_APP);
        db.execSQL("drop table " + DB.TABLE_APK);
        db.execSQL("update " + DB.TABLE_REPO + " set lastetag = NULL");
        createAppApk(db);
    }

    private static void createAppApk(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_APP);
        db.execSQL("create index app_id on " + DB.TABLE_APP + " (id);");
        db.execSQL(CREATE_TABLE_APK);
        db.execSQL("create index apk_vercode on " + DB.TABLE_APK + " (vercode);");
        db.execSQL("create index apk_id on " + DB.TABLE_APK + " (id);");
    }

    private static boolean columnExists(SQLiteDatabase db,
            String table, String column) {
        return (db.rawQuery( "select * from " + table + " limit 0,1", null )
                .getColumnIndex(column) != -1);
    }

}
