package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.compat.Compatibility;
import org.fdroid.fdroid.compat.SupportedArchitectures;

import java.util.Date;
import java.util.Set;
import java.util.HashSet;

public class Apk {

    public String id;
    public String version;
    public int vercode;
    public int detail_size; // Size in bytes - 0 means we don't know!
    public long repo; // ID of the repo it comes from
    public String detail_hash;
    public String detail_hashType;
    public int minSdkVersion; // 0 if unknown
    public Date added;
    public DB.CommaSeparatedList detail_permissions; // null if empty or
    // unknown
    public DB.CommaSeparatedList features; // null if empty or unknown

    public DB.CommaSeparatedList nativecode; // null if empty or unknown

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

    public int repoVersion;
    public String repoAddress;
    public DB.CommaSeparatedList incompatible_reasons;

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

    public Apk(Cursor cursor) {
        for(int i = 0; i < cursor.getColumnCount(); i ++ ) {
            String column = cursor.getColumnName(i);
            if (column.equals(ApkProvider.DataColumns.HASH)) {
                detail_hash = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.HASH_TYPE)) {
                detail_hashType = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.ADDED_DATE)) {
                added = ValueObject.toDate(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.FEATURES)) {
                features = DB.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.APK_ID)) {
                id = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.IS_COMPATIBLE)) {
                compatible = cursor.getInt(i) == 1;
            } else if (column.equals(ApkProvider.DataColumns.MIN_SDK_VERSION)) {
                minSdkVersion = cursor.getInt(i);
            } else if (column.equals(ApkProvider.DataColumns.NAME)) {
                apkName = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.PERMISSIONS)) {
                detail_permissions = DB.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.NATIVE_CODE)) {
                nativecode = DB.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.REPO_ID)) {
                repo = cursor.getInt(i);
            } else if (column.equals(ApkProvider.DataColumns.SIGNATURE)) {
                sig = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.SIZE)) {
                detail_size = cursor.getInt(i);
            } else if (column.equals(ApkProvider.DataColumns.SOURCE_NAME)) {
                srcname = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.VERSION)) {
                version = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.VERSION_CODE)) {
                vercode = cursor.getInt(i);
            } else if (column.equals(ApkProvider.DataColumns.REPO_VERSION)) {
                repoVersion = cursor.getInt(i);
            } else if (column.equals(ApkProvider.DataColumns.REPO_ADDRESS)) {
                repoAddress = cursor.getString(i);
            }
        }
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(ApkProvider.DataColumns.APK_ID, id);
        values.put(ApkProvider.DataColumns.VERSION, version);
        values.put(ApkProvider.DataColumns.VERSION_CODE, vercode);
        values.put(ApkProvider.DataColumns.REPO_ID, repo);
        values.put(ApkProvider.DataColumns.HASH, detail_hash);
        values.put(ApkProvider.DataColumns.HASH_TYPE, detail_hashType);
        values.put(ApkProvider.DataColumns.SIGNATURE, sig);
        values.put(ApkProvider.DataColumns.SOURCE_NAME, srcname);
        values.put(ApkProvider.DataColumns.SIZE, detail_size);
        values.put(ApkProvider.DataColumns.NAME, apkName);
        values.put(ApkProvider.DataColumns.MIN_SDK_VERSION, minSdkVersion);
        values.put(ApkProvider.DataColumns.ADDED_DATE,
                added == null ? "" : DB.DATE_FORMAT.format(added));
        values.put(ApkProvider.DataColumns.PERMISSIONS,
                DB.CommaSeparatedList.str(detail_permissions));
        values.put(ApkProvider.DataColumns.FEATURES, DB.CommaSeparatedList.str(features));
        values.put(ApkProvider.DataColumns.NATIVE_CODE, DB.CommaSeparatedList.str(nativecode));
        values.put(ApkProvider.DataColumns.IS_COMPATIBLE, compatible ? 1 : 0);
        return values;
    }

    // Call isCompatible(apk) on an instance of this class to
    // check if an APK is compatible with the user's device.
    public static class CompatibilityChecker extends Compatibility {

        private Set<String> features;
        private Set<String> cpuAbis;
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

        private boolean compatibleApi(DB.CommaSeparatedList nativecode) {
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
                apk.incompatible_reasons = DB.CommaSeparatedList.make(String.valueOf(apk.minSdkVersion));
                return false;
            }
            if (apk.features != null) {
                for (String feat : apk.features) {
                    if (ignoreTouchscreen
                            && feat.equals("android.hardware.touchscreen")) {
                        // Don't check it!
                    } else if (!features.contains(feat)) {
                        apk.incompatible_reasons = DB.CommaSeparatedList.make(feat);
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
                        + " only supports " + DB.CommaSeparatedList.str(apk.nativecode)
                        + " while your architectures are " + cpuAbisDesc);
                return false;
            }
            return true;
        }
    }
}
