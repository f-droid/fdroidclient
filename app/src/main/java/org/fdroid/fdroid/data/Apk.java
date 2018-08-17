package org.fdroid.fdroid.data;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.fdroid.fdroid.RepoXMLHandler;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.ApkTable.Cols;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

/**
 * Represents a single package of an application. This represents one particular
 * package of a given application, for info about the app in general, see
 * {@link App}.
 * <p>
 * <b>Do not rename these instance variables without careful consideration!</b>
 * They are mapped to JSON field names, the {@code fdroidserver} internal variable
 * names, and the {@code fdroiddata} YAML field names.  Only the instance variables
 * decorated with {@code @JsonIgnore} are not directly mapped.
 * <p>
 * <b>NOTE:</b>If an instance variable is only meant for internal state, and not for
 * representing data coming from the server, then it must also be decorated with
 * {@code @JsonIgnore} to prevent abuse!  The tests for
 * {@link org.fdroid.fdroid.IndexV1Updater} will also have to be updated.
 *
 * @see <a href="https://gitlab.com/fdroid/fdroiddata">fdroiddata</a>
 * @see <a href="https://gitlab.com/fdroid/fdroidserver">fdroidserver</a>
 */
public class Apk extends ValueObject implements Comparable<Apk>, Parcelable {

    // Using only byte-range keeps it only 8-bits in the SQLite database
    @JsonIgnore
    public static final int SDK_VERSION_MAX_VALUE = Byte.MAX_VALUE;
    @JsonIgnore
    public static final int SDK_VERSION_MIN_VALUE = 0;

    // these are never set by the Apk/package index metadata
    @JsonIgnore
    String repoAddress;
    @JsonIgnore
    int repoVersion;
    @JsonIgnore
    public SanitizedFile installedFile; // the .apk file on this device's filesystem
    @JsonIgnore
    public boolean compatible; // True if compatible with the device.

    @JacksonInject("repoId")
    public long repoId; // the database ID of the repo it comes from

    // these come directly from the index metadata
    public String packageName;
    @Nullable
    public String versionName;
    public int versionCode;
    public int size; // Size in bytes - 0 means we don't know!
    @NonNull
    public String hash; // checksum of the APK, in lowercase hex
    public String hashType;
    public int minSdkVersion = SDK_VERSION_MIN_VALUE; // 0 if unknown
    public int targetSdkVersion = SDK_VERSION_MIN_VALUE; // 0 if unknown
    public int maxSdkVersion = SDK_VERSION_MAX_VALUE; // "infinity" if not set
    public String obbMainFile;
    public String obbMainFileSha256;
    public String obbPatchFile;
    public String obbPatchFileSha256;
    public Date added;
    /**
     * The array of the names of the permissions that this APK requests. This is the
     * same data as {@link android.content.pm.PackageInfo#requestedPermissions}. Note this
     * does not mean that all these permissions have been granted, only requested.  For
     * example, a regular app can request a system permission, but it won't be granted it.
     */
    public String[] requestedPermissions;
    public String[] features; // null if empty or unknown

    public String[] nativecode; // null if empty or unknown

    /**
     * ID (md5 sum of public key) of signature. Might be null, in the
     * transition to this field existing.
     */
    public String sig;

    public String apkName; // F-Droid style APK name

    /**
     * If not null, this is the name of the source tarball for the
     * application. Null indicates that it's a developer's binary
     * build - otherwise it's built from source.
     */
    public String srcname;

    public String[] incompatibleReasons;

    public String[] antiFeatures;

    /**
     * The numeric primary key of the Metadata table, which is used to join apks.
     */
    public long appId;

    public Apk() {
    }

    /**
     * If you need an {@link Apk} but it is no longer in the database any more (e.g. because the
     * version you have installed is no longer in the repository metadata) then you can instantiate
     * an {@link Apk} via an {@link InstalledApp} instance.
     * <p>
     * Note: Many of the fields on this instance will not be known in this circumstance. Currently
     * the only things that are known are:
     * <p>
     * <ul>
     * <li>{@link Apk#packageName}
     * <li>{@link Apk#versionName}
     * <li>{@link Apk#versionCode}
     * <li>{@link Apk#hash}
     * <li>{@link Apk#hashType}
     * </ul>
     * <p>
     * This could instead be implemented by accepting a {@link PackageInfo} and it would get much
     * the same information, but it wouldn't have the hash of the package. Seeing as we've already
     * done the hard work to calculate that hash and stored it in the database, we may as well use
     * that.
     */
    public Apk(@NonNull InstalledApp app) {
        packageName = app.getPackageName();
        versionName = app.getVersionName();
        versionCode = app.getVersionCode();
        hash = app.getHash(); // checksum of the APK, in lowercase hex
        hashType = app.getHashType();

        // zero for "we don't know". If we require this in the future, then we could look up the
        // file on disk if required.
        size = 0;

        // Same as size. We could look this up if required but not needed at time of writing.
        installedFile = null;

        // If we are being created from an InstalledApp, it is because we couldn't load it from the
        // apk table in the database, indicating it is not available in any of our repos.
        repoId = 0;
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
                case Cols.Package.PACKAGE_NAME:
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
                case Cols.OBB_MAIN_FILE:
                    obbMainFile = cursor.getString(i);
                    break;
                case Cols.OBB_MAIN_FILE_SHA256:
                    obbMainFileSha256 = cursor.getString(i);
                    break;
                case Cols.OBB_PATCH_FILE:
                    obbPatchFile = cursor.getString(i);
                    break;
                case Cols.OBB_PATCH_FILE_SHA256:
                    obbPatchFileSha256 = cursor.getString(i);
                    break;
                case Cols.NAME:
                    apkName = cursor.getString(i);
                    break;
                case Cols.REQUESTED_PERMISSIONS:
                    requestedPermissions = convertToRequestedPermissions(cursor.getString(i));
                    break;
                case Cols.NATIVE_CODE:
                    nativecode = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.INCOMPATIBLE_REASONS:
                    incompatibleReasons = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.REPO_ID:
                    repoId = cursor.getInt(i);
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
                case Cols.AntiFeatures.ANTI_FEATURES:
                    antiFeatures = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
            }
        }
    }

    private void checkRepoAddress() {
        if (repoAddress == null || apkName == null) {
            throw new IllegalStateException(
                    "Apk needs to have both Schema.ApkTable.Cols.REPO_ADDRESS and "
                            + "Schema.ApkTable.Cols.NAME set in order to calculate URL "
                            + "[package: " + packageName
                            + ", versionCode: " + versionCode
                            + ", apkName: " + apkName
                            + ", repoAddress: " + repoAddress
                            + ", repoId: " + repoId + "]");
        }
    }

    @JsonIgnore  // prevent tests from failing due to nulls in checkRepoAddress()
    public String getUrl() {
        checkRepoAddress();
        return repoAddress + "/" + apkName.replace(" ", "%20");
    }

    /**
     * Get the URL to download the <i>main</i> expansion file, the primary
     * expansion file for additional resources required by your application.
     * The filename will always have the format:
     * "main.<i>versionCode</i>.<i>packageName</i>.obb"
     *
     * @return a URL to download the OBB file that matches this APK
     * @see #getPatchObbUrl()
     * @see <a href="https://developer.android.com/google/play/expansion-files.html">APK Expansion Files</a>
     */
    public String getMainObbUrl() {
        if (repoAddress == null || obbMainFile == null) {
            return null;
        }
        checkRepoAddress();
        return repoAddress + "/" + obbMainFile;
    }

    /**
     * Get the URL to download the optional <i>patch</i> expansion file, which
     * is intended for small updates to the <i>main</i> expansion file.
     * The filename will always have the format:
     * "patch.<i>versionCode</i>.<i>packageName</i>.obb"
     *
     * @return a URL to download the OBB file that matches this APK
     * @see #getMainObbUrl()
     * @see <a href="https://developer.android.com/google/play/expansion-files.html">APK Expansion Files</a>
     */
    public String getPatchObbUrl() {
        if (repoAddress == null || obbPatchFile == null) {
            return null;
        }
        checkRepoAddress();
        return repoAddress + "/" + obbPatchFile;
    }

    /**
     * Get the local {@link File} to the "main" OBB file.
     */
    public File getMainObbFile() {
        if (obbMainFile == null) {
            return null;
        }
        return new File(App.getObbDir(packageName), obbMainFile);
    }

    /**
     * Get the local {@link File} to the "patch" OBB file.
     */
    public File getPatchObbFile() {
        if (obbPatchFile == null) {
            return null;
        }
        return new File(App.getObbDir(packageName), obbPatchFile);
    }

    @Override
    public String toString() {
        return toContentValues().toString();
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(Cols.APP_ID, appId);
        values.put(Cols.VERSION_NAME, versionName);
        values.put(Cols.VERSION_CODE, versionCode);
        values.put(Cols.REPO_ID, repoId);
        values.put(Cols.HASH, hash);
        values.put(Cols.HASH_TYPE, hashType);
        values.put(Cols.SIGNATURE, sig);
        values.put(Cols.SOURCE_NAME, srcname);
        values.put(Cols.SIZE, size);
        values.put(Cols.NAME, apkName);
        values.put(Cols.MIN_SDK_VERSION, minSdkVersion);
        values.put(Cols.TARGET_SDK_VERSION, targetSdkVersion);
        values.put(Cols.MAX_SDK_VERSION, maxSdkVersion);
        values.put(Cols.OBB_MAIN_FILE, obbMainFile);
        values.put(Cols.OBB_MAIN_FILE_SHA256, obbMainFileSha256);
        values.put(Cols.OBB_PATCH_FILE, obbPatchFile);
        values.put(Cols.OBB_PATCH_FILE_SHA256, obbPatchFileSha256);
        values.put(Cols.ADDED_DATE, Utils.formatDate(added, ""));
        values.put(Cols.REQUESTED_PERMISSIONS, Utils.serializeCommaSeparatedString(requestedPermissions));
        values.put(Cols.FEATURES, Utils.serializeCommaSeparatedString(features));
        values.put(Cols.NATIVE_CODE, Utils.serializeCommaSeparatedString(nativecode));
        values.put(Cols.INCOMPATIBLE_REASONS, Utils.serializeCommaSeparatedString(incompatibleReasons));
        values.put(Cols.AntiFeatures.ANTI_FEATURES, Utils.serializeCommaSeparatedString(antiFeatures));
        values.put(Cols.IS_COMPATIBLE, compatible ? 1 : 0);
        return values;
    }

    @Override
    @TargetApi(19)
    public int compareTo(@NonNull Apk apk) {
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
        dest.writeLong(this.repoId);
        dest.writeString(this.hash);
        dest.writeString(this.hashType);
        dest.writeInt(this.minSdkVersion);
        dest.writeInt(this.targetSdkVersion);
        dest.writeInt(this.maxSdkVersion);
        dest.writeString(this.obbMainFile);
        dest.writeString(this.obbMainFileSha256);
        dest.writeString(this.obbPatchFile);
        dest.writeString(this.obbPatchFileSha256);
        dest.writeLong(this.added != null ? this.added.getTime() : -1);
        dest.writeStringArray(this.requestedPermissions);
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
        dest.writeStringArray(this.antiFeatures);
        dest.writeLong(this.appId);
    }

    protected Apk(Parcel in) {
        this.packageName = in.readString();
        this.versionName = in.readString();
        this.versionCode = in.readInt();
        this.size = in.readInt();
        this.repoId = in.readLong();
        this.hash = in.readString();
        this.hashType = in.readString();
        this.minSdkVersion = in.readInt();
        this.targetSdkVersion = in.readInt();
        this.maxSdkVersion = in.readInt();
        this.obbMainFile = in.readString();
        this.obbMainFileSha256 = in.readString();
        this.obbPatchFile = in.readString();
        this.obbPatchFileSha256 = in.readString();
        long tmpAdded = in.readLong();
        this.added = tmpAdded == -1 ? null : new Date(tmpAdded);
        this.requestedPermissions = in.createStringArray();
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
        this.antiFeatures = in.createStringArray();
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

    private String[] convertToRequestedPermissions(String permissionsFromDb) {
        String[] array = Utils.parseCommaSeparatedString(permissionsFromDb);
        if (array != null) {
            HashSet<String> requestedPermissionsSet = new HashSet<>();
            for (String permission : array) {
                requestedPermissionsSet.add(RepoXMLHandler.fdroidToAndroidPermission(permission));
            }
            return requestedPermissionsSet.toArray(new String[requestedPermissionsSet.size()]);
        }
        return null;
    }

    @JsonProperty("uses-permission")
    @SuppressWarnings("unused")
    private void setUsesPermission(Object[][] permissions) {
        setRequestedPermissions(permissions, 0);
    }

    @JsonProperty("uses-permission-sdk-23")
    @SuppressWarnings("unused")
    private void setUsesPermissionSdk23(Object[][] permissions) {
        setRequestedPermissions(permissions, 23);
    }

    private void setRequestedPermissions(Object[][] permissions, int minSdk) {
        HashSet<String> set = new HashSet<>();
        if (requestedPermissions != null) {
            Collections.addAll(set, requestedPermissions);
        }
        for (Object[] versions : permissions) {
            int maxSdk = Integer.MAX_VALUE;
            if (versions[1] != null) {
                maxSdk = (int) versions[1];
            }
            if (minSdk <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT <= maxSdk) {
                set.add((String) versions[0]);
            }
        }
        requestedPermissions = set.toArray(new String[set.size()]);
    }

    /**
     * Get the install path for a "non-apk" media file
     * Defaults to {@link android.os.Environment#DIRECTORY_DOWNLOADS}
     *
     * @return the install path for this {@link Apk}
     */

    public File getMediaInstallPath(Context context) {
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS); // Default for all other non-apk/media files
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(this.getUrl());
        if (TextUtils.isEmpty(fileExtension)) return path;
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String[] mimeType = mimeTypeMap.getMimeTypeFromExtension(fileExtension).split("/");
        String topLevelType;
        if (mimeType.length == 0) {
            topLevelType = "";
        } else {
            topLevelType = mimeType[0];
        }
        if ("audio".equals(topLevelType)) {
            path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC);
        } else if ("image".equals(topLevelType)) {
            path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
        } else if ("video".equals(topLevelType)) {
            path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES);
            // TODO support OsmAnd map files, other map apps?
            //} else if (mimeTypeMap.hasExtension("map")) {  // OsmAnd map files
            //} else if (this.apkName.matches(".*.ota_[0-9]*.zip")) {  // Over-The-Air update ZIP files
        } else if (this.apkName.endsWith(".zip")) {  // Over-The-Air update ZIP files
            path = new File(context.getApplicationInfo().dataDir + "/ota");
        }
        return path;
    }

    public boolean isMediaInstalled(Context context) {
        return new File(this.getMediaInstallPath(context), SanitizedFile.sanitizeFileName(this.apkName)).isFile();
    }

    /**
     * Default to assuming apk if apkName is null since that has always been
     * what we had.
     *
     * @return true if this is an apk instead of a non-apk/media file
     */
    public boolean isApk() {
        return this.apkName == null || this.apkName.endsWith(".apk");
    }
}
