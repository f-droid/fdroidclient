package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.database.Cursor;

import org.fdroid.fdroid.Utils;

import java.util.Date;

public class Apk extends ValueObject implements Comparable<Apk> {

    public String packageName;
    public String version;
    public int vercode;
    public int size; // Size in bytes - 0 means we don't know!
    public long repo; // ID of the repo it comes from
    public String hash;
    public String hashType;
    public int minSdkVersion; // 0 if unknown
    public int maxSdkVersion; // 0 if none
    public Date added;
    public Utils.CommaSeparatedList permissions; // null if empty or
    // unknown
    public Utils.CommaSeparatedList features; // null if empty or unknown

    public Utils.CommaSeparatedList nativecode; // null if empty or unknown

    // ID (md5 sum of public key) of signature. Might be null, in the
    // transition to this field existing.
    public String sig;

    // True if compatible with the device.
    public boolean compatible;

    public String apkName; // F-Droid style APK name
    public SanitizedFile installedFile; // the .apk file on this device's filesystem

    // If not null, this is the name of the source tarball for the
    // application. Null indicates that it's a developer's binary
    // build - otherwise it's built from source.
    public String srcname;

    public int repoVersion;
    public String repoAddress;
    public Utils.CommaSeparatedList incompatibleReasons;

    public Apk() { }

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
                case ApkProvider.DataColumns.VERSION:
                    version = cursor.getString(i);
                    break;
                case ApkProvider.DataColumns.VERSION_CODE:
                    vercode = cursor.getInt(i);
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

    @Override
    public String toString() {
        return packageName + " (version " + vercode + ")";
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(ApkProvider.DataColumns.PACKAGE_NAME, packageName);
        values.put(ApkProvider.DataColumns.VERSION, version);
        values.put(ApkProvider.DataColumns.VERSION_CODE, vercode);
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
    public int compareTo(Apk apk) {
        return Integer.valueOf(vercode).compareTo(apk.vercode);
    }

}
