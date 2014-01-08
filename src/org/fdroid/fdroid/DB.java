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
import java.security.MessageDigest;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.DisplayMetrics;
import android.util.Log;

import org.fdroid.fdroid.compat.Compatibility;
import org.fdroid.fdroid.compat.ContextCompat;
import org.fdroid.fdroid.compat.SupportedArchitectures;
import org.fdroid.fdroid.data.DBHelper;

public class DB {

    private static Semaphore dbSync = new Semaphore(1, true);
    private static DB dbInstance = null;

    // Initialise the database. Called once when the application starts up.
    static void initDB(Context ctx) {
        dbInstance = new DB(ctx);
    }

    // Get access to the database. Must be called before any database activity,
    // and releaseDB must be called subsequently. Returns null in the event of
    // failure.
    public static DB getDB() {
        try {
            dbSync.acquire();
            return dbInstance;
        } catch (InterruptedException e) {
            return null;
        }
    }

    // Release database access lock acquired via getDB().
    public static void releaseDB() {
        dbSync.release();
    }

    // Possible values of the SQLite flag "synchronous"
    public static final int SYNC_OFF = 0;
    public static final int SYNC_NORMAL = 1;
    public static final int SYNC_FULL = 2;

    private SQLiteDatabase db;

    public static final String TABLE_APP = "fdroid_app";

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
            detail_dogecoinAddr = null;
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

        // Dogecoin donate address, or null
        // Null when !detail_Populated
        public String detail_dogecoinAddr;

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
                    if ((!this.compatible || apk.compatible)
                            && apk.vercode <= curVercode
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
                    if ((!this.compatible || apk.compatible)
                            && apk.vercode > latestcode) {
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
    public static final String TABLE_APK = "fdroid_apk";

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
        private static class CompatibilityChecker extends Compatibility {

            private HashSet<String> features;
            private HashSet<String> cpuAbis;
            private String cpuAbisDesc;
            private boolean ignoreTouchscreen;

            public CompatibilityChecker(Context ctx) {

                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(ctx);
                ignoreTouchscreen = prefs
                        .getBoolean("ignoreTouchscreen", false);

                PackageManager pm = ctx.getPackageManager();
                StringBuilder logMsg = new StringBuilder();
                logMsg.append("Available device features:");
                features = new HashSet<String>();
                if (pm != null) {
                    for (FeatureInfo fi : pm.getSystemAvailableFeatures()) {
                        features.add(fi.name);
                        logMsg.append('\n');
                        logMsg.append(fi.name);
                    }
                }

                cpuAbis = SupportedArchitectures.getAbis();

                StringBuilder builder = new StringBuilder();
                boolean first = true;
                for (String abi : cpuAbis) {
                    if (first) first = false;
                    else builder.append(", ");
                    builder.append(abi);
                }
                cpuAbisDesc = builder.toString();
                builder = null;

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
                            + " only supports " + CommaSeparatedList.str(apk.nativecode)
                            + " while your architectures are " + cpuAbisDesc);
                    return false;
                }
                return true;
            }
        }
    }

    // The TABLE_REPO table stores the details of the repositories in use.
    public static final String TABLE_REPO = "fdroid_repo";

    public static class Repo {
        public int id;
        public String address;
        public String name;
        public String description;
        public int version; // index version, i.e. what fdroidserver built it - 0 if not specified
        public boolean inuse;
        public int priority;
        public String pubkey; // null for an unsigned repo
        public String fingerprint; // always null for an unsigned repo
        public int maxage; // maximum age of index that will be accepted - 0 for any
        public String lastetag; // last etag we updated from, null forces update
        public Date lastUpdated;

        /**
         * If we haven't run an update for this repo yet, then the name
         * will be unknown, in which case we will just take a guess at an
         * appropriate name based on the url (e.g. "fdroid.org/archive")
         */
        public String getName() {
            if (name == null) {
                String tempName = null;
                try {
                    URL url = new URL(address);
                    tempName = url.getHost() + url.getPath();
                } catch (MalformedURLException e) {
                    tempName = address;
                }
                return tempName;
            } else {
                return name;
            }
        }

        public String toString() {
            return address;
        }

        public int getNumberOfApps() {
            DB db = DB.getDB();
            int count = db.countAppsForRepo(id);
            DB.releaseDB();
            return count;
        }

        /**
         * @param application In order invalidate the list of apps, we require
         *                    a reference to the top level application.
         */
        public void enable(FDroidApp application) {
            try {
                DB db = DB.getDB();
                List<DB.Repo> toEnable = new ArrayList<DB.Repo>(1);
                toEnable.add(this);
                db.enableRepos(toEnable);
            } finally {
                DB.releaseDB();
            }
            application.invalidateAllApps();
        }

        /**
         * @param application See DB.Repo.enable(application)
         */
        public void disable(FDroidApp application) {
            disableRemove(application, false);
        }

        /**
         * @param application See DB.Repo.enable(application)
         */
        public void remove(FDroidApp application) {
            disableRemove(application, true);
        }

        /**
         * @param application See DB.Repo.enable(application)
         */
        private void disableRemove(FDroidApp application, boolean removeAfterDisabling) {
            try {
                DB db = DB.getDB();
                List<DB.Repo> toDisable = new ArrayList<DB.Repo>(1);
                toDisable.add(this);
                db.doDisableRepos(toDisable, removeAfterDisabling);
            } finally {
                DB.releaseDB();
            }
            application.invalidateAllApps();
        }

        public boolean isSigned() {
            return this.pubkey != null && this.pubkey.length() > 0;
        }

        public boolean hasBeenUpdated() {
            return this.lastetag != null;
        }
    }

    private int countAppsForRepo(int id) {
        String[] selection     = { "COUNT(distinct id)" };
        String[] selectionArgs = { Integer.toString(id) };
        Cursor result = db.query(
        TABLE_APK, selection, "repo = ?", selectionArgs, "repo", null, null);
        if (result.getCount() > 0) {
            result.moveToFirst();
            return result.getInt(0);
        } else {
            return 0;
        }
    }

    public static String calcFingerprint(String pubkey) {
        String ret = null;
        if (pubkey == null)
            return null;
        try {
            // keytool -list -v gives you the SHA-256 fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Hasher.unhex(pubkey));
            byte[] fingerprint = digest.digest();
            Formatter formatter = new Formatter(new StringBuilder());
            for (int i = 1; i < fingerprint.length; i++) {
                formatter.format("%02X", fingerprint[i]);
            }
            ret = formatter.toString();
            formatter.close();
        } catch (Exception e) {
            Log.w("FDroid", "Unable to get certificate fingerprint.\n"
                    + Log.getStackTraceString(e));
        }
        return ret;
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
    public static SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd", Locale.ENGLISH);

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

    // Delete the database, which should cause it to be re-created next time
    // it's used.
    public static void delete(Context ctx) {
        try {
            ctx.deleteDatabase(DBHelper.DATABASE_NAME);
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
                    null, null, null, null, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                CommaSeparatedList categories = CommaSeparatedList
                    .make(c.getString(0));
                if (categories != null) {
                    for (String category : categories) {
                        if (!result.contains(category)) {
                            result.add(category);
                        }
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
        Collections.sort(result);
        return result;
    }

    private static final String[] POPULATE_APP_COLS = new String[] {
        "description", "webURL", "trackerURL", "sourceURL",
        "donateURL", "bitcoinAddr", "flattrID", "litecoinAddr", "dogecoinAddr" };

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
            app.detail_dogecoinAddr = cursor.getString(8);
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
                        : dateFormat.parse(sAdded);
                String sLastUpdated = c.getString(11);
                app.lastUpdated = (sLastUpdated == null || sLastUpdated
                        .length() == 0) ? null : dateFormat
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
                    if (sysapk.applicationInfo != null) {
                        app.userInstalled = ((sysapk.applicationInfo.flags
                                & ApplicationInfo.FLAG_SYSTEM) != 1);
                    }
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

            List<Repo> repos = getRepos();
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            cols = new String[] { "id", "version", "vercode", "sig", "srcname",
                    "apkName", "minSdkVersion", "added", "features", "nativecode",
                    "compatible", "repo" };
            c = db.query(TABLE_APK, cols, null, null, null, null,
                    "vercode desc");
            c.moveToFirst();

            DisplayMetrics metrics = mContext.getResources()
                .getDisplayMetrics();
            String iconsDir = null;
            if (metrics.densityDpi >= 640) {
                iconsDir = "/icons-640/";
            } else if (metrics.densityDpi >= 480) {
                iconsDir = "/icons-480/";
            } else if (metrics.densityDpi >= 320) {
                iconsDir = "/icons-320/";
            } else if (metrics.densityDpi >= 240) {
                iconsDir = "/icons-240/";
            } else if (metrics.densityDpi >= 160) {
                iconsDir = "/icons-160/";
            } else {
                iconsDir = "/icons-120/";
            }
            metrics = null;

            while (!c.isAfterLast()) {
                String id = c.getString(0);
                App app = apps.get(id);
                if (app == null) {
                    c.moveToNext();
                    continue;
                }
                boolean compatible = c.getInt(10) == 1;
                int repoid = c.getInt(11);
                Apk apk = new Apk();
                apk.id = id;
                apk.version = c.getString(1);
                apk.vercode = c.getInt(2);
                apk.sig = c.getString(3);
                apk.srcname = c.getString(4);
                apk.apkName = c.getString(5);
                apk.minSdkVersion = c.getInt(6);
                String sApkAdded = c.getString(7);
                apk.added = (sApkAdded == null || sApkAdded.length() == 0) ? null
                        : dateFormat.parse(sApkAdded);
                apk.features = CommaSeparatedList.make(c.getString(8));
                apk.nativecode = CommaSeparatedList.make(c.getString(9));
                apk.compatible = compatible;
                apk.repo = repoid;
                app.apks.add(apk);
                if (app.iconUrl == null && app.icon != null) {
                    for (DB.Repo repo : repos) {
                        if (repo.id != repoid) continue;
                        if (repo.version >= 11) {
                            app.iconUrl = repo.address + iconsDir + app.icon;
                        } else {
                            app.iconUrl = repo.address + "/icons/" + app.icon;
                        }
                        break;
                    }
                }
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

        @Override
        public String toString() {
            return value;
        }

        @Override
        public Iterator<String> iterator() {
            SimpleStringSplitter splitter = new SimpleStringSplitter(',');
            splitter.setString(value);
            return splitter.iterator();
        }

        public boolean contains(String v) {
            for (String s : this) {
                if (s.equals(v))
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

        // Lazy initialise this...
        if (compatChecker == null) {
            compatChecker = new Apk.CompatibilityChecker(mContext);
        }

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
        values.put("dogecoinAddr", upapp.detail_dogecoinAddr);
        values.put("flattrID", upapp.detail_flattrID);
        values.put("added",
                upapp.added == null ? "" : dateFormat.format(upapp.added));
        values.put(
                "lastUpdated",
                upapp.added == null ? "" : dateFormat
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
                upapk.added == null ? "" : dateFormat.format(upapk.added));
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
                "description", "version", "inuse", "priority", "pubkey",
                "fingerprint", "maxage", "lastetag", "lastUpdated" },
                    "id = ?", new String[] { Integer.toString(id) }, null, null, null);
            if (!c.moveToFirst())
                return null;
            Repo repo = new Repo();
            repo.id = id;
            repo.address = c.getString(0);
            repo.name = c.getString(1);
            repo.description = c.getString(2);
            repo.version = c.getInt(3);
            repo.inuse = (c.getInt(4) == 1);
            repo.priority = c.getInt(5);
            repo.pubkey = c.getString(6);
            repo.fingerprint = c.getString(7);
            repo.maxage = c.getInt(8);
            repo.lastetag = c.getString(9);
            try {
                repo.lastUpdated =  c.getString(10) != null ?
                    dateFormat.parse( c.getString(10)) :
                    null;
            } catch (ParseException e) {
                Log.e("FDroid", "Error parsing date " + c.getString(10));
            }
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
                "description", "version", "inuse", "priority", "pubkey",
                "fingerprint", "maxage", "lastetag" },
                    null, null, null, null, "priority");
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Repo repo = new Repo();
                repo.id = c.getInt(0);
                repo.address = c.getString(1);
                repo.name = c.getString(2);
                repo.description = c.getString(3);
                repo.version = c.getInt(4);
                repo.inuse = (c.getInt(5) == 1);
                repo.priority = c.getInt(6);
                repo.pubkey = c.getString(7);
                repo.fingerprint = c.getString(8);
                repo.maxage = c.getInt(9);
                repo.lastetag = c.getString(10);
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

    public void enableRepos(List<DB.Repo> repos) {
        if (repos.isEmpty()) return;

        ContentValues values = new ContentValues(1);
        values.put("inuse", 1);

        String[] whereArgs  = new String[repos.size()];
        StringBuilder where = new StringBuilder("address IN (");
        for (int i = 0; i < repos.size(); i ++) {
            Repo repo = repos.get(i);
            repo.inuse = true;
            whereArgs[i] = repo.address;
            where.append('?');
            if ( i < repos.size() - 1 ) {
                where.append(',');
            }
        }
        where.append(")");
        db.update(TABLE_REPO, values, where.toString(), whereArgs);
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
        updateRepo(repo, "address", repo.address);
    }

    public void updateRepo(Repo repo) {
        updateRepo(repo, "id", repo.id + "");
    }

    private void updateRepo(Repo repo, String field, String value) {
        ContentValues values = new ContentValues();
        values.put("name", repo.name);
        values.put("address", repo.address);
        values.put("description", repo.description);
        values.put("version", repo.version);
        values.put("inuse", repo.inuse);
        values.put("priority", repo.priority);
        values.put("pubkey", repo.pubkey);
        if (repo.pubkey != null && repo.fingerprint == null) {
            // we got a new pubkey, so calc the fingerprint
            values.put("fingerprint", DB.calcFingerprint(repo.pubkey));
        } else {
            values.put("fingerprint", repo.fingerprint);
        }
        values.put("maxage", repo.maxage);
        values.put("lastetag", (String) null);
        db.update(TABLE_REPO, values, field + " = ?",
                new String[] { value });
    }

    /**
     * Updates the lastUpdated time for every enabled repo.
     */
    public void refreshLastUpdates() {
        ContentValues values = new ContentValues();
        values.put("lastUpdated", dateFormat.format(new Date()));
        db.update(TABLE_REPO, values, "inuse = 1",
                new String[] {});
    }

    public void writeLastEtag(Repo repo) {
        ContentValues values = new ContentValues();
        values.put("lastetag", repo.lastetag);
        values.put("lastUpdated", dateFormat.format(new Date()));
        db.update(TABLE_REPO, values, "address = ?",
                new String[] { repo.address });
    }

    public void addRepo(String address, String name, String description,
            int version, int priority, String pubkey, String fingerprint,
            int maxage, boolean inuse)
                    throws SecurityException {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("name", name);
        values.put("description", description);
        values.put("version", version);
        values.put("inuse", inuse ? 1 : 0);
        values.put("priority", priority);
        values.put("pubkey", pubkey);
        String calcedFingerprint = DB.calcFingerprint(pubkey);
        if (fingerprint == null) {
            fingerprint = calcedFingerprint;
        } else if (calcedFingerprint != null) {
            fingerprint = fingerprint.toUpperCase(Locale.ENGLISH);
            if (!fingerprint.equals(calcedFingerprint)) {
                throw new SecurityException("Given fingerprint does not match calculated one! ("
                        + fingerprint + " != " + calcedFingerprint);
            }
        }
        values.put("fingerprint", fingerprint);
        values.put("maxage", maxage);
        values.put("lastetag", (String) null);
        db.insert(TABLE_REPO, null, values);
    }

    public void doDisableRepos(List<Repo> repos, boolean remove) {
        if (repos.isEmpty()) return;
        db.beginTransaction();

        // TODO: Replace with
        //   "delete from apk join repo where repo in (?, ?, ...)
        //   "update repo set inuse = 0 | delete from repo ] where repo in (?, ?, ...)
        try {
            for (Repo repo : repos) {

                String address = repo.address;
                // Before removing the repo, remove any apks that are
                // connected to it...
                Cursor c = null;
                try {
                    c = db.query(TABLE_REPO, new String[]{"id"},
                            "address = ?", new String[]{address},
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
                else {
                    ContentValues values = new ContentValues(2);
                    values.put("inuse", 0);
                    values.put("lastetag", (String)null);
                    db.update(TABLE_REPO, values, "address = ?",
                            new String[] { address });
                }
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
