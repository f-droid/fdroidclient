/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;

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
            + "installedVersion text," + "hasUpdates int not null,"
            + "primary key(id));";

    public static class App {

        public App() {
            name = "Unknown";
            summary = "Unknown application";
            icon = "noicon.png";
            id = "unknown";
            license = "Unknown";
            category = "Uncategorized";
            trackerURL = "";
            sourceURL = "";
            donateURL = null;
            webURL = "";
            antiFeatures = null;
            requirements = null;
            hasUpdates = false;
            updated = false;
            added = null;
            lastUpdated = null;
            apks = new Vector<Apk>();
        }

        public String id;
        public String name;
        public String summary;
        public String icon;
        public String description;
        public String license;
        public String category;
        public String webURL;
        public String trackerURL;
        public String sourceURL;
        public String donateURL; // Donate link, or null
        public String installedVersion;
        public String currentVersion;
        public int installedVerCode;
        public String marketVersion;
        public int marketVercode;
        public Date added;
        public Date lastUpdated;

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

        // Used internally for tracking during repo updates.
        public boolean updated;

        public Vector<Apk> apks;

        // Get the current version - this will be one of the Apks from 'apks'.
        // Can return null if there are no available versions.
        // This should be the 'current' version, as in the most recent stable
        // one, that most users would want by default. It might not be the
        // most recent, if for example there are betas etc.
        // To skip compatibility checks, pass null as the checker.
        public Apk getCurrentVersion(DB.Apk.CompatibilityChecker checker) {

            // Try and return the real current version first...
            if (marketVersion != null && marketVercode > 0) {
                for (Apk apk : apks) {
                    if (apk.vercode == marketVercode
                            && (checker == null || checker.isCompatible(apk)))
                        return apk;
                }
            }

            // If we don't know the current version, or we don't have it, we
            // return the most recent compatible version we have...
            int latestcode = -1;
            Apk latestapk = null;
            for (Apk apk : apks) {
                if (apk.vercode > latestcode
                        && (checker == null || checker.isCompatible(apk))) {
                    latestapk = apk;
                    latestcode = apk.vercode;
                }
            }
            return latestapk;
        }

    }

    // The TABLE_APK table stores details of all the application versions we
    // know about. Each relates directly back to an entry in TABLE_APP.
    // This information is retrieved from the repositories.
    private static final String TABLE_APK = "fdroid_apk";
    private static final String CREATE_TABLE_APK = "create table " + TABLE_APK
            + " ( " + "id text not null, " + "version text not null, "
            + "server text not null, " + "hash text not null, "
            + "vercode int not null," + "apkName text not null, "
            + "size int not null," + "primary key(id,version));";

    public static class Apk {

        public Apk() {
            updated = false;
            size = 0;
            apkSource = null;
            added = null;
        }

        public String id;
        public String version;
        public int vercode;
        public int size; // Size in bytes - 0 means we don't know!
        public String server;
        public String hash;
        public String hashType;
        public int minSdkVersion; // 0 if unknown
        public Date added;
        public CommaSeparatedList permissions; // null if empty or unknown
        public CommaSeparatedList features; // null if empty or unknown

        // ID (md5 sum of public key) of signature. Might be null, in the
        // transition to this field existing.
        public String sig;

        public String apkName;

        // If null, the apk comes from the same server as the repo index.
        // Otherwise this is the complete URL to download the apk from.
        public String apkSource;

        // If not null, this is the name of the source tarball for the
        // application. Null indicates that it's a developer's binary
        // build - otherwise it's built from source.
        public String srcname;

        // Used internally for tracking during repo updates.
        public boolean updated;

        public String getURL() {
            String path = apkName.replace(" ", "%20");
            return server + "/" + path;
        }

        // Call isCompatible(apk) on an instance of this class to
        // check if an APK is compatible with the user's device.
        public static abstract class CompatibilityChecker {

            // Because Build.VERSION.SDK_INT requires API level 5
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

        private static class EclairChecker extends CompatibilityChecker {

            private HashSet<String> features;

            public EclairChecker(Context ctx) {
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
                        if (!features.contains(feat)) {
                            Log.d("FDroid", "Incompatible based on lack of "
                                    + feat);
                            return false;
                        }
                    }
                }
                return true;
            }
        }
    }

    // Let other classes reuse the already instantiated compatibility
    // checker, mostly to avoid redundant log output.
    public Apk.CompatibilityChecker getCompatibilityChecker() {
        return compatChecker;
    }

    // The TABLE_REPO table stores the details of the repositories in use.
    private static final String TABLE_REPO = "fdroid_repo";
    private static final String CREATE_TABLE_REPO = "create table "
            + TABLE_REPO + " (" + "address text primary key, "
            + "inuse integer not null, " + "priority integer not null);";

    public static class Repo {
        public String address;
        public boolean inuse;
        public int priority;
        public String pubkey; // null for an unsigned repo
    }

    // SQL to update the database to versions beyond the first. Here is
    // how the database works:
    //
    // * The SQL to create the database tables always creates version
    // 1. This SQL will never be altered.
    // * In the array below there is SQL for each subsequent version
    // from 2 onwards.
    // * For a new install, the database is always initialised to version
    // 1.
    // * Then, whether it's a new install or not, all the upgrade SQL in
    // the array below is executed in order to bring the database up to
    // the latest version.
    // * The current version is tracked by an entry in the TABLE_VERSION
    // table.
    //
    private static final String[][] DB_UPGRADES = {

            // Version 2...
            { "alter table " + TABLE_APP + " add marketVersion text",
                    "alter table " + TABLE_APP + " add marketVercode integer" },

            // Version 3...
            { "alter table " + TABLE_APK + " add apkSource text" },

            // Version 4...
            { "alter table " + TABLE_APP + " add installedVerCode integer" },

            // Version 5...
            { "alter table " + TABLE_APP + " add antiFeatures string" },

            // Version 6...
            { "alter table " + TABLE_APK + " add sig string" },

            // Version 7...
            { "alter table " + TABLE_REPO + " add pubkey string" },

            // Version 8...
            { "alter table " + TABLE_APP + " add donateURL string" },

            // Version 9...
            { "alter table " + TABLE_APK + " add srcname string" },

            // Version 10...
            { "alter table " + TABLE_APK + " add minSdkVersion integer",
                    "alter table " + TABLE_APK + " add permissions string",
                    "alter table " + TABLE_APK + " add features string" },

            // Version 11...
            { "alter table " + TABLE_APP + " add requirements string" },

            // Version 12...
            { "alter table " + TABLE_APK + " add hashType string",
                    "update " + TABLE_APK + " set hashType = 'MD5'" },

            // Version 13...
            { "alter table " + TABLE_APP + " add category string" },

            // Version 14...
            { "alter table " + TABLE_APK + " add added string",
                    "alter table " + TABLE_APP + " add added string",
                    "alter table " + TABLE_APP + " add lastUpdated string" } };

    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DB_UPGRADES.length + 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_REPO);
            db.execSQL(CREATE_TABLE_APP);
            db.execSQL(CREATE_TABLE_APK);
            onUpgrade(db, 1, DB_UPGRADES.length + 1);
            ContentValues values = new ContentValues();
            values.put("address",
                    mContext.getString(R.string.default_repo_address));
            values.put("pubkey",
                    mContext.getString(R.string.default_repo_pubkey));
            values.put("inuse", 1);
            values.put("priority", 10);
            db.insert(TABLE_REPO, null, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            for (int v = oldVersion + 1; v <= newVersion; v++)
                for (int i = 0; i < DB_UPGRADES[v - 2].length; i++)
                    db.execSQL(DB_UPGRADES[v - 2][i]);
        }

    }

    public static String getIconsPath() {
        return "/sdcard/.fdroid/icons/";
    }

    private PackageManager mPm;
    private Context mContext;
    private Apk.CompatibilityChecker compatChecker;

    // The date format used for storing dates (e.g. lastupdated, added) in the
    // database.
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private DB(Context ctx) {

        mContext = ctx;
        DBHelper h = new DBHelper(ctx);
        db = h.getWritableDatabase();
        mPm = ctx.getPackageManager();
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
        compatChecker = Apk.CompatibilityChecker.getChecker(ctx);
    }

    public void close() {
        db.close();
        db = null;
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

    // Get the number of apps that have updates available.
    public int getNumUpdates() {
        Vector<App> apps = getApps(null, null, false, true);
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
                String s = c.getString(c.getColumnIndex("category"));
                Log.d("FDroid", "Category: " + s);
                if (s == null) {
                    s = "none";
                }
                result.add(s);
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

    // Return a list of apps matching the given criteria. Filtering is
    // also done based on compatibility and anti-features according to
    // the user's current preferences.
    // 'appid' - specific app id to retrieve, or null
    // 'filter' - search text to filter on, or null
    // 'update' - update installed version information from device, rather than
    // simply using values cached in the database. Slower.
    // 'exclusions' - apply filtering for compatibility, anti-features, etc.
    public Vector<App> getApps(String appid, String filter, boolean update,
            boolean exclusions) {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        boolean pref_antiAds = prefs.getBoolean("antiAds", false);
        boolean pref_antiTracking = prefs.getBoolean("antiTracking", false);
        boolean pref_antiNonFreeAdd = prefs.getBoolean("antiNonFreeAdd", false);
        boolean pref_antiNonFreeNet = prefs.getBoolean("antiNonFreeNet", false);
        boolean pref_antiNonFreeDep = prefs.getBoolean("antiNonFreeDep", false);
        boolean pref_showIncompat = prefs.getBoolean("showIncompatible", false);
        boolean pref_rooted = prefs.getBoolean("rooted", true);

        Vector<App> result = new Vector<App>();
        Cursor c = null;
        Cursor c2 = null;
        try {

            String query = "select * from " + TABLE_APP;
            if (appid != null) {
                query += " where id = '" + appid + "'";
            } else if (filter != null) {
                query += " where name like '%" + filter + "%'"
                        + " or description like '%" + filter + "%'";
            }
            query += " order by name collate nocase";

            c = db.rawQuery(query, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {

                App app = new App();
                app.antiFeatures = DB.CommaSeparatedList.make(c.getString(c
                        .getColumnIndex("antiFeatures")));
                boolean include = true;
                if (app.antiFeatures != null && exclusions) {
                    for (String af : app.antiFeatures) {
                        if (af.equals("Ads") && !pref_antiAds)
                            include = false;
                        else if (af.equals("Tracking") && !pref_antiTracking)
                            include = false;
                        else if (af.equals("NonFreeNet")
                                && !pref_antiNonFreeNet)
                            include = false;
                        else if (af.equals("NonFreeAdd")
                                && !pref_antiNonFreeAdd)
                            include = false;
                        else if (af.equals("NonFreeDep")
                                && !pref_antiNonFreeDep)
                            include = false;
                    }
                }
                app.requirements = DB.CommaSeparatedList.make(c.getString(c
                        .getColumnIndex("requirements")));
                if (app.requirements != null && exclusions) {
                    for (String r : app.requirements) {
                        if (r.equals("root") && !pref_rooted) {
                            include = false;
                        }
                    }
                }

                if (include) {
                    app.id = c.getString(c.getColumnIndex("id"));
                    app.name = c.getString(c.getColumnIndex("name"));
                    app.summary = c.getString(c.getColumnIndex("summary"));
                    app.icon = c.getString(c.getColumnIndex("icon"));
                    app.description = c.getString(c
                            .getColumnIndex("description"));
                    app.license = c.getString(c.getColumnIndex("license"));
                    app.category = c.getString(c.getColumnIndex("category"));
                    app.webURL = c.getString(c.getColumnIndex("webURL"));
                    app.trackerURL = c
                            .getString(c.getColumnIndex("trackerURL"));
                    app.sourceURL = c.getString(c.getColumnIndex("sourceURL"));
                    app.donateURL = c.getString(c.getColumnIndex("donateURL"));
                    app.installedVersion = c.getString(c
                            .getColumnIndex("installedVersion"));
                    app.installedVerCode = c.getInt(c
                            .getColumnIndex("installedVerCode"));
                    app.marketVersion = c.getString(c
                            .getColumnIndex("marketVersion"));
                    app.marketVercode = c.getInt(c
                            .getColumnIndex("marketVercode"));
                    String sAdded = c.getString(c.getColumnIndex("added"));
                    app.added = (sAdded == null || sAdded.length() == 0) ? null
                            : mDateFormat.parse(sAdded);
                    String sLastUpdated = c.getString(c
                            .getColumnIndex("lastUpdated"));
                    app.lastUpdated = (sLastUpdated == null || sLastUpdated
                            .length() == 0) ? null : mDateFormat
                            .parse(sLastUpdated);
                    app.hasUpdates = false;

                    c2 = db.rawQuery("select * from " + TABLE_APK
                            + " where id = ? order by vercode desc",
                            new String[] { app.id });
                    c2.moveToFirst();
                    boolean compatible = pref_showIncompat || !exclusions;
                    while (!c2.isAfterLast()) {
                        Apk apk = new Apk();
                        apk.id = app.id;
                        apk.version = c2
                                .getString(c2.getColumnIndex("version"));
                        apk.vercode = c2.getInt(c2.getColumnIndex("vercode"));
                        apk.server = c2.getString(c2.getColumnIndex("server"));
                        apk.hash = c2.getString(c2.getColumnIndex("hash"));
                        apk.hashType = c2.getString(c2
                                .getColumnIndex("hashType"));
                        apk.sig = c2.getString(c2.getColumnIndex("sig"));
                        apk.srcname = c2
                                .getString(c2.getColumnIndex("srcname"));
                        apk.size = c2.getInt(c2.getColumnIndex("size"));
                        apk.apkName = c2
                                .getString(c2.getColumnIndex("apkName"));
                        apk.apkSource = c2.getString(c2
                                .getColumnIndex("apkSource"));
                        apk.minSdkVersion = c2.getInt(c2
                                .getColumnIndex("minSdkVersion"));
                        String sApkAdded = c2.getString(c2
                                .getColumnIndex("added"));
                        apk.added = (sApkAdded == null || sApkAdded.length() == 0) ? null
                                : mDateFormat.parse(sApkAdded);
                        apk.permissions = CommaSeparatedList.make(c2
                                .getString(c2.getColumnIndex("permissions")));
                        apk.features = CommaSeparatedList.make(c2.getString(c2
                                .getColumnIndex("features")));
                        app.apks.add(apk);
                        if (!compatible && compatChecker.isCompatible(apk)) {
                            // At least one compatible APK.
                            compatible = true;
                        }
                        c2.moveToNext();
                    }
                    c2.close();

                    if (compatible) {
                        result.add(app);
                    } else {
                        Log.d("FDroid", "Excluding incompatible application: "
                                + app.id);
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
            if (c2 != null) {
                c2.close();
            }
        }

        if (update) {
            db.beginTransaction();
            try {
                getUpdates(result);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e("FDroid",
                        "Exception while getting updates: "
                                + Log.getStackTraceString(e));
            } finally {
                db.endTransaction();
            }
        }

        // We'll say an application has updates if it's installed AND the
        // installed version is not the 'current' one AND the installed
        // version is older than the current one.
        for (App app : result) {
            Apk curver = app.getCurrentVersion(compatChecker);
            if (curver != null && app.installedVersion != null
                    && !app.installedVersion.equals(curver.version)) {
                if (app.installedVerCode < curver.vercode) {
                    app.hasUpdates = true;
                    app.currentVersion = curver.version;
                }
            }
        }

        return result;
    }

    // Verify installed status against the system's package list.
    private void getUpdates(Vector<DB.App> apps) {
        List<PackageInfo> installedPackages = mPm.getInstalledPackages(0);
        Map<String, PackageInfo> systemApks = new HashMap<String, PackageInfo>();
        Log.d("FDroid", "Reading installed packages");
        for (PackageInfo appInfo : installedPackages) {
            systemApks.put(appInfo.packageName, appInfo);
        }

        for (DB.App app : apps) {
            if (systemApks.containsKey(app.id)) {
                PackageInfo sysapk = systemApks.get(app.id);
                String version = sysapk.versionName;
                int vercode = sysapk.versionCode;
                if (app.installedVersion == null
                        || !app.installedVersion.equals(version)) {
                    setInstalledVersion(app.id, version, vercode);
                    app.installedVersion = version;
                    app.installedVerCode = vercode;
                }
            } else {
                if (app.installedVersion != null) {
                    setInstalledVersion(app.id, null, 0);
                    app.installedVersion = null;
                }
            }
        }
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

    // Called before a repo update starts.
    public void beginUpdate() {
        // Get a list of all apps. All the apps and apks in this list will
        // have 'updated' set to false at this point, and we will only set
        // it to true when we see the app/apk in a repository. Thus, at the
        // end, any that are still false can be removed.
        // TODO: Need to ensure that UI and UpdateService can't both be doing
        // an update at the same time.
        updateApps = getApps(null, null, true, false);
        Log.d("FDroid", "AppUpdate: " + updateApps.size()
                + " apps before starting.");
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
    public void updateApplication(App upapp) {

        if (updateApps == null) {
            return;
        }

        boolean found = false;
        for (App app : updateApps) {
            if (app.id.equals(upapp.id)) {
                // Log.d("FDroid", "AppUpdate: " + app.id
                // + " is already in the database.");
                updateApp(app, upapp);
                app.updated = true;
                found = true;
                for (Apk upapk : upapp.apks) {
                    boolean afound = false;
                    for (Apk apk : app.apks) {
                        if (apk.version.equals(upapk.version)) {
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
                        // Log.d("FDroid", "AppUpdate: " + upapk.version
                        // + " is a new version.");
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
            // Log
            // .d("FDroid", "AppUpdate: " + upapp.id
            // + " is a new application.");
            updateApp(null, upapp);
            for (Apk upapk : upapp.apks) {
                updateApkIfDifferent(null, upapk);
                upapk.updated = true;
            }
            upapp.updated = true;
            updateApps.add(upapp);
        }

    }

    // Update application details in the database.
    // 'oldapp' - previous details - i.e. what's in the database.
    // If null, this app is not in the database at all and
    // should be added.
    // 'upapp' - updated details
    // Note that we deliberately do not update installedVersion or
    // installedVerCode here - for a new app, they default correctly,
    // and for an existing app we want to keep that cached data.
    private void updateApp(App oldapp, App upapp) {
        ContentValues values = new ContentValues();
        values.put("id", upapp.id);
        values.put("name", upapp.name);
        values.put("summary", upapp.summary);
        values.put("icon", upapp.icon);
        values.put("description", upapp.description);
        values.put("license", upapp.license);
        values.put("category", upapp.category);
        values.put("webURL", upapp.webURL);
        values.put("trackerURL", upapp.trackerURL);
        values.put("sourceURL", upapp.sourceURL);
        values.put("donateURL", upapp.donateURL);
        values.put("added",
                upapp.added == null ? "" : mDateFormat.format(upapp.added));
        values.put(
                "lastUpdated",
                upapp.added == null ? "" : mDateFormat
                        .format(upapp.lastUpdated));
        values.put("marketVersion", upapp.marketVersion);
        values.put("marketVercode", upapp.marketVercode);
        values.put("antiFeatures", CommaSeparatedList.str(upapp.antiFeatures));
        values.put("requirements", CommaSeparatedList.str(upapp.requirements));
        values.put("hasUpdates", upapp.hasUpdates ? 1 : 0);
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
        values.put("server", upapk.server);
        values.put("hash", upapk.hash);
        values.put("hashType", upapk.hashType);
        values.put("sig", upapk.sig);
        values.put("srcname", upapk.srcname);
        values.put("size", upapk.size);
        values.put("apkName", upapk.apkName);
        values.put("apkSource", upapk.apkSource);
        values.put("minSdkVersion", upapk.minSdkVersion);
        values.put("added",
                upapk.added == null ? "" : mDateFormat.format(upapk.added));
        values.put("permissions", CommaSeparatedList.str(upapk.permissions));
        values.put("features", CommaSeparatedList.str(upapk.features));
        if (oldapk != null) {
            db.update(TABLE_APK, values, "id = ? and version =?", new String[] {
                    oldapk.id, oldapk.version });
        } else {
            db.insert(TABLE_APK, null, values);
        }
    }

    public void setInstalledVersion(String id, String version, int vercode) {
        ContentValues values = new ContentValues();
        values.put("installedVersion", version);
        values.put("installedVerCode", vercode);
        db.update(TABLE_APP, values, "id = ?", new String[] { id });
    }

    // Get a list of the configured repositories.
    public Vector<Repo> getRepos() {
        Vector<Repo> repos = new Vector<Repo>();
        Cursor c = null;
        try {
            c = db.rawQuery("select address, inuse, priority, pubkey from "
                    + TABLE_REPO + " order by priority", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Repo repo = new Repo();
                repo.address = c.getString(0);
                repo.inuse = (c.getInt(1) == 1);
                repo.priority = c.getInt(2);
                repo.pubkey = c.getString(3);
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
                + " set inuse=1-inuse where address = ?",
                new String[] { address });
    }

    public void updateRepoByAddress(Repo repo) {
        ContentValues values = new ContentValues();
        values.put("inuse", repo.inuse);
        values.put("priority", repo.priority);
        values.put("pubkey", repo.pubkey);
        db.update(TABLE_REPO, values, "address = ?",
                new String[] { repo.address });
    }

    public void addServer(String address, int priority, String pubkey) {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("inuse", 1);
        values.put("priority", priority);
        values.put("pubkey", pubkey);
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
