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
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Set;
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
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.DisplayMetrics;
import android.util.Log;

import org.fdroid.fdroid.compat.Compatibility;
import org.fdroid.fdroid.compat.ContextCompat;
import org.fdroid.fdroid.compat.SupportedArchitectures;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.data.Repo;

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
        public long repo; // ID of the repo it comes from
        public String detail_hash;
        public String detail_hashType;
        public int minSdkVersion; // 0 if unknown
        public Date added;
        public CommaSeparatedList detail_permissions; // null if empty or
                                                      // unknown
        public CommaSeparatedList features; // null if empty or unknown

        public CommaSeparatedList nativecode; // null if empty or unknown

        public CommaSeparatedList incompatible_reasons; // null if empty or
                                                        // unknown
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

            private Set<String> features;
            private Set<String> cpuAbis;
            private String cpuAbisDesc;
            private boolean ignoreTouchscreen;

            public CompatibilityChecker(Context ctx) {

                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(ctx);
                ignoreTouchscreen = prefs
                        .getBoolean(Preferences.PREF_IGN_TOUCH, false);

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
                if (!hasApi(apk.minSdkVersion)) {
                    apk.incompatible_reasons = CommaSeparatedList.make(String.valueOf(apk.minSdkVersion));
                    return false;
                }
                if (apk.features != null) {
                    for (String feat : apk.features) {
                        if (ignoreTouchscreen
                                && feat.equals("android.hardware.touchscreen")) {
                            // Don't check it!
                        } else if (!features.contains(feat)) {
                            apk.incompatible_reasons = CommaSeparatedList.make(feat);
                            Log.d("FDroid", apk.id + " vercode " + apk.vercode
                                    + " is incompatible based on lack of "
                                    + feat);
                            return false;
                        }
                    }
                }
                if (!compatibleApi(apk.nativecode)) {
                    apk.incompatible_reasons = apk.nativecode;
                    Log.d("FDroid", apk.id + " vercode " + apk.vercode
                            + " only supports " + CommaSeparatedList.str(apk.nativecode)
                            + " while your architectures are " + cpuAbisDesc);
                    return false;
                }
                return true;
            }
        }
    }

    public int countAppsForRepo(long id) {
        String[] selection     = { "COUNT(distinct id)" };
        String[] selectionArgs = { Long.toString(id) };
        Cursor result = db.query(
        TABLE_APK, selection, "repo = ?", selectionArgs, "repo", null, null);
        if (result.getCount() > 0) {
            result.moveToFirst();
            return result.getInt(0);
        } else {
            return 0;
        }
    }

    public static String calcFingerprint(String keyHexString) {
        if (TextUtils.isEmpty(keyHexString))
            return null;
        else
            return calcFingerprint(Hasher.unhex(keyHexString));
    }

    public static String calcFingerprint(Certificate cert) {
        try {
            return calcFingerprint(cert.getEncoded());
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    public static String calcFingerprint(byte[] key) {
        String ret = null;
        try {
            // keytool -list -v gives you the SHA-256 fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key);
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
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private DB(Context ctx) {

        mContext = ctx;
        DBHelper h = new DBHelper(ctx);
        db = h.getWritableDatabase();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        String sync_mode = prefs.getString(Preferences.PREF_DB_SYNC, null);
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

    private void populateApkDetails(Apk apk, long repo) {
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
    public void populateDetails(App app, long apkRepo) {
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
                        : DATE_FORMAT.parse(sAdded);
                String sLastUpdated = c.getString(11);
                app.lastUpdated = (sLastUpdated == null || sLastUpdated
                        .length() == 0) ? null : DATE_FORMAT
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

            String query = "SELECT apk.id, apk.version, apk.vercode, apk.sig,"
                    + " apk.srcname, apk.apkName, apk.minSdkVersion, "
                    + " apk.added, apk.features, apk.nativecode, "
                    + " apk.compatible, apk.repo, repo.version, repo.address "
                    + " FROM " + TABLE_APK + " as apk "
                    + " LEFT JOIN " + DBHelper.TABLE_REPO + " as repo "
                    + " ON repo._id = apk.repo "
                    + " ORDER BY apk.vercode DESC";

            c = db.rawQuery(query, null);
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
            Log.d("FDroid", "Density-specific icons dir is " + iconsDir);

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
                        : DATE_FORMAT.parse(sApkAdded);
                apk.features = CommaSeparatedList.make(c.getString(8));
                apk.nativecode = CommaSeparatedList.make(c.getString(9));
                apk.compatible = compatible;
                apk.repo = repoid;
                app.apks.add(apk);
                if (app.iconUrl == null && app.icon != null) {
                    int repoVersion = c.getInt(12);
                    String repoAddress = c.getString(13);
                    if (repoVersion >= 11) {
                        app.iconUrl = repoAddress + iconsDir + app.icon;
                    } else {
                        app.iconUrl = repoAddress + "/icons/" + app.icon;
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
                upapp.added == null ? "" : DATE_FORMAT.format(upapp.added));
        values.put(
                "lastUpdated",
                upapp.added == null ? "" : DATE_FORMAT
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
                upapk.added == null ? "" : DATE_FORMAT.format(upapk.added));
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

    public void setIgnoreUpdates(String appid, boolean All, int This) {
        db.execSQL("update " + TABLE_APP + " set"
                + " ignoreAllUpdates=" + (All ? '1' : '0')
                + ", ignoreThisUpdate="+This
                + " where id = ?", new String[] { appid });
    }

    public void purgeApps(Repo repo, FDroidApp fdroid) {
        db.beginTransaction();

        try {
            db.delete(TABLE_APK, "repo = ?", new String[] { Long.toString(repo.getId()) });
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

        fdroid.invalidateAllApps();
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
