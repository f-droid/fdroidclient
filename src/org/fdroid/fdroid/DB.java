/*
a * Copyright (C) 2010  Ciaran Gultnieks, ciaran@ciarang.com
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DB {

    private static final String DATABASE_NAME = "fdroid_db";

    private SQLiteDatabase db;

    // The TABLE_VERSION table tracks the database version.
    private static final String TABLE_VERSION = "fdroid_version";
    private static final String CREATE_TABLE_VERSION = "create table "
            + TABLE_VERSION + "( version int not null); insert into "
            + TABLE_VERSION + "(version) values (1);";

    // The TABLE_APP table stores details of all the applications we know about.
    // This information is retrieved from the repositories.
    private static final String TABLE_APP = "fdroid_app";
    private static final String CREATE_TABLE_APP = "create table " + TABLE_APP
            + "( " + "id text not null, " + "name text not null, "
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
            trackerURL = "";
            sourceURL = "";
            webURL = "";
            hasUpdates = false;
            updated = false;
            apks = new Vector<Apk>();
        }

        public String id;
        public String name;
        public String summary;
        public String icon;
        public String description;
        public String license;
        public String webURL;
        public String trackerURL;
        public String sourceURL;
        public String installedVersion;

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
        public Apk getCurrentVersion() {
            // But, notwithstanding the above comment, FOR NOW, it's the
            // most recent...
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

    }

    // The TABLE_APK table stores details of all the application versions we
    // know
    // about. Each relates directly back to an entry in TABLE_APP.
    // This information is retrieved from the repositories.
    private static final String TABLE_APK = "fdroid_apk";
    private static final String CREATE_TABLE_APK = "create table " + TABLE_APK
            + "( " + "id text not null, " + "version text not null, "
            + "server text not null, " + "hash text not null, "
            + "vercode int not null," + "apkName text not null, "
            + "size int not null," + "primary key(id,version));";

    public static class Apk {

        public Apk() {
            updated = false;
            size = 0;
        }

        public String id;
        public String version;
        public int vercode;
        public int size; // Size in bytes - 0 means we don't know!
        public String server;
        public String hash;
        public String apkName;

        // Used internally for tracking during repo updates.
        public boolean updated;

        public String getURL() {
            String path = apkName.replace(" ", "%20");
            return server + "/" + path;
        }
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
    }

    private PackageManager mPm;

    public DB(Context ctx) {
        db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

        Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name= '"
                        + TABLE_VERSION + "'", null);
        boolean newinst = (c.getCount() == 0);
        c.close();
        if (newinst)
            reset();

        mPm = ctx.getPackageManager();
    }

    // Reset the database, i.e. (re-)create all the tables from scratch and
    // populate any initial data.
    public void reset() {
        db.execSQL("drop table if exists " + TABLE_VERSION);
        db.execSQL("drop table if exists " + TABLE_REPO);
        db.execSQL("drop table if exists " + TABLE_APP);
        db.execSQL("drop table if exists " + TABLE_APK);
        db.execSQL(CREATE_TABLE_VERSION);
        db.execSQL(CREATE_TABLE_REPO);
        db.execSQL(CREATE_TABLE_APP);
        db.execSQL(CREATE_TABLE_APK);
        addServer("http://f-droid.org/repo", 10);
    }

    public void close() {
        db.close();
        db = null;
    }

    // Return a list of apps matching the given criteria.
    // 'appid' - specific app id to retrieve, or null
    // 'filter' - search text to filter on.
    // 'update' - update installed version information from device, rather than
    // simply using values cached in the database. Slower.
    public Vector<App> getApps(String appid, String filter, boolean update) {
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
                app.id = c.getString(c.getColumnIndex("id"));
                app.name = c.getString(c.getColumnIndex("name"));
                app.summary = c.getString(c.getColumnIndex("summary"));
                app.icon = c.getString(c.getColumnIndex("icon"));
                app.description = c.getString(c.getColumnIndex("description"));
                app.license = c.getString(c.getColumnIndex("license"));
                app.webURL = c.getString(c.getColumnIndex("webURL"));
                app.trackerURL = c.getString(c.getColumnIndex("trackerURL"));
                app.sourceURL = c.getString(c.getColumnIndex("sourceURL"));
                app.installedVersion = c.getString(c
                        .getColumnIndex("installedVersion"));
                app.hasUpdates = false;

                c2 = db.rawQuery("select * from " + TABLE_APK + " where "
                        + "id = '" + app.id + "' order by vercode desc", null);
                c2.moveToFirst();
                while (!c2.isAfterLast()) {
                    Apk apk = new Apk();
                    apk.id = app.id;
                    apk.version = c2.getString(c2.getColumnIndex("version"));
                    apk.vercode = c2.getInt(c2.getColumnIndex("vercode"));
                    apk.server = c2.getString(c2.getColumnIndex("server"));
                    apk.hash = c2.getString(c2.getColumnIndex("hash"));
                    apk.size = c2.getInt(c2.getColumnIndex("size"));
                    apk.apkName = c2.getString(c2.getColumnIndex("apkName"));
                    app.apks.add(apk);
                    c2.moveToNext();
                }
                c2.close();

                result.add(app);
                c.moveToNext();
            }

        } catch (Exception e) {
            Log.d("FDroid", "Exception during database reading - "
                    + e.getMessage());
        } finally {
            if (c != null) {
                c.close();
            }
            if (c2 != null) {
                c2.close();
            }
        }

        if (update) {
            getUpdates(result);
        }

        // We'll say an application has updates if it's installed and the
        // installed version is not the 'current' one.
        for (App app : result) {
            if (app.installedVersion != null
                    && !app.installedVersion
                            .equals(app.getCurrentVersion().version)) {
                app.hasUpdates = true;
            }
        }

        return result;
    }

    // Verify installed status against the system's package list.
    private void getUpdates(Vector<DB.App> apps) {
        List<PackageInfo> installedPackages = mPm.getInstalledPackages(0);
        Map<String, PackageInfo> systemApks = new HashMap<String, PackageInfo>();
        for (PackageInfo appInfo : installedPackages) {
            systemApks.put(appInfo.packageName, appInfo);
        }

        for (DB.App app : apps) {
            if (systemApks.containsKey(app.id)) {
                String version = systemApks.get(app.id).versionName;
                if (app.installedVersion == null
                        || !app.installedVersion.equals(version)) {
                    setInstalledVersion(app.id, version);
                    app.installedVersion = version;
                }
            } else {
                if (app.installedVersion != null) {
                    setInstalledVersion(app.id, null);
                    app.installedVersion = null;
                }
            }
        }
    }

    private Vector<App> updateApps = null;

    // Called before a repo update starts.
    public void beginUpdate() {
        // Get a list of all apps. All the apps and apks in this list will
        // have 'updated' set to false at this point, and we will only set
        // it to true when we see the app/apk in a repository. Thus, at the
        // end, any that are still false can be removed.
        updateApps = getApps(null, null, true);
        Log.d("FDroid", "AppUpdate: " + updateApps.size()
                + " apps before starting.");
    }

    // Called when a repo update ends. Any applications that have not been
    // updated (by a call to updateApplication) are assumed to be no longer
    // in the repos.
    public void endUpdate() {
        for (App app : updateApps) {
            if (!app.updated) {
                // The application hasn't been updated, so it's no longer
                // in the repos.
                Log.d("FDroid", "AppUpdate: " + app.name
                        + " is no longer in any repository - removing");
                db.delete(TABLE_APP, "id = '" + app.id + "'", null);
                db.delete(TABLE_APK, "id = '" + app.id + "'", null);
            } else {
                for (Apk apk : app.apks) {
                    if (!apk.updated) {
                        // The package hasn't been updated, so this is a
                        // version that's no longer available.
                        Log.d("FDroid", "AppUpdate: Package " + apk.id + "/"
                                + apk.version
                                + " is no longer in any repository - removing");
                        db.delete(TABLE_APK, "id = '" + app.id
                                + "' and version ='" + apk.version + "'", null);
                    }
                }
            }
        }
        updateApps = null;
    }

    // Called during update to supply new details for an application (or
    // details of a completely new one).
    public void updateApplication(App upapp) {

        if (updateApps == null) {
            return;
        }

        boolean found = false;
        for (App app : updateApps) {
            if (app.id.equals(upapp.id)) {
                Log.d("FDroid", "AppUpdate: " + app.id
                        + " is already in the database.");
                updateAppIfDifferent(app, upapp);
                app.updated = true;
                found = true;
                for (Apk upapk : upapp.apks) {
                    boolean afound = false;
                    for (Apk apk : app.apks) {
                        if (apk.version.equals(upapk.version)) {
                            Log.d("FDroid", "AppUpdate: " + apk.version
                                    + " is a known version.");
                            updateApkIfDifferent(apk, upapk);
                            apk.updated = true;
                            afound = true;
                            break;
                        }
                    }
                    if (!afound) {
                        // A new version of this application.
                        Log.d("FDroid", "AppUpdate: " + upapk.version
                                + " is a new version.");
                        updateApkIfDifferent(null, upapk);
                        upapk.updated = true;
                        app.apks.add(upapk);
                        app.hasUpdates = true;
                    }
                }
                break;
            }
        }
        if (!found) {
            // It's a brand new application...
            Log
                    .d("FDroid", "AppUpdate: " + upapp.id
                            + " is a new application.");
            updateAppIfDifferent(null, upapp);
            for (Apk upapk : upapp.apks) {
                updateApkIfDifferent(null, upapk);
                upapk.updated = true;
            }
            upapp.updated = true;
            updateApps.add(upapp);
        }

    }

    // Update application details in the database, if different to the
    // previous ones.
    // 'oldapp' - previous details - i.e. what's in the database.
    // If null, this app is not in the database at all and
    // should be added.
    // 'upapp' - updated details
    private void updateAppIfDifferent(App oldapp, App upapp) {
        ContentValues values = new ContentValues();
        values.put("id", upapp.id);
        values.put("name", upapp.name);
        values.put("summary", upapp.summary);
        values.put("icon", upapp.icon);
        values.put("description", upapp.description);
        values.put("license", upapp.license);
        values.put("webURL", upapp.webURL);
        values.put("trackerURL", upapp.trackerURL);
        values.put("sourceURL", upapp.sourceURL);
        values.put("installedVersion", upapp.installedVersion);
        values.put("hasUpdates", upapp.hasUpdates ? 1 : 0);
        if (oldapp != null) {
            db.update(TABLE_APP, values, "id = '" + oldapp.id + "'", null);
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
        values.put("size", upapk.size);
        values.put("apkName", upapk.apkName);
        if (oldapk != null) {
            db.update(TABLE_APK, values, "id = '" + oldapk.id
                    + "' and version = '" + oldapk.version + "'", null);
        } else {
            db.insert(TABLE_APK, null, values);
        }
    }

    public void setInstalledVersion(String id, String version) {
        ContentValues values = new ContentValues();
        values.put("installedVersion", version);
        db.update(TABLE_APP, values, "id = '" + id + "'", null);
    }

    // Get a list of the configured repositories.
    public Vector<Repo> getRepos() {
        Vector<Repo> repos = new Vector<Repo>();
        Cursor c = null;
        try {
            c = db.rawQuery("select address, inuse, priority from "
                    + TABLE_REPO + " order by priority", null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                Repo repo = new Repo();
                repo.address = c.getString(0);
                repo.inuse = (c.getInt(1) == 1);
                repo.priority = c.getInt(2);
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
        db.rawQuery("update " + TABLE_REPO
                + " set inuse=1-inuse where address='" + address + "'", null);
    }

    public void addServer(String address, int priority) {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("inuse", 1);
        values.put("priority", priority);
        db.insert(TABLE_REPO, null, values);
    }

    public void removeServers(Vector<String> addresses) {
        for (String address : addresses) {
            db.delete(TABLE_REPO, "address = '" + address + "'", null);
        }
    }

}
