/*
 * Copyright (C) 2016 Blue Jay Wireless
 * Copyright (C) 2015-2016 Daniel Martí <mvdan@mvdan.cc>
 * Copyright (C) 2015 Christian Morgner
 * Copyright (C) 2014-2016 Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2013-2016 Peter Serwylo <peter@serwylo.com>
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.fdroid.fdroid.data.Schema.CatJoinTable;
import org.fdroid.fdroid.data.Schema.PackageTable;
import org.fdroid.fdroid.data.Schema.AppPrefsTable;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.fdroid.fdroid.data.Schema.RepoTable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("LineLength")
class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";

    public static final int REPO_XML_ARG_COUNT = 8;

    private static final String DATABASE_NAME = "fdroid";

    private static final String CREATE_TABLE_PACKAGE = "CREATE TABLE " + PackageTable.NAME
            + " ( "
            + PackageTable.Cols.PACKAGE_NAME + " text not null, "
            + PackageTable.Cols.PREFERRED_METADATA + " integer"
            + ");";

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
            + RepoTable.Cols.TIMESTAMP + " integer not null default 0, "
            + RepoTable.Cols.ICON + " string, "
            + RepoTable.Cols.MIRRORS + " string, "
            + RepoTable.Cols.PUSH_REQUESTS + " integer not null default " + Repo.PUSH_REQUEST_IGNORE
            + ");";

    static final String CREATE_TABLE_APK =
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
            + ApkTable.Cols.OBB_MAIN_FILE + " string, "
            + ApkTable.Cols.OBB_MAIN_FILE_SHA256 + " string, "
            + ApkTable.Cols.OBB_PATCH_FILE + " string, "
            + ApkTable.Cols.OBB_PATCH_FILE_SHA256 + " string, "
            + ApkTable.Cols.REQUESTED_PERMISSIONS + " string, "
            + ApkTable.Cols.FEATURES + " string, "
            + ApkTable.Cols.NATIVE_CODE + " string, "
            + ApkTable.Cols.HASH_TYPE + " string, "
            + ApkTable.Cols.ADDED_DATE + " string, "
            + ApkTable.Cols.IS_COMPATIBLE + " int not null, "
            + ApkTable.Cols.INCOMPATIBLE_REASONS + " text, "
            + ApkTable.Cols.ANTI_FEATURES + " string"
            + ");";

    static final String CREATE_TABLE_APP_METADATA = "CREATE TABLE " + AppMetadataTable.NAME
            + " ( "
            + AppMetadataTable.Cols.PACKAGE_ID + " integer not null, "
            + AppMetadataTable.Cols.REPO_ID + " integer not null, "
            + AppMetadataTable.Cols.NAME + " text not null, "
            + AppMetadataTable.Cols.SUMMARY + " text not null, "
            + AppMetadataTable.Cols.ICON + " text, "
            + AppMetadataTable.Cols.DESCRIPTION + " text not null, "
            + AppMetadataTable.Cols.WHATSNEW + " text, "
            + AppMetadataTable.Cols.LICENSE + " text not null, "
            + AppMetadataTable.Cols.AUTHOR_NAME + " text, "
            + AppMetadataTable.Cols.AUTHOR_EMAIL + " text, "
            + AppMetadataTable.Cols.WEBSITE + " text, "
            + AppMetadataTable.Cols.ISSUE_TRACKER + " text, "
            + AppMetadataTable.Cols.SOURCE_CODE + " text, "
            + AppMetadataTable.Cols.VIDEO + " string, "
            + AppMetadataTable.Cols.CHANGELOG + " text, "
            + AppMetadataTable.Cols.PREFERRED_SIGNER + " text,"
            + AppMetadataTable.Cols.SUGGESTED_VERSION_CODE + " text,"
            + AppMetadataTable.Cols.UPSTREAM_VERSION_NAME + " text,"
            + AppMetadataTable.Cols.UPSTREAM_VERSION_CODE + " integer,"
            + AppMetadataTable.Cols.ANTI_FEATURES + " string,"
            + AppMetadataTable.Cols.DONATE + " string,"
            + AppMetadataTable.Cols.BITCOIN + " string,"
            + AppMetadataTable.Cols.LITECOIN + " string,"
            + AppMetadataTable.Cols.FLATTR_ID + " string,"
            + AppMetadataTable.Cols.REQUIREMENTS + " string,"
            + AppMetadataTable.Cols.ADDED + " string,"
            + AppMetadataTable.Cols.LAST_UPDATED + " string,"
            + AppMetadataTable.Cols.IS_COMPATIBLE + " int not null,"
            + AppMetadataTable.Cols.ICON_URL + " text, "
            + AppMetadataTable.Cols.ICON_URL_LARGE + " text, "
            + AppMetadataTable.Cols.FEATURE_GRAPHIC + " string,"
            + AppMetadataTable.Cols.PROMO_GRAPHIC + " string,"
            + AppMetadataTable.Cols.TV_BANNER + " string,"
            + AppMetadataTable.Cols.PHONE_SCREENSHOTS + " string,"
            + AppMetadataTable.Cols.SEVEN_INCH_SCREENSHOTS + " string,"
            + AppMetadataTable.Cols.TEN_INCH_SCREENSHOTS + " string,"
            + AppMetadataTable.Cols.TV_SCREENSHOTS + " string,"
            + AppMetadataTable.Cols.WEAR_SCREENSHOTS + " string,"
            + AppMetadataTable.Cols.IS_APK + " boolean,"
            + "primary key(" + AppMetadataTable.Cols.PACKAGE_ID + ", " + AppMetadataTable.Cols.REPO_ID + "));";

    private static final String CREATE_TABLE_APP_PREFS = "CREATE TABLE " + AppPrefsTable.NAME
            + " ( "
            + AppPrefsTable.Cols.PACKAGE_NAME + " TEXT, "
            + AppPrefsTable.Cols.IGNORE_THIS_UPDATE + " INT BOOLEAN NOT NULL, "
            + AppPrefsTable.Cols.IGNORE_ALL_UPDATES + " INT NOT NULL "
            + " );";

    private static final String CREATE_TABLE_CATEGORY = "CREATE TABLE " + Schema.CategoryTable.NAME
            + " ( "
            + Schema.CategoryTable.Cols.NAME + " TEXT NOT NULL "
            + " );";

    /**
     * The order of the two columns in the primary key matters for this table. The index that is
     * built for sqlite to quickly search the primary key will be sorted by app metadata id first,
     * and category id second. This means that we don't need a separate individual index on the
     * app metadata id, because it can instead look through the primary key index. This can be
     * observed by flipping the order of the primary key columns, and noting the resulting sqlite
     * logs along the lines of:
     *   E/SQLiteLog(14164): (284) automatic index on fdroid_categoryAppMetadataJoin(appMetadataId)
     */
    static final String CREATE_TABLE_CAT_JOIN = "CREATE TABLE " + CatJoinTable.NAME
            + " ( "
            + CatJoinTable.Cols.APP_METADATA_ID + " INT NOT NULL, "
            + CatJoinTable.Cols.CATEGORY_ID + " INT NOT NULL, "
            + "primary key(" + CatJoinTable.Cols.APP_METADATA_ID + ", " + CatJoinTable.Cols.CATEGORY_ID + ") "
            + " );";

    private static final String CREATE_TABLE_INSTALLED_APP = "CREATE TABLE " + InstalledAppTable.NAME
            + " ( "
            + InstalledAppTable.Cols.PACKAGE_ID + " INT NOT NULL UNIQUE, "
            + InstalledAppTable.Cols.VERSION_CODE + " INT NOT NULL, "
            + InstalledAppTable.Cols.VERSION_NAME + " TEXT NOT NULL, "
            + InstalledAppTable.Cols.APPLICATION_LABEL + " TEXT NOT NULL, "
            + InstalledAppTable.Cols.SIGNATURE + " TEXT NOT NULL, "
            + InstalledAppTable.Cols.LAST_UPDATE_TIME + " INTEGER NOT NULL DEFAULT 0, "
            + InstalledAppTable.Cols.HASH_TYPE + " TEXT NOT NULL, "
            + InstalledAppTable.Cols.HASH + " TEXT NOT NULL"
            + " );";

    protected static final int DB_VERSION = 74;

    private final Context context;

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE_PACKAGE);
        db.execSQL(CREATE_TABLE_APP_METADATA);
        db.execSQL(CREATE_TABLE_APK);
        db.execSQL(CREATE_TABLE_CATEGORY);
        db.execSQL(CREATE_TABLE_CAT_JOIN);
        db.execSQL(CREATE_TABLE_INSTALLED_APP);
        db.execSQL(CREATE_TABLE_REPO);
        db.execSQL(CREATE_TABLE_APP_PREFS);
        ensureIndexes(db);

        String[] defaultRepos = context.getResources().getStringArray(R.array.default_repos);
        if (defaultRepos.length % REPO_XML_ARG_COUNT != 0) {
            throw new IllegalArgumentException(
                    "default_repo.xml array does not have the right number of elements");
        }
        for (int i = 0; i < defaultRepos.length / REPO_XML_ARG_COUNT; i++) {
            int offset = i * REPO_XML_ARG_COUNT;
            insertRepo(
                    db,
                    defaultRepos[offset],     // name
                    defaultRepos[offset + 1], // address
                    defaultRepos[offset + 2], // description
                    defaultRepos[offset + 3], // version
                    defaultRepos[offset + 4], // enabled
                    defaultRepos[offset + 5], // priority
                    defaultRepos[offset + 6], // pushRequests
                    defaultRepos[offset + 7]  // pubkey
            );
        }
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
        supportRepoPushRequests(db, oldVersion);
        migrateToPackageTable(db, oldVersion);
        addObbFiles(db, oldVersion);
        addCategoryTables(db, oldVersion);
        addIndexV1Fields(db, oldVersion);
        addIndexV1AppFields(db, oldVersion);
        recalculatePreferredMetadata(db, oldVersion);
        addWhatsNewAndVideo(db, oldVersion);
        dropApkPrimaryKey(db, oldVersion);
        addIntegerPrimaryKeyToInstalledApps(db, oldVersion);
        addPreferredSignerToApp(db, oldVersion);
        updatePreferredSignerIfEmpty(db, oldVersion);
        addIsAppToApp(db, oldVersion);
    }

    private void addIsAppToApp(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 74) {
            return;
        }

        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.IS_APK)) {
            Log.i(TAG, "Figuring out whether each \"app\" is actually an app, or it represents other media.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.IS_APK + " boolean;");

            // Find all apks for which their filename DOESN'T end in ".apk", and if there is more than one, the
            // corresponding app is updated to be marked as media.
            String apkName = ApkTable.Cols.NAME;
            String query = "UPDATE " + AppMetadataTable.NAME + " SET " + AppMetadataTable.Cols.IS_APK + " = (" +
                    "  SELECT COUNT(*) FROM " + ApkTable.NAME + " AS apk" +
                    "  WHERE " +
                    "    " + ApkTable.Cols.APP_ID + " = " + AppMetadataTable.NAME + "." + AppMetadataTable.Cols.ROW_ID +
                    "    AND SUBSTR(" + apkName + ", LENGTH(" + apkName + ") - 3) != '.apk'" +
                    ") = 0;";
            Log.i(TAG, query);
            db.execSQL(query);
        }
    }

    private void updatePreferredSignerIfEmpty(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 73) {
            return;
        }

        Log.i(TAG, "Forcing repo refresh to calculate preferred signer.");
        resetTransient(db);
    }

    private void addPreferredSignerToApp(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 72) {
            return;
        }

        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.PREFERRED_SIGNER)) {
            Log.i(TAG, "Adding preferred signer to app table.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.PREFERRED_SIGNER + " text;");
        }
    }

    private void addIntegerPrimaryKeyToInstalledApps(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 71) {
            return;
        }

        Log.i(TAG, "Replacing primary key on installed app table with integer for performance.");

        db.beginTransaction();
        try {
            if (tableExists(db, Schema.InstalledAppTable.NAME)) {
                db.execSQL("DROP TABLE " + Schema.InstalledAppTable.NAME);
            }

            db.execSQL(CREATE_TABLE_INSTALLED_APP);
            ensureIndexes(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void dropApkPrimaryKey(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 70) {
            return;
        }

        // versionCode + repo is no longer a valid primary key given a repo can have multiple apks
        // with the same versionCode, signed by different certificates.
        Log.i(TAG, "Dropping composite primary key on apk table in favour of sqlite's rowid");
        resetTransient(db);
    }

    private void addWhatsNewAndVideo(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 69) {
            return;
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.WHATSNEW)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.WHATSNEW + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.WHATSNEW + " text;");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.VIDEO)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.VIDEO + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.VIDEO + " string;");
        }
    }

    private void recalculatePreferredMetadata(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 68) {
            return;
        }

        Log.i(TAG, "Previously, the repository metadata was being interpreted backwards. Need to force a repo refresh to fix this.");
        resetTransient(db);
    }

    private void addIndexV1AppFields(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 67) {
            return;
        }
        // Strings
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.FEATURE_GRAPHIC)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.FEATURE_GRAPHIC + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.FEATURE_GRAPHIC + " string;");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.PROMO_GRAPHIC)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.PROMO_GRAPHIC + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.PROMO_GRAPHIC + " string;");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.TV_BANNER)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.TV_BANNER + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.TV_BANNER + " string;");
        }
        // String Arrays
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.PHONE_SCREENSHOTS)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.PHONE_SCREENSHOTS + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.PHONE_SCREENSHOTS + " string;");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.SEVEN_INCH_SCREENSHOTS)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.SEVEN_INCH_SCREENSHOTS + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.SEVEN_INCH_SCREENSHOTS + " string;");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.TEN_INCH_SCREENSHOTS)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.TEN_INCH_SCREENSHOTS + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.TEN_INCH_SCREENSHOTS + " string;");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.TV_SCREENSHOTS)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.TV_SCREENSHOTS + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.TV_SCREENSHOTS + " string;");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.WEAR_SCREENSHOTS)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.WEAR_SCREENSHOTS + " field to " + AppMetadataTable.NAME + " table in db.");
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.WEAR_SCREENSHOTS + " string;");
        }
    }

    private void addIndexV1Fields(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 66) {
            return;
        }
        if (!columnExists(db, Schema.RepoTable.NAME, RepoTable.Cols.ICON)) {
            Utils.debugLog(TAG, "Adding " + RepoTable.Cols.ICON + " field to " + RepoTable.NAME + " table in db.");
            db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.ICON + " string;");
        }

        if (!columnExists(db, RepoTable.NAME, RepoTable.Cols.MIRRORS)) {
            Utils.debugLog(TAG, "Adding " + RepoTable.Cols.MIRRORS + " field to " + RepoTable.NAME + " table in db.");
            db.execSQL("alter table " + RepoTable.NAME + " add column " + RepoTable.Cols.MIRRORS + " string;");
        }

        if (!columnExists(db, ApkTable.NAME, ApkTable.Cols.ANTI_FEATURES)) {
            Utils.debugLog(TAG, "Adding " + ApkTable.Cols.ANTI_FEATURES + " field to " + ApkTable.NAME + " table in db.");
            db.execSQL("alter table " + ApkTable.NAME + " add column " + ApkTable.Cols.ANTI_FEATURES + " string;");
        }
    }

    /**
     * It is possible to correctly migrate categories from the previous `categories` column in
     * app metadata to the new join table without destroying any data and requiring a repo update.
     * However, in practice other code since the previous stable has already reset the transient
     * tables and forced a repo update, so it is much easier to do the same here. It wont have any
     * negative impact on those upgrading from the previous stable. If there was a number of solid
     * alpha releases before this, then a proper migration would've be in order.
     */
    private void addCategoryTables(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 65) {
            return;
        }

        resetTransient(db);
    }

    private void addObbFiles(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 64) {
            return;
        }

        Utils.debugLog(TAG, "Ensuring " + ApkTable.Cols.OBB_MAIN_FILE + ", " +
                ApkTable.Cols.OBB_PATCH_FILE + ", and hash columns exist on " + ApkTable.NAME);

        if (!columnExists(db, ApkTable.NAME, ApkTable.Cols.OBB_MAIN_FILE)) {
            db.execSQL("alter table " + ApkTable.NAME + " add column "
                    + ApkTable.Cols.OBB_MAIN_FILE + " string");
        }

        if (!columnExists(db, ApkTable.NAME, ApkTable.Cols.OBB_MAIN_FILE_SHA256)) {
            db.execSQL("alter table " + ApkTable.NAME + " add column "
                    + ApkTable.Cols.OBB_MAIN_FILE_SHA256 + " string");
        }

        if (!columnExists(db, ApkTable.NAME, ApkTable.Cols.OBB_PATCH_FILE)) {
            db.execSQL("alter table " + ApkTable.NAME + " add column "
                    + ApkTable.Cols.OBB_PATCH_FILE + " string");
        }

        if (!columnExists(db, ApkTable.NAME, ApkTable.Cols.OBB_PATCH_FILE_SHA256)) {
            db.execSQL("alter table " + ApkTable.NAME + " add column "
                    + ApkTable.Cols.OBB_PATCH_FILE_SHA256 + " string");
        }
    }

    private void migrateToPackageTable(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 63) {
            return;
        }

        resetTransient(db);

        // By pushing _ALL_ repositories to a priority of 10, it makes it slightly easier
        // to query for the non-default repositories later on in this method.
        ContentValues highPriority = new ContentValues(1);
        highPriority.put(RepoTable.Cols.PRIORITY, 10);
        db.update(RepoTable.NAME, highPriority, null, null);

        String[] defaultRepos = context.getResources().getStringArray(R.array.default_repos);
        String fdroidPubKey = defaultRepos[7];
        String fdroidAddress = defaultRepos[1];
        String fdroidArchiveAddress = defaultRepos[REPO_XML_ARG_COUNT + 1];
        String gpPubKey = defaultRepos[REPO_XML_ARG_COUNT * 2 + 7];
        String gpAddress = defaultRepos[REPO_XML_ARG_COUNT * 2 + 1];
        String gpArchiveAddress = defaultRepos[REPO_XML_ARG_COUNT * 3 + 1];

        updateRepoPriority(db, fdroidPubKey, fdroidAddress, 1);
        updateRepoPriority(db, fdroidPubKey, fdroidArchiveAddress, 2);
        updateRepoPriority(db, gpPubKey, gpAddress, 3);
        updateRepoPriority(db, gpPubKey, gpArchiveAddress, 4);

        int priority = 5;
        String[] projection = new String[] {RepoTable.Cols.SIGNING_CERT, RepoTable.Cols.ADDRESS};

        // Order by ID, because that is a good analogy for the order in which they were added.
        // The order in which they were added is likely the order they present in the ManageRepos activity.
        Cursor cursor = db.query(RepoTable.NAME, projection, RepoTable.Cols.PRIORITY + " > 4", null, null, null, RepoTable.Cols._ID);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String signingCert = cursor.getString(cursor.getColumnIndex(RepoTable.Cols.SIGNING_CERT));
            String address = cursor.getString(cursor.getColumnIndex(RepoTable.Cols.ADDRESS));
            updateRepoPriority(db, signingCert, address, priority);
            cursor.moveToNext();
            priority++;
        }
        cursor.close();
    }

    private void updateRepoPriority(SQLiteDatabase db, String signingCert, String address, int priority) {
        ContentValues values = new ContentValues(1);
        values.put(RepoTable.Cols.PRIORITY, Integer.toString(priority));

        Utils.debugLog(TAG, "Setting priority of repo " + address + " to " + priority);
        db.update(
                RepoTable.NAME,
                values,
                RepoTable.Cols.SIGNING_CERT + " = ? AND " + RepoTable.Cols.ADDRESS + " = ?",
                new String[] {signingCert, address}
        );
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
                + "id, "
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
                        + ApkTable.Cols.REQUESTED_PERMISSIONS + " string, "
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
                        ApkTable.Cols.REQUESTED_PERMISSIONS,
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
                        "WHERE " + ApkTable.NAME + ".id = app.id)";
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
                                          String name, String address, String description) {
        ContentValues values = new ContentValues();
        values.clear();
        values.put(RepoTable.Cols.NAME, name);
        values.put(RepoTable.Cols.DESCRIPTION, description);
        db.update(RepoTable.NAME, values, RepoTable.Cols.ADDRESS + " = ?", new String[]{
                address,
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

        String[] defaultRepos = context.getResources().getStringArray(R.array.default_repos);
        for (int i = 0; i < defaultRepos.length / REPO_XML_ARG_COUNT; i++) {
            int offset = i * REPO_XML_ARG_COUNT;
            insertNameAndDescription(db,
                    defaultRepos[offset],     // name
                    defaultRepos[offset + 1], // address
                    defaultRepos[offset + 2] // description
            );
        }
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
        if (oldVersion >= 48 || columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.CHANGELOG)) {
            return;
        }
        Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.CHANGELOG + " column to " + AppMetadataTable.NAME);
        db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.CHANGELOG + " text");
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

        String query = "UPDATE fdroid_app "
                + "SET iconUrl = ("
                + "  SELECT (fdroid_repo.address || CASE WHEN fdroid_repo.version >= ? THEN ? ELSE ? END || fdroid_app.icon) "
                + "  FROM fdroid_apk "
                + "  JOIN fdroid_repo ON (fdroid_repo._id = fdroid_apk.repo) "
                + "  WHERE fdroid_app.id = fdroid_apk.id AND fdroid_apk.vercode = fdroid_app.suggestedVercode "
                + "), iconUrlLarge = ("
                + "  SELECT (fdroid_repo.address || CASE WHEN fdroid_repo.version >= ? THEN ? ELSE ? END || fdroid_app.icon) "
                + "  FROM fdroid_apk "
                + "  JOIN fdroid_repo ON (fdroid_repo._id = fdroid_apk.repo) "
                + "  WHERE fdroid_app.id = fdroid_apk.id AND fdroid_apk.vercode = fdroid_app.suggestedVercode"
                + ")";

        String iconsDir = Utils.getIconsDir(context, 1.0);
        String iconsDirLarge = Utils.getIconsDir(context, 1.5);
        String repoVersion = Integer.toString(Repo.VERSION_DENSITY_SPECIFIC_ICONS);
        Utils.debugLog(TAG, "Using icons dir '" + iconsDir + "'");
        Utils.debugLog(TAG, "Using large icons dir '" + iconsDirLarge + "'");
        String[] args = {
                repoVersion, iconsDir, Utils.FALLBACK_ICONS_DIR,
                repoVersion, iconsDirLarge, Utils.FALLBACK_ICONS_DIR,
        };

        db.rawQuery(query, args);

        clearRepoEtags(db);
    }

    private void addAuthorToApp(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 53) {
            return;
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.AUTHOR_NAME)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.AUTHOR_NAME + " column to " + AppMetadataTable.NAME);
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.AUTHOR_NAME + " text");
        }
        if (!columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.AUTHOR_EMAIL)) {
            Utils.debugLog(TAG, "Adding " + AppMetadataTable.Cols.AUTHOR_EMAIL + " column to " + AppMetadataTable.NAME);
            db.execSQL("alter table " + AppMetadataTable.NAME + " add column " + AppMetadataTable.Cols.AUTHOR_EMAIL + " text");
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

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("triedEmptyUpdate", false)
                .apply();

        db.beginTransaction();
        try {
            if (tableExists(db, Schema.CategoryTable.NAME)) {
                db.execSQL("DROP TABLE " + Schema.CategoryTable.NAME);
            }

            if (tableExists(db, CatJoinTable.NAME)) {
                db.execSQL("DROP TABLE " + CatJoinTable.NAME);
            }

            if (tableExists(db, PackageTable.NAME)) {
                db.execSQL("DROP TABLE " + PackageTable.NAME);
            }

            db.execSQL("DROP TABLE " + AppMetadataTable.NAME);
            db.execSQL("DROP TABLE " + ApkTable.NAME);

            db.execSQL(CREATE_TABLE_PACKAGE);
            db.execSQL(CREATE_TABLE_APP_METADATA);
            db.execSQL(CREATE_TABLE_APK);
            db.execSQL(CREATE_TABLE_CATEGORY);
            db.execSQL(CREATE_TABLE_CAT_JOIN);
            clearRepoEtags(db);
            ensureIndexes(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("triedEmptyUpdate", false)
                .apply();

        db.execSQL("drop table " + AppMetadataTable.NAME);
        db.execSQL("drop table " + ApkTable.NAME);
        clearRepoEtags(db);
        db.execSQL(CREATE_TABLE_APP_METADATA);
        db.execSQL(CREATE_TABLE_APK);
        ensureIndexes(db);
    }

    private static void ensureIndexes(SQLiteDatabase db) {
        if (tableExists(db, PackageTable.NAME)) {
            Utils.debugLog(TAG, "Ensuring indexes exist for " + PackageTable.NAME);
            db.execSQL("CREATE INDEX IF NOT EXISTS package_packageName on " + PackageTable.NAME + " (" + PackageTable.Cols.PACKAGE_NAME + ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS package_preferredMetadata on " + PackageTable.NAME + " (" + PackageTable.Cols.PREFERRED_METADATA + ");");
        }

        Utils.debugLog(TAG, "Ensuring indexes exist for " + AppMetadataTable.NAME);
        db.execSQL("CREATE INDEX IF NOT EXISTS name on " + AppMetadataTable.NAME + " (" + AppMetadataTable.Cols.NAME + ");"); // Used for sorting most lists
        db.execSQL("CREATE INDEX IF NOT EXISTS added on " + AppMetadataTable.NAME + " (" + AppMetadataTable.Cols.ADDED + ");"); // Used for sorting "newly added"

        if (columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.PACKAGE_ID)) {
            db.execSQL("CREATE INDEX IF NOT EXISTS metadata_packageId ON " + AppMetadataTable.NAME + " (" + AppMetadataTable.Cols.PACKAGE_ID + ");");
        }

        if (columnExists(db, AppMetadataTable.NAME, AppMetadataTable.Cols.REPO_ID)) {
            db.execSQL("CREATE INDEX IF NOT EXISTS metadata_repoId ON " + AppMetadataTable.NAME + " (" + AppMetadataTable.Cols.REPO_ID + ");");
        }

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

        if (columnExists(db, InstalledAppTable.NAME, InstalledAppTable.Cols.PACKAGE_ID)) {
            Utils.debugLog(TAG, "Ensuring indexes exist for " + InstalledAppTable.NAME);
            db.execSQL("CREATE INDEX IF NOT EXISTS installedApp_packageId_vercode on " + InstalledAppTable.NAME + " (" +
                    InstalledAppTable.Cols.PACKAGE_ID + ", " + InstalledAppTable.Cols.VERSION_CODE + ");");
        }

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
        if (tableExists(db, "fdroid_installedApp")) {
            db.execSQL("DROP TABLE fdroid_installedApp;");
        }

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

    private void supportRepoPushRequests(SQLiteDatabase db, int oldVersion) {
        if (oldVersion >= 62) {
            return;
        }
        Utils.debugLog(TAG, "Adding " + RepoTable.Cols.PUSH_REQUESTS
                + " columns to " + RepoTable.NAME);
        db.execSQL("alter table " + RepoTable.NAME + " add column "
                + RepoTable.Cols.PUSH_REQUESTS + " integer not null default "
                + Repo.PUSH_REQUEST_IGNORE);
    }

    private static boolean columnExists(SQLiteDatabase db, String table, String field) {
        boolean found = false;
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(cursor.getColumnIndex("name"));
            if (name.equalsIgnoreCase(field)) {
                found = true;
                break;
            }
            cursor.moveToNext();
        }
        cursor.close();
        return found;
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        Cursor cursor = db.query("sqlite_master", new String[] {"name"},
                "type = 'table' AND name = ?", new String[] {table}, null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    private void insertRepo(SQLiteDatabase db, String name, String address,
                            String description, String version, String enabled,
                            String priority, String pushRequests, String pubKey) {
        ContentValues values = new ContentValues();
        values.put(RepoTable.Cols.ADDRESS, address);
        values.put(RepoTable.Cols.NAME, name);
        values.put(RepoTable.Cols.DESCRIPTION, description);
        values.put(RepoTable.Cols.SIGNING_CERT, pubKey);
        values.put(RepoTable.Cols.FINGERPRINT, Utils.calcFingerprint(pubKey));
        values.put(RepoTable.Cols.MAX_AGE, 0);
        values.put(RepoTable.Cols.VERSION, Utils.parseInt(version, 0));
        values.put(RepoTable.Cols.IN_USE, Utils.parseInt(enabled, 0));
        values.put(RepoTable.Cols.PRIORITY, Utils.parseInt(priority, Integer.MAX_VALUE));
        values.put(RepoTable.Cols.LAST_ETAG, (String) null);
        values.put(RepoTable.Cols.TIMESTAMP, 0);

        switch (pushRequests) {
            case "ignore":
                values.put(RepoTable.Cols.PUSH_REQUESTS, Repo.PUSH_REQUEST_IGNORE);
                break;
            case "prompt":
                values.put(RepoTable.Cols.PUSH_REQUESTS, Repo.PUSH_REQUEST_PROMPT);
                break;
            case "always":
                values.put(RepoTable.Cols.PUSH_REQUESTS, Repo.PUSH_REQUEST_ACCEPT_ALWAYS);
                break;
            default:
                throw new IllegalArgumentException(pushRequests + " is not a supported option!");
        }

        Utils.debugLog(TAG, "Adding repository " + name + " with push requests as " + pushRequests);
        db.insert(RepoTable.NAME, null, values);
    }

}
