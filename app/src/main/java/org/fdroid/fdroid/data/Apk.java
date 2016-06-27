package org.fdroid.fdroid.data;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.ApkTable.Cols;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class Apk extends ValueObject implements Comparable<Apk>, Parcelable {

    // Using only byte-range keeps it only 8-bits in the SQLite database
    public static final int SDK_VERSION_MAX_VALUE = Byte.MAX_VALUE;
    public static final int SDK_VERSION_MIN_VALUE = 0;

    public String packageName;
    public String versionName;
    public int versionCode;
    public int size; // Size in bytes - 0 means we don't know!
    public long repo; // ID of the repo it comes from
    public String hash; // checksum of the APK, in lowercase hex
    public String hashType;
    public int minSdkVersion = SDK_VERSION_MIN_VALUE; // 0 if unknown
    public int targetSdkVersion = SDK_VERSION_MIN_VALUE; // 0 if unknown
    public int maxSdkVersion = SDK_VERSION_MAX_VALUE; // "infinity" if not set
    public Date added;
    public String[] permissions; // null if empty or
    // unknown
    public String[] features; // null if empty or unknown

    public String[] nativecode; // null if empty or unknown

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
    public String[] incompatibleReasons;

    /**
     * The numeric primary key of the App table, which is used to join apks.
     */
    public long appId;

    public Apk() {
    }

    public Apk(Cursor cursor) {

        checkCursorPosition(cursor);

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            switch (cursor.getColumnName(i)) {
                case Cols.APP_ID:
                    appId = cursor.getLong(i);
                    break;
                case Cols.HASH:
                    hash = cursor.getString(i);
                    break;
                case Cols.HASH_TYPE:
                    hashType = cursor.getString(i);
                    break;
                case Cols.ADDED_DATE:
                    added = Utils.parseDate(cursor.getString(i), null);
                    break;
                case Cols.FEATURES:
                    features = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.App.PACKAGE_NAME:
                    packageName = cursor.getString(i);
                    break;
                case Cols.IS_COMPATIBLE:
                    compatible = cursor.getInt(i) == 1;
                    break;
                case Cols.MIN_SDK_VERSION:
                    minSdkVersion = cursor.getInt(i);
                    break;
                case Cols.TARGET_SDK_VERSION:
                    targetSdkVersion = cursor.getInt(i);
                    break;
                case Cols.MAX_SDK_VERSION:
                    maxSdkVersion = cursor.getInt(i);
                    break;
                case Cols.NAME:
                    apkName = cursor.getString(i);
                    break;
                case Cols.PERMISSIONS:
                    permissions = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.NATIVE_CODE:
                    nativecode = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.INCOMPATIBLE_REASONS:
                    incompatibleReasons = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.REPO_ID:
                    repo = cursor.getInt(i);
                    break;
                case Cols.SIGNATURE:
                    sig = cursor.getString(i);
                    break;
                case Cols.SIZE:
                    size = cursor.getInt(i);
                    break;
                case Cols.SOURCE_NAME:
                    srcname = cursor.getString(i);
                    break;
                case Cols.VERSION_NAME:
                    versionName = cursor.getString(i);
                    break;
                case Cols.VERSION_CODE:
                    versionCode = cursor.getInt(i);
                    break;
                case Cols.Repo.VERSION:
                    repoVersion = cursor.getInt(i);
                    break;
                case Cols.Repo.ADDRESS:
                    repoAddress = cursor.getString(i);
                    break;
            }
        }
    }

    public String getUrl() {
        if (repoAddress == null || apkName == null) {
            throw new IllegalStateException("Apk needs to have both Schema.ApkTable.Cols.REPO_ADDRESS and Schema.ApkTable.Cols.NAME set in order to calculate URL.");
        }
        return repoAddress + "/" + apkName.replace(" ", "%20");
    }

    public ArrayList<String> getFullPermissionList() {
        if (this.permissions == null) {
            return new ArrayList<>();
        }

        ArrayList<String> permissionsFull = new ArrayList<>();
        for (String perm : this.permissions) {
            permissionsFull.add(fdroidToAndroidPermission(perm));
        }
        return permissionsFull;
    }

    public String[] getFullPermissionsArray() {
        ArrayList<String> fullPermissions = getFullPermissionList();
        return fullPermissions.toArray(new String[fullPermissions.size()]);
    }

    public HashSet<String> getFullPermissionsSet() {
        return new HashSet<>(getFullPermissionList());
    }

    /**
     * It appears that the default Android permissions in android.Manifest.permissions
     * are prefixed with "android.permission." and then the constant name.
     * FDroid just includes the constant name in the apk list, so we prefix it
     * with "android.permission."
     *
     * see https://gitlab.com/fdroid/fdroidserver/blob/master/fdroidserver/update.py#L535#
     */
    private static String fdroidToAndroidPermission(String permission) {
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
        values.put(Cols.APP_ID, appId);
        values.put(Cols.VERSION_NAME, versionName);
        values.put(Cols.VERSION_CODE, versionCode);
        values.put(Cols.REPO_ID, repo);
        values.put(Cols.HASH, hash);
        values.put(Cols.HASH_TYPE, hashType);
        values.put(Cols.SIGNATURE, sig);
        values.put(Cols.SOURCE_NAME, srcname);
        values.put(Cols.SIZE, size);
        values.put(Cols.NAME, apkName);
        values.put(Cols.MIN_SDK_VERSION, minSdkVersion);
        values.put(Cols.TARGET_SDK_VERSION, targetSdkVersion);
        values.put(Cols.MAX_SDK_VERSION, maxSdkVersion);
        values.put(Cols.ADDED_DATE, Utils.formatDate(added, ""));
        values.put(Cols.PERMISSIONS, Utils.serializeCommaSeparatedString(permissions));
        values.put(Cols.FEATURES, Utils.serializeCommaSeparatedString(features));
        values.put(Cols.NATIVE_CODE, Utils.serializeCommaSeparatedString(nativecode));
        values.put(Cols.INCOMPATIBLE_REASONS, Utils.serializeCommaSeparatedString(incompatibleReasons));
        values.put(Cols.IS_COMPATIBLE, compatible ? 1 : 0);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.versionName);
        dest.writeInt(this.versionCode);
        dest.writeInt(this.size);
        dest.writeLong(this.repo);
        dest.writeString(this.hash);
        dest.writeString(this.hashType);
        dest.writeInt(this.minSdkVersion);
        dest.writeInt(this.targetSdkVersion);
        dest.writeInt(this.maxSdkVersion);
        dest.writeLong(this.added != null ? this.added.getTime() : -1);
        dest.writeStringArray(this.permissions);
        dest.writeStringArray(this.features);
        dest.writeStringArray(this.nativecode);
        dest.writeString(this.sig);
        dest.writeByte(this.compatible ? (byte) 1 : (byte) 0);
        dest.writeString(this.apkName);
        dest.writeSerializable(this.installedFile);
        dest.writeString(this.srcname);
        dest.writeInt(this.repoVersion);
        dest.writeString(this.repoAddress);
        dest.writeStringArray(this.incompatibleReasons);
        dest.writeLong(this.appId);
    }

    protected Apk(Parcel in) {
        this.packageName = in.readString();
        this.versionName = in.readString();
        this.versionCode = in.readInt();
        this.size = in.readInt();
        this.repo = in.readLong();
        this.hash = in.readString();
        this.hashType = in.readString();
        this.minSdkVersion = in.readInt();
        this.targetSdkVersion = in.readInt();
        this.maxSdkVersion = in.readInt();
        long tmpAdded = in.readLong();
        this.added = tmpAdded == -1 ? null : new Date(tmpAdded);
        this.permissions = in.createStringArray();
        this.features = in.createStringArray();
        this.nativecode = in.createStringArray();
        this.sig = in.readString();
        this.compatible = in.readByte() != 0;
        this.apkName = in.readString();
        this.installedFile = (SanitizedFile) in.readSerializable();
        this.srcname = in.readString();
        this.repoVersion = in.readInt();
        this.repoAddress = in.readString();
        this.incompatibleReasons = in.createStringArray();
        this.appId = in.readLong();
    }

    public static final Parcelable.Creator<Apk> CREATOR = new Parcelable.Creator<Apk>() {
        @Override
        public Apk createFromParcel(Parcel source) {
            return new Apk(source);
        }

        @Override
        public Apk[] newArray(int size) {
            return new Apk[size];
        }
    };
}
