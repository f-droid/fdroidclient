package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.database.Cursor;
import org.fdroid.fdroid.Utils;

import java.util.*;

public class Apk extends ValueObject implements Comparable<Apk> {

    public String id;
    public String version;
    public int vercode;
    public int size; // Size in bytes - 0 means we don't know!
    public long repo; // ID of the repo it comes from
    public String hash;
    public String hashType;
    public int minSdkVersion; // 0 if unknown
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

    public String apkName;

    // If not null, this is the name of the source tarball for the
    // application. Null indicates that it's a developer's binary
    // build - otherwise it's built from source.
    public String srcname;

    // Used internally for tracking during repo updates.
    public boolean updated;

    public int repoVersion;
    public String repoAddress;
    public Utils.CommaSeparatedList incompatible_reasons;

    public Apk() {
        updated = false;
        size = 0;
        added = null;
        repo = 0;
        hash = null;
        hashType = null;
        permissions = null;
        compatible = false;
    }

    public Apk(Cursor cursor) {

        checkCursorPosition(cursor);

        for(int i = 0; i < cursor.getColumnCount(); i ++ ) {
            String column = cursor.getColumnName(i);
            if (column.equals(ApkProvider.DataColumns.HASH)) {
                hash = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.HASH_TYPE)) {
                hashType = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.ADDED_DATE)) {
                added = ValueObject.toDate(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.FEATURES)) {
                features = Utils.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.APK_ID)) {
                id = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.IS_COMPATIBLE)) {
                compatible = cursor.getInt(i) == 1;
            } else if (column.equals(ApkProvider.DataColumns.MIN_SDK_VERSION)) {
                minSdkVersion = cursor.getInt(i);
            } else if (column.equals(ApkProvider.DataColumns.NAME)) {
                apkName = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.PERMISSIONS)) {
                permissions = Utils.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.NATIVE_CODE)) {
                nativecode = Utils.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.INCOMPATIBLE_REASONS)) {
                incompatible_reasons = Utils.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(ApkProvider.DataColumns.REPO_ID)) {
                repo = cursor.getInt(i);
            } else if (column.equals(ApkProvider.DataColumns.SIGNATURE)) {
                sig = cursor.getString(i);
            } else if (column.equals(ApkProvider.DataColumns.SIZE)) {
                size = cursor.getInt(i);
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

    @Override
    public String toString() {
        return id + " (version " + vercode + ")";
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(ApkProvider.DataColumns.APK_ID, id);
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
        values.put(ApkProvider.DataColumns.ADDED_DATE, added == null ? "" : Utils.DATE_FORMAT.format(added));
        values.put(ApkProvider.DataColumns.PERMISSIONS, Utils.CommaSeparatedList.str(permissions));
        values.put(ApkProvider.DataColumns.FEATURES, Utils.CommaSeparatedList.str(features));
        values.put(ApkProvider.DataColumns.NATIVE_CODE, Utils.CommaSeparatedList.str(nativecode));
        values.put(ApkProvider.DataColumns.INCOMPATIBLE_REASONS, Utils.CommaSeparatedList.str(incompatible_reasons));
        values.put(ApkProvider.DataColumns.IS_COMPATIBLE, compatible ? 1 : 0);
        return values;
    }

    @Override
    public int compareTo(Apk apk) {
        return Integer.valueOf(vercode).compareTo(apk.vercode);
    }

}
