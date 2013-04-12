/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;

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
            + "requirements string," + "category string," + "added string,"
            + "lastUpdated string," + "compatible int not null,"
            + "primary key(id));";

    public static class App implements Comparable<App> {

        public App() {
            name = "Unknown";
            summary = "Unknown application";
            icon = "noicon.png";
            id = "unknown";
            license = "Unknown";
            category = "Uncategorized";
            detail_trackerURL = null;
            detail_sourceURL = null;
            detail_donateURL = null;
            detail_webURL = null;
            antiFeatures = null;
            requirements = null;
            hasUpdates = false;
            updated = false;
            added = null;
            lastUpdated = null;
            apks = new Vector<Apk>();
            detail_Populated = false;
            compatible = false;
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
        public String category;

        // Null when !detail_Populated
        public String detail_webURL;

        // Null when !detail_Populated
        public String detail_trackerURL;

        // Null when !detail_Populated
        public String detail_sourceURL;

        // Donate link, or null
        // Null when !detail_Populated
        public String detail_donateURL;

        public String curVersion;
        public int curVercode;
        public Date added;
        public Date lastUpdated;

        // Installed version (or null) and version code. These are valid only
        // when getApps() has been called with getinstalledinfo=true.
        public String installedVersion;
        public int installedVerCode;

        // List of anti-features (as defined in the metadata
        // documentation) or null if there aren't any.
        public CommaSeparatedList antiFeatures;

        // List of special requirements (such as root privileges) or
        // null if there aren't any.
        public CommaSeparatedList requirements;

        // True if there are new versions (apks) that the user hasn't
        // explicitly ignored. (We're currently not using the database
        // field for this - we make the decision on the fly in getApps().
        public boolean hasUpdates;

        // The name of the version that would be updated to.
        public String updateVersion;

        // Used internally for tracking during repo updates.
        public boolean updated;

        // List of apks.
        public Vector<Apk> apks;

        // Get the current version - this will be one of the Apks from 'apks'.
        // Can return null if there are no available versions.
        // This should be the 'current' version, as in the most recent stable
        // one, that most users would want by default. It might not be the
        // most recent, if for example there are betas etc.
        public Apk getCurrentVersion() {

            // Try and return the real current version first...
            if (curVersion != null && curVercode > 0) {
                for (Apk apk : apks) {
                    if (apk.vercode == curVercode)
                        return apk;
                }
            }

            // If we don't know the current version, or we don't have it, we
            // return the most recent version we have...
            int latestcode = -1;
            Apk latestapk = null;
            for (Apk apk : apks) {
                if (apk.vercode > latestcode) {
                    latestapk = apk;
                    latestcode = apk.vercode;
                }
            }
            return latestapk;
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
            + "features string," + "hashType string," + "added string,"
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
        public static abstract class CompatibilityChecker {

            // Because Build.VERSION.SDK_INT requires API level 5
            @SuppressWarnings("deprecation")
            protected final static int SDK_INT = Integer
                    .parseInt(Build.VERSION.SDK);

            public abstract boolean isCompatible(Apk apk);

            public static CompatibilityChecker getChecker(Context ctx) {
                CompatibilityChecker checker;
                if (SDK_INT >= 5)
                    checker = new EclairChecker(ctx);
                else
                    checker = new BasicChecker();
                Log.d("FDroid", "Compatibility checker for API level "
                        + SDK_INT + ": " + checker.getClass().getName());
                return checker;
            }
        }

        private static class BasicChecker extends CompatibilityChecker {
            public boolean isCompatible(Apk apk) {
                return (apk.minSdkVersion <= SDK_INT);
            }
        }

        @TargetApi(5)
        private static class EclairChecker extends CompatibilityChecker {

            private HashSet<String> features;
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
                Log.d("FDroid", logMsg.toString());
            }

            public boolean isCompatible(Apk apk) {
                if (apk.minSdkVersion > SDK_INT)
                    return false;
                if (apk.features != null) {
                    for (String feat : apk.features) {
                        if (ignoreTouchscreen
                                && feat.equals("android.hardware.touchscreen")) {
                            // Don't check it!
                        } else if (!features.contains(feat)) {
                            Log.d("FDroid", apk.id
                                    + " is incompatible based on lack of "
                                    + feat);
                            return false;
                        }
                    }
                }
                return true;
            }
        }
    }

    // The TABLE_REPO table stores the details of the repositories in use.
    private static final String TABLE_REPO = "fdroid_repo";
    private static final String CREATE_TABLE_REPO = "create table "
            + TABLE_REPO + " (id integer primary key, address text not null, "
            + "inuse integer not null, " + "priority integer not null,"
            + "pubkey text, lastetag text);";

    public static class Repo {
        public int id;
        public String address;
        public boolean inuse;
        public int priority;
        public String pubkey; // null for an unsigned repo
        public String lastetag; // last etag we updated from, null forces update
    }

    private final int DBVersion = 20;

    private static void createAppApk(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_APP);
        db.execSQL("create index app_id on " + TABLE_APP + " (id);");
        db.execSQL("create index app_category on " + TABLE_APP + " (category);");
        db.execSQL(CREATE_TABLE_APK);
        db.execSQL("create index apk_vercode on " + TABLE_APK + " (vercode);");
        db.execSQL("create index apk_id on " + TABLE_APK + " (id);");
    }

    public static void resetTransient(SQLiteDatabase db) {
        db.execSQL("drop table " + TABLE_APP);
        db.execSQL("drop table " + TABLE_APK);
        db.execSQL("update " + TABLE_REPO + " set lastetag = NULL");
        createAppApk(db);
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
            values.put("pubkey",
                    mContext.getString(R.string.default_repo_pubkey));
            values.put("inuse", 1);
            values.put("priority", 10);
            values.put("lastetag", (String) null);
            db.insert(TABLE_REPO, null, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            resetTransient(db);

            // Migrate repo list to new structure. (No way to change primary
            // key in sqlite - table must be recreated)
            if (oldVersion < 20) {
                Vector<Repo> oldrepos = new Vector<Repo>();
                Cursor c = db.rawQuery("select address, inuse, pubkey from "
                        + TABLE_REPO, null);
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

        }

    }

    public static File getDataPath() {
        return new File(Environment.getExternalStorageDirectory(), ".fdroid");
    }

    public static File getIconsPath() {
        return new File(getDataPath(), "icons");
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

    // Get the number of apps that have updates available. This can be a
    // time consuming operation.
    public int getNumUpdates() {
        Vector<App> apps = getApps(true);
        int count = 0;
        for (App app : apps) {
            if (app.hasUpdates)
                count++;
        }
        return count;
    }

    public Vector<String> getCategories() {
        Vector<String> result = new Vector<String>();
        Cursor c = null;
        try {
            c = db.rawQuery("select distinct category from " + TABLE_APP
                    + " order by category", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                String s = c.getString(0);
                if (s != null) {
                    result.add(s);
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

    // Populate the details for the given app, if necessary.
    // If 'apkrepo' is non-zero, only apks from that repo are
    // populated (this is used during the update process)
    public void populateDetails(App app, int apkrepo) {
        if (app.detail_Populated)
            return;
        Cursor c = null;
        try {
            String[] cols = new String[] { "description", "webURL",
                    "trackerURL", "sourceURL", "donateURL" };
            c = db.query(TABLE_APP, cols, "id = ?", new String[] { app.id },
                    null, null, null, null);
            c.moveToFirst();
            app.detail_description = c.getString(0);
            app.detail_webURL = c.getString(1);
            app.detail_trackerURL = c.getString(2);
            app.detail_sourceURL = c.getString(3);
            app.detail_donateURL = c.getString(4);
            c.close();
            c = null;

            cols = new String[] { "hash", "hashType", "size", "permissions" };
            for (Apk apk : app.apks) {

                if (apkrepo == 0 || apkrepo == apk.repo) {
                    c = db.query(
                            TABLE_APK,
                            cols,
                            "id = ? and vercode = ?",
                            new String[] { apk.id,
                                    Integer.toString(apk.vercode) }, null,
                            null, null, null);
                    c.moveToFirst();
                    apk.detail_hash = c.getString(0);
                    apk.detail_hashType = c.getString(1);
                    apk.detail_size = c.getInt(2);
                    apk.detail_permissions = CommaSeparatedList.make(c
                            .getString(3));
                    c.close();
                    c = null;
                }
            }
            app.detail_Populated = true;

        } finally {
            if (c != null)
                c.close();
        }
    }

    // Return a list of apps matching the given criteria. Filtering is
    // also done based on compatibility and anti-features according to
    // the user's current preferences.
    public Vector<App> getApps(boolean getinstalledinfo) {

        // If we're going to need it, get info in what's currently installed
        Map<String, PackageInfo> systemApks = null;
        if (getinstalledinfo) {
            Log.d("FDroid", "Reading installed packages");
            systemApks = new HashMap<String, PackageInfo>();
            List<PackageInfo> installedPackages = mContext.getPackageManager().getInstalledPackages(0);
            for (PackageInfo appInfo : installedPackages) {
                systemApks.put(appInfo.packageName, appInfo);
            }
        }

        Map<String, App> apps = new HashMap<String, App>();
        Cursor c = null;
        long startTime = System.currentTimeMillis();
        try {

            String cols[] = new String[] { "antiFeatures", "requirements",
                    "id", "name", "summary", "icon", "license", "category",
                    "curVersion", "curVercode", "added", "lastUpdated",
                    "compatible" };
            c = db.query(TABLE_APP, cols, null, null, null, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {

                App app = new App();
                app.antiFeatures = DB.CommaSeparatedList.make(c.getString(0));
                app.requirements = DB.CommaSeparatedList.make(c.getString(1));
                app.id = c.getString(2);
                app.name = c.getString(3);
                app.summary = c.getString(4);
                app.icon = c.getString(5);
                app.license = c.getString(6);
                app.category = c.getString(7);
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
                app.hasUpdates = false;

                if (getinstalledinfo && systemApks.containsKey(app.id)) {
                    PackageInfo sysapk = systemApks.get(app.id);
                    app.installedVersion = sysapk.versionName;
                    app.installedVerCode = sysapk.versionCode;
                } else {
                    app.installedVersion = null;
                    app.installedVerCode = 0;
                }

                apps.put(app.id, app);

                c.moveToNext();
            }
            c.close();
            c = null;

            Log.d("FDroid", "Read app data from database " + " (took "
                    + (System.currentTimeMillis() - startTime) + " ms)");

            cols = new String[] { "id", "version", "vercode", "sig", "srcname",
                    "apkName", "minSdkVersion", "added", "features",
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
                apk.compatible = c.getInt(9) == 1;
                apk.repo = c.getInt(10);
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

        Vector<App> result = new Vector<App>(apps.values());
        Collections.sort(result);

        // Fill in the hasUpdates fields if we have the necessary information...
        if (getinstalledinfo) {

            // We'll say an application has updates if it's installed AND the
            // installed version is not the 'current' one AND the installed
            // version is older than the current one.
            for (App app : result) {
                Apk curver = app.getCurrentVersion();
                if (curver != null && app.installedVersion != null
                        && !app.installedVersion.equals(curver.version)) {
                    if (app.installedVerCode < curver.vercode) {
                        app.hasUpdates = true;
                        app.updateVersion = curver.version;
                    }
                }
            }
        }

        return result;
    }

    public Vector<String> doSearch(String query) {

        Vector<String> ids = new Vector<String>();
        Cursor c = null;
        try {
            String filter = "%" + query + "%";
            c = db.query(TABLE_APP, new String[] { "id" },
                    "name like ? or summary like ? or description like ?",
                    new String[] { filter, filter, filter }, null, null, null);
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
    }

    private Vector<App> updateApps = null;

    // Called before a repo update starts. Returns the number of updates
    // available beforehand.
    public int beginUpdate(Vector<DB.App> apps) {
        // Get a list of all apps. All the apps and apks in this list will
        // have 'updated' set to false at this point, and we will only set
        // it to true when we see the app/apk in a repository. Thus, at the
        // end, any that are still false can be removed.
        updateApps = apps;
        Log.d("FDroid", "AppUpdate: " + updateApps.size()
                + " apps before starting.");
        // Wrap the whole update in a transaction. Make sure to call
        // either endUpdate or cancelUpdate to commit or discard it,
        // respectively.
        db.beginTransaction();

        int count = 0;
        for (App app : updateApps) {
            if (app.hasUpdates)
                count++;
        }
        return count;
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

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        boolean prefCompat = prefs.getBoolean("showIncompatible", false);

        // See if it's compatible (by which we mean if it has at least one
        // compatible apk - if it's not, leave it out)
        // Also keep a list of which were compatible, because they're the
        // only ones we'll add, unless the showIncompatible preference is set.
        Vector<Apk> compatibleapks = new Vector<Apk>();
        for (Apk apk : upapp.apks) {
            if (compatChecker.isCompatible(apk)) {
                apk.compatible = true;
                compatibleapks.add(apk);
            }
        }
        if (compatibleapks.size() > 0)
            upapp.compatible = true;
        if (prefCompat)
            compatibleapks = upapp.apks;
        if (compatibleapks.size() == 0)
            return false;

        boolean found = false;
        for (App app : updateApps) {
            if (app.id.equals(upapp.id)) {
                updateApp(app, upapp);
                app.updated = true;
                found = true;
                for (Apk upapk : compatibleapks) {
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
            for (Apk upapk : compatibleapks) {
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
        values.put("category", upapp.category);
        values.put("webURL", upapp.detail_webURL);
        values.put("trackerURL", upapp.detail_trackerURL);
        values.put("sourceURL", upapp.detail_sourceURL);
        values.put("donateURL", upapp.detail_donateURL);
        values.put("added",
                upapp.added == null ? "" : mDateFormat.format(upapp.added));
        values.put(
                "lastUpdated",
                upapp.added == null ? "" : mDateFormat
                        .format(upapp.lastUpdated));
        values.put("curVersion", upapp.curVersion);
        values.put("curVercode", upapp.curVercode);
        values.put("antiFeatures", CommaSeparatedList.str(upapp.antiFeatures));
        values.put("requirements", CommaSeparatedList.str(upapp.requirements));
        values.put("compatible", upapp.compatible ? 1 : 0);
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
        values.put("compatible", upapk.compatible ? 1 : 0);
        if (oldapk != null) {
            db.update(TABLE_APK, values,
                    "id = ? and vercode = " + Integer.toString(oldapk.vercode),
                    new String[] { oldapk.id });
        } else {
            db.insert(TABLE_APK, null, values);
        }
    }

    // Get details of a repo, given the ID. Returns null if the repo
    // doesn't exist.
    public Repo getRepo(int id) {
        Cursor c = null;
        try {
            c = db.query(TABLE_REPO, new String[] { "address, inuse",
                    "priority", "pubkey", "lastetag" },
                    "id = " + Integer.toString(id), null, null, null, null);
            if (!c.moveToFirst())
                return null;
            Repo repo = new Repo();
            repo.id = id;
            repo.address = c.getString(0);
            repo.inuse = (c.getInt(1) == 1);
            repo.priority = c.getInt(2);
            repo.pubkey = c.getString(3);
            repo.lastetag = c.getString(4);
            return repo;
        } finally {
            if (c != null)
                c.close();
        }
    }

    // Get a list of the configured repositories.
    public Vector<Repo> getRepos() {
        Vector<Repo> repos = new Vector<Repo>();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "select id, address, inuse, priority, pubkey, lastetag from "
                            + TABLE_REPO + " order by priority", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Repo repo = new Repo();
                repo.id = c.getInt(0);
                repo.address = c.getString(1);
                repo.inuse = (c.getInt(2) == 1);
                repo.priority = c.getInt(3);
                repo.pubkey = c.getString(4);
                repo.lastetag = c.getString(5);
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

    public void updateRepoByAddress(Repo repo) {
        ContentValues values = new ContentValues();
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

    public void addRepo(String address, int priority, String pubkey,
            boolean inuse) {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("inuse", inuse ? 1 : 0);
        values.put("priority", priority);
        values.put("pubkey", pubkey);
        values.put("lastetag", (String) null);
        db.insert(TABLE_REPO, null, values);
    }

    public void removeServers(Vector<String> addresses) {
        db.beginTransaction();
        try {
            for (String address : addresses) {
                db.delete(TABLE_REPO, "address = ?", new String[] { address });
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
