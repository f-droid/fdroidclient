/*
 * Copyright (C) 2010-13  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;

import org.fdroid.fdroid.compat.Compatibility;
import org.fdroid.fdroid.compat.ContextCompat;

public class DB {

    private static Semaphore dbSync = new Semaphore(1, true);
    static DB dbInstance = null;

    // Initialise the database. Called once when the application starts up.
    static void initDB(Context ctx) {
        dbInstance = new DB(ctx);
    }

    // Get access to the database. Must be called before any database activity,
    // and releaseDB must be called subsequently. Returns null in the event of
    // failure.
    static DB getDB() {
        try {
            dbSync.acquire();
            return dbInstance;
        } catch (InterruptedException e) {
            return null;
        }
    }

    // Release database access lock acquired via getDB().
    static void releaseDB() {
        dbSync.release();
    }

    private static final String DATABASE_NAME = "fdroid";

    // Possible values of the SQLite flag "synchronous"
    public static final int SYNC_OFF = 0;
    public static final int SYNC_NORMAL = 1;
    public static final int SYNC_FULL = 2;

    private SQLiteDatabase db;

    // The TABLE_APP table stores details of all the applications we know about.
    // This information is retrieved from the repositories.
    private static final String TABLE_APP = "fdroid_app";
    private static final String CREATE_TABLE_APP = "create table " + TABLE_APP
            + " ( " + "id text not null, " + "name text not null, "
            + "summary text not null, " + "icon text, "
            + "description text not null, " + "license text not null, "
            + "webURL text, " + "trackerURL text, " + "sourceURL text, "
            + "curVersion text," + "curVercode integer,"
            + "antiFeatures string," + "donateURL string,"
            + "bitcoinAddr string," + "litecoinAddr string,"
            + "flattrID string," + "requirements string,"
            + "categories string," + "added string,"
            + "lastUpdated string," + "compatible int not null,"
            + "ignoreAllUpdates int not null,"
            + "ignoreThisUpdate int not null,"
            + "primary key(id));";

    public static class App implements Comparable<App> {

        public App() {
            name = "Unknown";
            summary = "Unknown application";
            icon = null;
            id = "unknown";
            license = "Unknown";
            detail_trackerURL = null;
            detail_sourceURL = null;
            detail_donateURL = null;
            detail_bitcoinAddr = null;
            detail_litecoinAddr = null;
            detail_webURL = null;
            categories = null;
            antiFeatures = null;
            requirements = null;
            hasUpdates = false;
            toUpdate = false;
            updated = false;
            added = null;
            lastUpdated = null;
            apks = new ArrayList<Apk>();
            detail_Populated = false;
            compatible = false;
            ignoreAllUpdates = false;
            ignoreThisUpdate = 0;
            filtered = false;
            iconUrl = null;
        }

        // True when all the detail fields are populated, False otherwise.
        public boolean detail_Populated;

        // True if compatible with the device (i.e. if at least one apk is)
        public boolean compatible;

        public String id;
        public String name;
        public String summary;
        public String icon;

        // Null when !detail_Populated
        public String detail_description;

        public String license;

        // Null when !detail_Populated
        public String detail_webURL;

        // Null when !detail_Populated
        public String detail_trackerURL;

        // Null when !detail_Populated
        public String detail_sourceURL;

        // Donate link, or null
        // Null when !detail_Populated
        public String detail_donateURL;

        // Bitcoin donate address, or null
        // Null when !detail_Populated
        public String detail_bitcoinAddr;

        // Litecoin donate address, or null
        // Null when !detail_Populated
        public String detail_litecoinAddr;

        // Flattr donate ID, or null
        // Null when !detail_Populated
        public String detail_flattrID;

        public String curVersion;
        public int curVercode;
        public Apk curApk;
        public Date added;
        public Date lastUpdated;

        // Installed version (or null), version code and whether it was
        // installed by the user or bundled with the system. These are valid
        // only when getApps() has been called with getinstalledinfo=true.
        public String installedVersion;
        public int installedVerCode;
        public boolean userInstalled;

        // List of categories (as defined in the metadata
        // documentation) or null if there aren't any.
        public CommaSeparatedList categories;

        // List of anti-features (as defined in the metadata
        // documentation) or null if there aren't any.
        public CommaSeparatedList antiFeatures;

        // List of special requirements (such as root privileges) or
        // null if there aren't any.
        public CommaSeparatedList requirements;

        // Whether the app is filtered or not based on AntiFeatures and root
        // permission (set in the Settings page)
        public boolean filtered;

        // True if there are new versions (apks) available, regardless of
        // any filtering
        public boolean hasUpdates;

        // True if there are new versions (apks) available and the user wants
        // to be notified about them
        public boolean toUpdate;

        // True if all updates for this app are to be ignored
        public boolean ignoreAllUpdates;

        // True if the current update for this app is to be ignored
        public int ignoreThisUpdate;

        // Used internally for tracking during repo updates.
        public boolean updated;

        // List of apks.
        public List<Apk> apks;

        public String iconUrl;

        // Get the current version - this will be one of the Apks from 'apks'.
        // Can return null if there are no available versions.
        // This should be the 'current' version, as in the most recent stable
        // one, that most users would want by default. It might not be the
        // most recent, if for example there are betas etc.
        public Apk getCurrentVersion() {

            // Try and return the real current version first. It will find the
            // closest version smaller than the curVercode, being the same
            // vercode if it exists.
            if (curVercode > 0) {
                int latestcode = -1;
                Apk latestapk = null;
                for (Apk apk : apks) {
                    if (apk.compatible && apk.vercode <= curVercode
                            && apk.vercode > latestcode) {
                        latestapk = apk;
                        latestcode = apk.vercode;
                    }
                }
                return latestapk;
            }

            // If the current version was not set we return the most recent apk.
            if (curVercode == -1) {
                int latestcode = -1;
                Apk latestapk = null;
                for (Apk apk : apks) {
                    if (apk.compatible && apk.vercode > latestcode) {
                        latestapk = apk;
                        latestcode = apk.vercode;
                    }
                }
                return latestapk;
            }

            return null;
        }

        @Override
        public int compareTo(App arg0) {
            return name.compareToIgnoreCase(arg0.name);
        }

    }

    // The TABLE_APK table stores details of all the application versions we
    // know about. Each relates directly back to an entry in TABLE_APP.
    // This information is retrieved from the repositories.
    private static final String TABLE_APK = "fdroid_apk";
    private static final String CREATE_TABLE_APK = "create table " + TABLE_APK
            + " ( " + "id text not null, " + "version text not null, "
            + "repo integer not null, " + "hash text not null, "
            + "vercode int not null," + "apkName text not null, "
            + "size int not null," + "sig string," + "srcname string,"
            + "minSdkVersion integer," + "permissions string,"
            + "features string," + "nativecode string,"
            + "hashType string," + "added string,"
            + "compatible int not null," + "primary key(id,vercode));";

    public static class Apk {

        public Apk() {
            updated = false;
            detail_size = 0;
            added = null;
            repo = 0;
            detail_hash = null;
            detail_hashType = null;
            detail_permissions = null;
            compatible = false;
        }

        public String id;
        public String version;
        public int vercode;
        public int detail_size; // Size in bytes - 0 means we don't know!
        public int repo; // ID of the repo it comes from
        public String detail_hash;
        public String detail_hashType;
        public int minSdkVersion; // 0 if unknown
        public Date added;
        public CommaSeparatedList detail_permissions; // null if empty or
                                                      // unknown
        public CommaSeparatedList features; // null if empty or unknown

        public CommaSeparatedList nativecode; // null if empty or unknown

        // ID (md5 sum of public key) of signature. Might be null, in the
        // transition to this field existing.
        public String sig;

        // True if compatible with the device.
        public boolean compatible;

        public String apkName;

        // If not null, this is the name of the source tarball for the
        // application. Null indicates that it's a developer's binary
        // build - otherwise it's built from source.
        public String srcname;

        // Used internally for tracking during repo updates.
        public boolean updated;

        // Call isCompatible(apk) on an instance of this class to
        // check if an APK is compatible with the user's device.
        public static abstract class CompatibilityChecker extends Compatibility {

            public abstract boolean isCompatible(Apk apk);

            public static CompatibilityChecker getChecker(Context ctx) {
                CompatibilityChecker checker;
                if (hasApi(5))
                    checker = new EclairChecker(ctx);
                else
                    checker = new BasicChecker();
                Log.d("FDroid", "Compatibility checker for API level "
                        + getApi() + ": " + checker.getClass().getName());
                return checker;
            }
        }

        private static class BasicChecker extends CompatibilityChecker {
            public boolean isCompatible(Apk apk) {
                return hasApi(apk.minSdkVersion);
            }
        }

        @TargetApi(5)
        private static class EclairChecker extends CompatibilityChecker {

            private HashSet<String> features;
            private List<String> cpuAbis;
            private boolean ignoreTouchscreen;

            public EclairChecker(Context ctx) {

                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(ctx);
                ignoreTouchscreen = prefs
                        .getBoolean("ignoreTouchscreen", false);

                PackageManager pm = ctx.getPackageManager();
                StringBuilder logMsg = new StringBuilder();
                logMsg.append("Available device features:");
                features = new HashSet<String>();
                for (FeatureInfo fi : pm.getSystemAvailableFeatures()) {
                    features.add(fi.name);
                    logMsg.append('\n');
                    logMsg.append(fi.name);
                }

                cpuAbis = new ArrayList<String>();
                if (hasApi(8))
                    cpuAbis.add(android.os.Build.CPU_ABI2);
                cpuAbis.add(android.os.Build.CPU_ABI);

                Log.d("FDroid", logMsg.toString());
            }

            private boolean compatibleApi(CommaSeparatedList nativecode) {
                if (nativecode == null) return true;
                for (String abi : nativecode) {
                    if (cpuAbis.contains(abi)) {
                        return true;
                    }
                }
                return false;
            }

            public boolean isCompatible(Apk apk) {
                if (!hasApi(apk.minSdkVersion))
                    return false;
                if (apk.features != null) {
                    for (String feat : apk.features) {
                        if (ignoreTouchscreen
                                && feat.equals("android.hardware.touchscreen")) {
                            // Don't check it!
                        } else if (!features.contains(feat)) {
                            Log.d("FDroid", apk.id + " vercode " + apk.vercode
                                    + " is incompatible based on lack of "
                                    + feat);
                            return false;
                        }
                    }
                }
                if (!compatibleApi(apk.nativecode)) {
                    Log.d("FDroid", apk.id + " vercode " + apk.vercode
                            + " makes use of incompatible native code: "
                            + CommaSeparatedList.str(apk.nativecode)
                            + " while your architecture is " + cpuAbis.get(0));
                    return false;
                }
                return true;
            }
        }
    }

    // The TABLE_REPO table stores the details of the repositories in use.
    private static final String TABLE_REPO = "fdroid_repo";
    private static final String CREATE_TABLE_REPO = "create table "
            + TABLE_REPO + " (id integer primary key, address text not null, "
            + "name text, description text, inuse integer not null, "
            + "priority integer not null, pubkey text, lastetag text);";

    public static class Repo {
        public int id;
        public String address;
        public String name;
        public String description;
        public boolean inuse;
        public int priority;
        public String pubkey; // null for an unsigned repo
        public String lastetag; // last etag we updated from, null forces update
    }

    private final int DBVersion = 28;

    private static void createAppApk(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_APP);
        db.execSQL("create index app_id on " + TABLE_APP + " (id);");
        db.execSQL(CREATE_TABLE_APK);
        db.execSQL("create index apk_vercode on " + TABLE_APK + " (vercode);");
        db.execSQL("create index apk_id on " + TABLE_APK + " (id);");
    }

    public void resetTransient(SQLiteDatabase db) {
        mContext.getSharedPreferences("FDroid", Context.MODE_PRIVATE).edit()
                .putBoolean("triedEmptyUpdate", false).commit();
        db.execSQL("drop table " + TABLE_APP);
        db.execSQL("drop table " + TABLE_APK);
        db.execSQL("update " + TABLE_REPO + " set lastetag = NULL");
        createAppApk(db);
    }

    private static boolean columnExists(SQLiteDatabase db,
            String table, String column) {
        return (db.rawQuery( "select * from " + table + " limit 0,1", null )
                .getColumnIndex(column) != -1);
    }

    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DBVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            createAppApk(db);

            db.execSQL(CREATE_TABLE_REPO);
            ContentValues values = new ContentValues();
            values.put("address",
                    mContext.getString(R.string.default_repo_address));
            values.put("name",
                    mContext.getString(R.string.default_repo_name));
            values.put("description",
                    mContext.getString(R.string.default_repo_description));
            values.put("pubkey",
                    mContext.getString(R.string.default_repo_pubkey));
            values.put("inuse", 1);
            values.put("priority", 10);
            values.put("lastetag", (String) null);
            db.insert(TABLE_REPO, null, values);

            values = new ContentValues();
            values.put("address",
                    mContext.getString(R.string.default_repo_address2));
            values.put("name",
                    mContext.getString(R.string.default_repo_name2));
            values.put("description",
                    mContext.getString(R.string.default_repo_description2));
            values.put("pubkey",
                    mContext.getString(R.string.default_repo_pubkey));
            values.put("inuse", 0);
            values.put("priority", 20);
            values.put("lastetag", (String) null);
            db.insert(TABLE_REPO, null, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // Migrate repo list to new structure. (No way to change primary
            // key in sqlite - table must be recreated)
            if (oldVersion < 20) {
                List<Repo> oldrepos = new ArrayList<Repo>();
                Cursor c = db.query(TABLE_REPO,
                        new String[] { "address", "inuse", "pubkey" },
                        null, null, null, null, null);
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    Repo repo = new Repo();
                    repo.address = c.getString(0);
                    repo.inuse = (c.getInt(1) == 1);
                    repo.pubkey = c.getString(2);
                    oldrepos.add(repo);
                    c.moveToNext();
                }
                c.close();
                db.execSQL("drop table " + TABLE_REPO);
                db.execSQL(CREATE_TABLE_REPO);
                for (Repo repo : oldrepos) {
                    ContentValues values = new ContentValues();
                    values.put("address", repo.address);
                    values.put("inuse", repo.inuse);
                    values.put("priority", 10);
                    values.put("pubkey", repo.pubkey);
                    values.put("lastetag", (String) null);
                    db.insert(TABLE_REPO, null, values);
                }
            }

            // The other tables are transient and can just be reset. Do this after
            // the repo table changes though, because it also clears the lastetag
            // fields which didn't always exist.
            resetTransient(db);

            if (oldVersion < 21) {
                if (!columnExists(db, TABLE_REPO, "name"))
                    db.execSQL("alter table " + TABLE_REPO + " add column name text");
                if (!columnExists(db, TABLE_REPO, "description"))
                    db.execSQL("alter table " + TABLE_REPO + " add column description text");
                ContentValues values = new ContentValues();
                values.put("name", mContext.getString(R.string.default_repo_name));
                values.put("description", mContext.getString(R.string.default_repo_description));
                db.update(TABLE_REPO, values, "address = ?", new String[] {
                    mContext.getString(R.string.default_repo_address) });
                values.clear();
                values.put("name", mContext.getString(R.string.default_repo_name2));
                values.put("description", mContext.getString(R.string.default_repo_description2));
                db.update(TABLE_REPO, values, "address = ?", new String[] {
                    mContext.getString(R.string.default_repo_address2) });
            }

        }

    }

    /**
     * Get the local storage (cache) path. This will also create it if
     * it doesn't exist. It can return null if it's currently unavailable.
     */
    public static File getDataPath(Context ctx) {
        return ContextCompat.create(ctx).getExternalCacheDir();
    }

    private Context mContext;
    private Apk.CompatibilityChecker compatChecker = null;

    // The date format used for storing dates (e.g. lastupdated, added) in the
    // database.
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private DB(Context ctx) {

        mContext = ctx;
        DBHelper h = new DBHelper(ctx);
        db = h.getWritableDatabase();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        String sync_mode = prefs.getString("dbSyncMode", null);
        if ("off".equals(sync_mode))
            setSynchronizationMode(SYNC_OFF);
        else if ("normal".equals(sync_mode))
            setSynchronizationMode(SYNC_NORMAL);
        else if ("full".equals(sync_mode))
            setSynchronizationMode(SYNC_FULL);
        else
            sync_mode = null;
        if (sync_mode != null)
            Log.d("FDroid", "Database synchronization mode: " + sync_mode);
    }

    public void close() {
        db.close();
        db = null;
    }

    // Reset the transient data in the database.
    public void reset() {
        resetTransient(db);
    }

    // Delete the database, which should cause it to be re-created next time
    // it's used.
    public static void delete(Context ctx) {
        try {
            ctx.deleteDatabase(DATABASE_NAME);
            // Also try and delete the old one, from versions 0.13 and earlier.
            ctx.deleteDatabase("fdroid_db");
        } catch (Exception ex) {
            Log.e("FDroid",
                    "Exception in DB.delete:\n" + Log.getStackTraceString(ex));
        }
    }

    public List<String> getCategories() {
        List<String> result = new ArrayList<String>();
        Cursor c = null;
        try {
            c = db.query(true, TABLE_APP, new String[] { "categories" },
                    null, null, null, null, "categories", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Log.d("FDroid", "== CATEGS "+c.getString(0));
                CommaSeparatedList categories = CommaSeparatedList
                    .make(c.getString(0));
                for (String category : categories) {
                    Log.d("FDroid", "== CATEG "+category);
                    if (!result.contains(category)) {
                        Log.d("FDroid", "== CATEG ADDED "+category);
                        result.add(category);
                    }
                }
                c.moveToNext();
            }
        } catch (Exception e) {
            Log.e("FDroid",
                    "Exception during database reading:\n"
                            + Log.getStackTraceString(e));
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }

    private static final String[] POPULATE_APP_COLS = new String[] {
        "description", "webURL", "trackerURL", "sourceURL",
        "donateURL", "bitcoinAddr", "flattrID", "litecoinAddr" };

    private void populateAppDetails(App app) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_APP, POPULATE_APP_COLS, "id = ?",
                    new String[] { app.id }, null, null, null, null);
            cursor.moveToFirst();
            app.detail_description = cursor.getString(0);
            app.detail_webURL = cursor.getString(1);
            app.detail_trackerURL = cursor.getString(2);
            app.detail_sourceURL = cursor.getString(3);
            app.detail_donateURL = cursor.getString(4);
            app.detail_bitcoinAddr = cursor.getString(5);
            app.detail_flattrID = cursor.getString(6);
            app.detail_litecoinAddr = cursor.getString(7);
            app.detail_Populated = true;
        } catch (Exception e) {
            Log.d("FDroid", "Error populating app details " + app.id );
            Log.d("FDroid", e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static final String[] POPULATE_APK_COLS = new String[] { "hash", "hashType", "size", "permissions" };

    private void populateApkDetails(Apk apk, int repo) {
        if (repo == 0 || repo == apk.repo) {
            Cursor cursor = null;
            try {
                cursor = db.query(
                        TABLE_APK,
                        POPULATE_APK_COLS,
                        "id = ? and vercode = ?",
                        new String[] { apk.id,
                                Integer.toString(apk.vercode) }, null,
                        null, null, null);
                cursor.moveToFirst();
                apk.detail_hash = cursor.getString(0);
                apk.detail_hashType = cursor.getString(1);
                apk.detail_size = cursor.getInt(2);
                apk.detail_permissions = CommaSeparatedList.make(cursor
                        .getString(3));
            } catch (Exception e) {
                Log.d("FDroid", "Error populating apk details for " + apk.id + " (version " + apk.version + ")");
                Log.d("FDroid", e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            Log.d("FDroid", "Not setting details for apk '" + apk.id + "' (version " + apk.version +") because it belongs to a different repo.");
        }
    }

    // Populate the details for the given app, if necessary.
    // If 'apkrepo' is non-zero, only apks from that repo are
    // populated (this is used during the update process)
    public void populateDetails(App app, int apkRepo) {
        if (!app.detail_Populated) {
            populateAppDetails(app);
        }

        for (Apk apk : app.apks) {
            if (apk.detail_hash == null) {
                populateApkDetails(apk, apkRepo);
            }
        }
    }

    // Repopulate the details for the given app.
    // If 'apkrepo' is non-zero, only apks from that repo are
    // populated.
    public void repopulateDetails(App app, int apkRepo) {
        populateAppDetails(app);

        for (Apk apk : app.apks) {
            populateApkDetails(apk, apkRepo);
        }
    }

    // Return a list of apps matching the given criteria. Filtering is
    // also done based on compatibility and anti-features according to
    // the user's current preferences.
    public List<App> getApps(boolean getinstalledinfo) {

        // If we're going to need it, get info in what's currently installed
        Map<String, PackageInfo> systemApks = null;
        if (getinstalledinfo) {
            Log.d("FDroid", "Reading installed packages");
            systemApks = new HashMap<String, PackageInfo>();
            List<PackageInfo> installedPackages = mContext.getPackageManager()
                    .getInstalledPackages(0);
            for (PackageInfo appInfo : installedPackages) {
                systemApks.put(appInfo.packageName, appInfo);
            }
        }

        Map<String, App> apps = new HashMap<String, App>();
        Cursor c = null;
        long startTime = System.currentTimeMillis();
        try {

            String cols[] = new String[] { "antiFeatures", "requirements",
                    "categories", "id", "name", "summary", "icon", "license",
                    "curVersion", "curVercode", "added", "lastUpdated",
                    "compatible", "ignoreAllUpdates", "ignoreThisUpdate" };
            c = db.query(TABLE_APP, cols, null, null, null, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {

                App app = new App();
                app.antiFeatures = DB.CommaSeparatedList.make(c.getString(0));
                app.requirements = DB.CommaSeparatedList.make(c.getString(1));
                app.categories = DB.CommaSeparatedList.make(c.getString(2));
                app.id = c.getString(3);
                app.name = c.getString(4);
                app.summary = c.getString(5);
                app.icon = c.getString(6);
                app.license = c.getString(7);
                app.curVersion = c.getString(8);
                app.curVercode = c.getInt(9);
                String sAdded = c.getString(10);
                app.added = (sAdded == null || sAdded.length() == 0) ? null
                        : mDateFormat.parse(sAdded);
                String sLastUpdated = c.getString(11);
                app.lastUpdated = (sLastUpdated == null || sLastUpdated
                        .length() == 0) ? null : mDateFormat
                        .parse(sLastUpdated);
                app.compatible = c.getInt(12) == 1;
                app.ignoreAllUpdates = c.getInt(13) == 1;
                app.ignoreThisUpdate = c.getInt(14);
                app.hasUpdates = false;

                if (getinstalledinfo && systemApks.containsKey(app.id)) {
                    PackageInfo sysapk = systemApks.get(app.id);
                    app.installedVersion = sysapk.versionName;
                    if (app.installedVersion == null)
                        app.installedVersion = "null";
                    app.installedVerCode = sysapk.versionCode;
                    app.userInstalled = ((sysapk.applicationInfo.flags
                            & ApplicationInfo.FLAG_SYSTEM) != 1);
                } else {
                    app.installedVersion = null;
                    app.installedVerCode = 0;
                    app.userInstalled = false;
                }

                apps.put(app.id, app);

                c.moveToNext();
            }
            c.close();
            c = null;

            Log.d("FDroid", "Read app data from database " + " (took "
                    + (System.currentTimeMillis() - startTime) + " ms)");

            cols = new String[] { "id", "version", "vercode", "sig", "srcname",
                    "apkName", "minSdkVersion", "added", "features", "nativecode",
                    "compatible", "repo" };
            c = db.query(TABLE_APK, cols, null, null, null, null,
                    "vercode desc");
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Apk apk = new Apk();
                apk.id = c.getString(0);
                apk.version = c.getString(1);
                apk.vercode = c.getInt(2);
                apk.sig = c.getString(3);
                apk.srcname = c.getString(4);
                apk.apkName = c.getString(5);
                apk.minSdkVersion = c.getInt(6);
                String sApkAdded = c.getString(7);
                apk.added = (sApkAdded == null || sApkAdded.length() == 0) ? null
                        : mDateFormat.parse(sApkAdded);
                apk.features = CommaSeparatedList.make(c.getString(8));
                apk.nativecode = CommaSeparatedList.make(c.getString(9));
                apk.compatible = c.getInt(10) == 1;
                apk.repo = c.getInt(11);
                apps.get(apk.id).apks.add(apk);
                c.moveToNext();
            }
            c.close();

        } catch (Exception e) {
            Log.e("FDroid",
                    "Exception during database reading:\n"
                            + Log.getStackTraceString(e));
        } finally {
            if (c != null) {
                c.close();
            }

            Log.d("FDroid", "Read app and apk data from database " + " (took "
                    + (System.currentTimeMillis() - startTime) + " ms)");
        }

        List<App> result = new ArrayList<App>(apps.values());
        Collections.sort(result);

        // Fill in the hasUpdates fields if we have the necessary information...
        if (getinstalledinfo) {

            // We'll say an application has updates if it's installed AND the
            // version is older than the current one
            for (App app : result) {
                app.curApk = app.getCurrentVersion();
                if (app.curApk != null
                        && app.installedVerCode > 0
                        && app.installedVerCode < app.curApk.vercode) {
                    app.hasUpdates = true;
                }
            }
        }

        return result;
    }


    // Alternative to getApps() that only refreshes the installation details
    // of those apps in invalidApps. Much faster when returning from
    // installs/uninstalls, where getApps() was already called before.
    public List<App> refreshApps(List<App> apps, List<String> invalidApps) {

        List<PackageInfo> installedPackages = mContext.getPackageManager()
                .getInstalledPackages(0);
        long startTime = System.currentTimeMillis();
        List<String> refreshedApps = new ArrayList<String>();
        for (String appid : invalidApps) {
            if (refreshedApps.contains(appid)) continue;
            App app = null;
            int index = -1;
            for (App oldapp : apps) {
                index++;
                if (oldapp.id.equals(appid)) {
                    app = oldapp;
                    break;
                }
            }

            if (app == null) continue;

            PackageInfo installed = null;

            for (PackageInfo appInfo : installedPackages) {
                if (appInfo.packageName.equals(appid)) {
                    installed = appInfo;
                    break;
                }
            }

            if (installed != null) {
                app.installedVersion = installed.versionName;
                if (app.installedVersion == null)
                    app.installedVersion = "null";
                app.installedVerCode = installed.versionCode;
            } else {
                app.installedVersion = null;
                app.installedVerCode = 0;
            }

            app.hasUpdates = false;
            app.curApk = app.getCurrentVersion();
            if (app.curApk != null
                    && app.installedVersion != null
                    && app.installedVerCode < app.curApk.vercode) {
                app.hasUpdates = true;
            }

            apps.set(index, app);
            refreshedApps.add(appid);
        }
        Log.d("FDroid", "Refreshing " + refreshedApps.size() + " apps took "
                + (System.currentTimeMillis() - startTime) + " ms");

        return apps;
    }

    public List<String> doSearch(String query) {

        List<String> ids = new ArrayList<String>();
        Cursor c = null;
        try {
            String filter = "%" + query + "%";
            c = db.query(TABLE_APP, new String[] { "id" },
                    "id like ? or name like ? or summary like ? or description like ?",
                    new String[] { filter, filter, filter, filter }, null, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                ids.add(c.getString(0));
                c.moveToNext();
            }
        } finally {
            if (c != null)
                c.close();
        }
        return ids;
    }

    public static class CommaSeparatedList implements Iterable<String> {
        private String value;

        private CommaSeparatedList(String list) {
            value = list;
        }

        public static CommaSeparatedList make(String list) {
            if (list == null || list.length() == 0)
                return null;
            else
                return new CommaSeparatedList(list);
        }

        public static String str(CommaSeparatedList instance) {
            return (instance == null ? null : instance.toString());
        }

        public String toString() {
            return value;
        }

        public Iterator<String> iterator() {
            SimpleStringSplitter splitter = new SimpleStringSplitter(',');
            splitter.setString(value);
            return splitter.iterator();
        }

        public boolean contains(String v) {
            Iterator<String> it = iterator();
            while (it.hasNext()) {
                if (it.next().equals(v))
                    return true;
            }
            return false;
        }
    }

    private List<App> updateApps = null;

    // Called before a repo update starts.
    public void beginUpdate(List<DB.App> apps) {
        // Get a list of all apps. All the apps and apks in this list will
        // have 'updated' set to false at this point, and we will only set
        // it to true when we see the app/apk in a repository. Thus, at the
        // end, any that are still false can be removed.
        updateApps = apps;
        Log.d("FDroid", "AppUpdate: " + updateApps.size() + " apps before starting.");
        // Wrap the whole update in a transaction. Make sure to call
        // either endUpdate or cancelUpdate to commit or discard it,
        // respectively.
        db.beginTransaction();
    }

    // Called when a repo update ends. Any applications that have not been
    // updated (by a call to updateApplication) are assumed to be no longer
    // in the repos.
    public void endUpdate() {
        if (updateApps == null)
            return;
        Log.d("FDroid", "Processing endUpdate - " + updateApps.size()
                + " apps before");
        for (App app : updateApps) {
            if (!app.updated) {
                // The application hasn't been updated, so it's no longer
                // in the repos.
                Log.d("FDroid", "AppUpdate: " + app.name
                        + " is no longer in any repository - removing");
                db.delete(TABLE_APP, "id = ?", new String[] { app.id });
                db.delete(TABLE_APK, "id = ?", new String[] { app.id });
            } else {
                for (Apk apk : app.apks) {
                    if (!apk.updated) {
                        // The package hasn't been updated, so this is a
                        // version that's no longer available.
                        Log.d("FDroid", "AppUpdate: Package " + apk.id + "/"
                                + apk.version
                                + " is no longer in any repository - removing");
                        db.delete(TABLE_APK, "id = ? and version = ?",
                                new String[] { app.id, apk.version });
                    }
                }
            }
        }
        // Commit updates to the database.
        db.setTransactionSuccessful();
        db.endTransaction();
        Log.d("FDroid", "AppUpdate: " + updateApps.size()
                + " apps on completion.");
        updateApps = null;
        return;
    }

    // Called instead of endUpdate if the update failed.
    public void cancelUpdate() {
        if (updateApps != null) {
            db.endTransaction();
            updateApps = null;
        }
    }

    // Called during update to supply new details for an application (or
    // details of a completely new one). Calls to this must be wrapped by
    // a call to beginUpdate and a call to endUpdate.
    // Returns true if the app was accepted. If it wasn't, it's probably
    // because it's not compatible with the device.
    public boolean updateApplication(App upapp) {

        if (updateApps == null) {
            return false;
        }

        // Lazy initialise this...
        if (compatChecker == null)
            compatChecker = Apk.CompatibilityChecker.getChecker(mContext);

        // See if it's compatible (by which we mean if it has at least one
        // compatible apk)
        upapp.compatible = false;
        for (Apk apk : upapp.apks) {
            if (compatChecker.isCompatible(apk)) {
                apk.compatible = true;
                upapp.compatible = true;
            }
        }

        boolean found = false;
        for (App app : updateApps) {
            if (app.id.equals(upapp.id)) {
                updateApp(app, upapp);
                app.updated = true;
                found = true;
                for (Apk upapk : upapp.apks) {
                    boolean afound = false;
                    for (Apk apk : app.apks) {
                        if (apk.vercode == upapk.vercode) {
                            // Log.d("FDroid", "AppUpdate: " + apk.version
                            // + " is a known version.");
                            updateApkIfDifferent(apk, upapk);
                            apk.updated = true;
                            afound = true;
                            break;
                        }
                    }
                    if (!afound) {
                        // A new version of this application.
                        updateApkIfDifferent(null, upapk);
                        upapk.updated = true;
                        app.apks.add(upapk);
                    }
                }
                break;
            }
        }
        if (!found) {
            // It's a brand new application...
            updateApp(null, upapp);
            for (Apk upapk : upapp.apks) {
                updateApkIfDifferent(null, upapk);
                upapk.updated = true;
            }
            upapp.updated = true;
            updateApps.add(upapp);
        }
        return true;

    }

    // Update application details in the database.
    // 'oldapp' - previous details - i.e. what's in the database.
    // If null, this app is not in the database at all and
    // should be added.
    // 'upapp' - updated details
    private void updateApp(App oldapp, App upapp) {
        ContentValues values = new ContentValues();
        values.put("id", upapp.id);
        values.put("name", upapp.name);
        values.put("summary", upapp.summary);
        values.put("icon", upapp.icon);
        values.put("description", upapp.detail_description);
        values.put("license", upapp.license);
        values.put("webURL", upapp.detail_webURL);
        values.put("trackerURL", upapp.detail_trackerURL);
        values.put("sourceURL", upapp.detail_sourceURL);
        values.put("donateURL", upapp.detail_donateURL);
        values.put("bitcoinAddr", upapp.detail_bitcoinAddr);
        values.put("litecoinAddr", upapp.detail_litecoinAddr);
        values.put("flattrID", upapp.detail_flattrID);
        values.put("added",
                upapp.added == null ? "" : mDateFormat.format(upapp.added));
        values.put(
                "lastUpdated",
                upapp.added == null ? "" : mDateFormat
                        .format(upapp.lastUpdated));
        values.put("curVersion", upapp.curVersion);
        values.put("curVercode", upapp.curVercode);
        values.put("categories", CommaSeparatedList.str(upapp.categories));
        values.put("antiFeatures", CommaSeparatedList.str(upapp.antiFeatures));
        values.put("requirements", CommaSeparatedList.str(upapp.requirements));
        values.put("compatible", upapp.compatible ? 1 : 0);

        // Values to keep if already present
        if (oldapp == null) {
            values.put("ignoreAllUpdates", upapp.ignoreAllUpdates ? 1 : 0);
            values.put("ignoreThisUpdate", upapp.ignoreThisUpdate);
        } else {
            values.put("ignoreAllUpdates", oldapp.ignoreAllUpdates ? 1 : 0);
            values.put("ignoreThisUpdate", oldapp.ignoreThisUpdate);
        }

        if (oldapp != null) {
            db.update(TABLE_APP, values, "id = ?", new String[] { oldapp.id });
        } else {
            db.insert(TABLE_APP, null, values);
        }
    }

    // Update apk details in the database, if different to the
    // previous ones.
    // 'oldapk' - previous details - i.e. what's in the database.
    // If null, this apk is not in the database at all and
    // should be added.
    // 'upapk' - updated details
    private void updateApkIfDifferent(Apk oldapk, Apk upapk) {
        ContentValues values = new ContentValues();
        values.put("id", upapk.id);
        values.put("version", upapk.version);
        values.put("vercode", upapk.vercode);
        values.put("repo", upapk.repo);
        values.put("hash", upapk.detail_hash);
        values.put("hashType", upapk.detail_hashType);
        values.put("sig", upapk.sig);
        values.put("srcname", upapk.srcname);
        values.put("size", upapk.detail_size);
        values.put("apkName", upapk.apkName);
        values.put("minSdkVersion", upapk.minSdkVersion);
        values.put("added",
                upapk.added == null ? "" : mDateFormat.format(upapk.added));
        values.put("permissions",
                CommaSeparatedList.str(upapk.detail_permissions));
        values.put("features", CommaSeparatedList.str(upapk.features));
        values.put("nativecode", CommaSeparatedList.str(upapk.nativecode));
        values.put("compatible", upapk.compatible ? 1 : 0);
        if (oldapk != null) {
            db.update(TABLE_APK, values,
                    "id = ? and vercode = ?",
                    new String[] { oldapk.id, Integer.toString(oldapk.vercode) });
        } else {
            db.insert(TABLE_APK, null, values);
        }
    }

    // Get details of a repo, given the ID. Returns null if the repo
    // doesn't exist.
    public Repo getRepo(int id) {
        Cursor c = null;
        try {
            c = db.query(TABLE_REPO, new String[] { "address", "name",
                "description", "inuse", "priority", "pubkey", "lastetag" },
                    "id = ?", new String[] { Integer.toString(id) }, null, null, null);
            if (!c.moveToFirst())
                return null;
            Repo repo = new Repo();
            repo.id = id;
            repo.address = c.getString(0);
            repo.name = c.getString(1);
            repo.description = c.getString(2);
            repo.inuse = (c.getInt(3) == 1);
            repo.priority = c.getInt(4);
            repo.pubkey = c.getString(5);
            repo.lastetag = c.getString(6);
            return repo;
        } finally {
            if (c != null)
                c.close();
        }
    }

    // Get a list of the configured repositories.
    public List<Repo> getRepos() {
        List<Repo> repos = new ArrayList<Repo>();
        Cursor c = null;
        try {
            c = db.query(TABLE_REPO, new String[] { "id", "address", "name",
                    "description", "inuse", "priority", "pubkey", "lastetag" },
                    null, null, null, null, "priority");
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Repo repo = new Repo();
                repo.id = c.getInt(0);
                repo.address = c.getString(1);
                repo.name = c.getString(2);
                repo.description = c.getString(3);
                repo.inuse = (c.getInt(4) == 1);
                repo.priority = c.getInt(5);
                repo.pubkey = c.getString(6);
                repo.lastetag = c.getString(7);
                repos.add(repo);
                c.moveToNext();
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return repos;
    }

    public void changeServerStatus(String address) {
        db.execSQL("update " + TABLE_REPO
                + " set inuse=1-inuse, lastetag=null where address = ?",
                new String[] { address });
    }

    public void setIgnoreUpdates(String appid, boolean All, int This) {
        db.execSQL("update " + TABLE_APP + " set"
                + " ignoreAllUpdates=" + (All ? '1' : '0')
                + ", ignoreThisUpdate="+This
                + " where id = ?", new String[] { appid });
    }

    public void updateRepoByAddress(Repo repo) {
        ContentValues values = new ContentValues();
        values.put("name", repo.name);
        values.put("description", repo.description);
        values.put("inuse", repo.inuse);
        values.put("priority", repo.priority);
        values.put("pubkey", repo.pubkey);
        values.put("lastetag", (String) null);
        db.update(TABLE_REPO, values, "address = ?",
                new String[] { repo.address });
    }

    public void writeLastEtag(Repo repo) {
        ContentValues values = new ContentValues();
        values.put("lastetag", repo.lastetag);
        db.update(TABLE_REPO, values, "address = ?",
                new String[] { repo.address });
    }

    public void addRepo(String address, String name, String description,
            int priority, String pubkey, boolean inuse) {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("name", name);
        values.put("description", description);
        values.put("inuse", inuse ? 1 : 0);
        values.put("priority", priority);
        values.put("pubkey", pubkey);
        values.put("lastetag", (String) null);
        db.insert(TABLE_REPO, null, values);
    }

    public void doDisableRepos(List<String> addresses, boolean remove) {
        if (addresses.isEmpty()) return;
        db.beginTransaction();
        try {
            for (String address : addresses) {

                // Before removing the repo, remove any apks that are
                // connected to it...
                Cursor c = null;
                try {
                    c = db.query(TABLE_REPO, new String[] { "id" },
                            "address = ?", new String[] { address },
                            null, null, null, null);
                    c.moveToFirst();
                    if (!c.isAfterLast()) {
                        db.delete(TABLE_APK, "repo = ?",
                                new String[] { Integer.toString(c.getInt(0)) });
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                if (remove)
                    db.delete(TABLE_REPO, "address = ?",
                            new String[] { address });
            }
            List<App> apps = getApps(false);
            for (App app : apps) {
                if (app.apks.isEmpty()) {
                    db.delete(TABLE_APP, "id = ?", new String[] { app.id });
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int getSynchronizationMode() {
        Cursor cursor = db.rawQuery("PRAGMA synchronous", null);
        cursor.moveToFirst();
        int mode = cursor.getInt(0);
        cursor.close();
        return mode;
    }

    public void setSynchronizationMode(int mode) {
        db.execSQL("PRAGMA synchronous = " + mode);
    }
}
