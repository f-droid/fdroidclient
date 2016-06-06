package org.fdroid.fdroid.data;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;

import org.fdroid.fdroid.Utils;

import java.util.ArrayList;
import java.util.Date;

public class Apk extends ValueObject implements Comparable<Apk> {

    // Using only byte-range keeps it only 8-bits in the SQLite database
    public static final int SDK_VERSION_MAX_VALUE = Byte.MAX_VALUE;
    public static final int SDK_VERSION_MIN_VALUE = 0;

    public String packageName;
    public String versionName;
    public int versionCode;
    public int size; // Size in bytes - 0 means we don't know!
    public long repo; // ID of the repo it comes from
    public String hash;
    public String hashType;
    public int minSdkVersion = SDK_VERSION_MIN_VALUE; // 0 if unknown
    public int maxSdkVersion = SDK_VERSION_MAX_VALUE; // "infinity" if not set
    public Date added;
    public Utils.CommaSeparatedList permissions; // null if empty or
    // unknown
    public Utils.CommaSeparatedList features; // null if empty or unknown

    public Utils.CommaSeparatedList nativecode; // null if empty or unknown

    /**
     * ID (md5 sum of public key) of signature. Might be null, in the
     * transition to this field existing.
     */
    public String sig;

    /**
     * True if compatible with the device.
     */
    public boolean compatible;

    public String apkName; // F-Droid style APK name
    public SanitizedFile installedFile; // the .apk file on this device's filesystem

    /**
     * If not null, this is the name of the source tarball for the
     * application. Null indicates that it's a developer's binary
     * build - otherwise it's built from source.
     */
    public String srcname;

    public int repoVersion;
    public String repoAddress;
    public Utils.CommaSeparatedList incompatibleReasons;

    public Apk() {
    }

    public Apk(Parcelable parcelable) {
        this(new ContentValuesCursor((ContentValues) parcelable));
    }

    public Apk(Cursor cursor) {

        checkCursorPosition(cursor);

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            switch (cursor.getColumnName(i)) {
                case ApkProvider.DataColumns.HASH:
                    hash = cursor.getString(i);
                    break;
                case ApkProvider.DataColumns.HASH_TYPE:
                    hashType = cursor.getString(i);
                    break;
                case ApkProvider.DataColumns.ADDED_DATE:
                    added = Utils.parseDate(cursor.getString(i), null);
                    break;
                case ApkProvider.DataColumns.FEATURES:
                    features = Utils.CommaSeparatedList.make(cursor.getString(i));
                    break;
                case ApkProvider.DataColumns.PACKAGE_NAME:
                    packageName = cursor.getString(i);
                    break;
                case ApkProvider.DataColumns.IS_COMPATIBLE:
                    compatible = cursor.getInt(i) == 1;
                    break;
                case ApkProvider.DataColumns.MIN_SDK_VERSION:
                    minSdkVersion = cursor.getInt(i);
                    break;
                case ApkProvider.DataColumns.MAX_SDK_VERSION:
                    maxSdkVersion = cursor.getInt(i);
                    break;
                case ApkProvider.DataColumns.NAME:
                    apkName = cursor.getString(i);
                    break;
                case ApkProvider.DataColumns.PERMISSIONS:
                    permissions = Utils.CommaSeparatedList.make(cursor.getString(i));
                    break;
                case ApkProvider.DataColumns.NATIVE_CODE:
                    nativecode = Utils.CommaSeparatedList.make(cursor.getString(i));
                    break;
                case ApkProvider.DataColumns.INCOMPATIBLE_REASONS:
                    incompatibleReasons = Utils.CommaSeparatedList.make(cursor.getString(i));
                    break;
                case ApkProvider.DataColumns.REPO_ID:
                    repo = cursor.getInt(i);
                    break;
                case ApkProvider.DataColumns.SIGNATURE:
                    sig = cursor.getString(i);
                    break;
                case ApkProvider.DataColumns.SIZE:
                    size = cursor.getInt(i);
                    break;
                case ApkProvider.DataColumns.SOURCE_NAME:
                    srcname = cursor.getString(i);
                    break;
                case ApkProvider.DataColumns.VERSION_NAME:
                    versionName = cursor.getString(i);
                    break;
                case ApkProvider.DataColumns.VERSION_CODE:
                    versionCode = cursor.getInt(i);
                    break;
                case ApkProvider.DataColumns.REPO_VERSION:
                    repoVersion = cursor.getInt(i);
                    break;
                case ApkProvider.DataColumns.REPO_ADDRESS:
                    repoAddress = cursor.getString(i);
                    break;
            }
        }
    }

    public String getUrl() {
        if (repoAddress == null || apkName == null) {
            throw new IllegalStateException("Apk needs to have both ApkProvider.DataColumns.REPO_ADDRESS and ApkProvider.DataColumns.NAME set in order to calculate URL.");
        }
        return repoAddress + "/" + apkName.replace(" ", "%20");
    }

    public ArrayList<String> getFullPermissionList() {
        if (this.permissions == null) {
            return null;
        }

        ArrayList<String> permissionsFull = new ArrayList<>();
        for (String perm : this.permissions) {
            permissionsFull.add(fdroidToAndroidPermission(perm));
        }
        return permissionsFull;
    }

    public String[] getFullPermissionsArray() {
        ArrayList<String> fullPermissions = getFullPermissionList();
        if (fullPermissions == null) {
            return null;
        }

        return fullPermissions.toArray(new String[fullPermissions.size()]);
    }

    /**
     * It appears that the default Android permissions in android.Manifest.permissions
     * are prefixed with "android.permission." and then the constant name.
     * FDroid just includes the constant name in the apk list, so we prefix it
     * with "android.permission."
     *
     * see https://gitlab.com/fdroid/fdroidserver/blob/master/fdroidserver/update.py#L535#
     */
    public static String fdroidToAndroidPermission(String permission) {
        if (!permission.contains(".")) {
            return "android.permission." + permission;
        }

        return permission;
    }

    @Override
    public String toString() {
        return packageName + " (version " + versionCode + ")";
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(ApkProvider.DataColumns.PACKAGE_NAME, packageName);
        values.put(ApkProvider.DataColumns.VERSION_NAME, versionName);
        values.put(ApkProvider.DataColumns.VERSION_CODE, versionCode);
        values.put(ApkProvider.DataColumns.REPO_ID, repo);
        values.put(ApkProvider.DataColumns.HASH, hash);
        values.put(ApkProvider.DataColumns.HASH_TYPE, hashType);
        values.put(ApkProvider.DataColumns.SIGNATURE, sig);
        values.put(ApkProvider.DataColumns.SOURCE_NAME, srcname);
        values.put(ApkProvider.DataColumns.SIZE, size);
        values.put(ApkProvider.DataColumns.NAME, apkName);
        values.put(ApkProvider.DataColumns.MIN_SDK_VERSION, minSdkVersion);
        values.put(ApkProvider.DataColumns.MAX_SDK_VERSION, maxSdkVersion);
        values.put(ApkProvider.DataColumns.ADDED_DATE, Utils.formatDate(added, ""));
        values.put(ApkProvider.DataColumns.PERMISSIONS, Utils.CommaSeparatedList.str(permissions));
        values.put(ApkProvider.DataColumns.FEATURES, Utils.CommaSeparatedList.str(features));
        values.put(ApkProvider.DataColumns.NATIVE_CODE, Utils.CommaSeparatedList.str(nativecode));
        values.put(ApkProvider.DataColumns.INCOMPATIBLE_REASONS, Utils.CommaSeparatedList.str(incompatibleReasons));
        values.put(ApkProvider.DataColumns.IS_COMPATIBLE, compatible ? 1 : 0);
        return values;
    }

    @Override
    @TargetApi(19)
    public int compareTo(Apk apk) {
        if (Build.VERSION.SDK_INT < 19) {
            return Integer.valueOf(versionCode).compareTo(apk.versionCode);
        }
        return Integer.compare(versionCode, apk.versionCode);
    }

}
