package org.fdroid.fdroid.data;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.fdroid.database.AppManifest;
import org.fdroid.database.AppVersion;
import org.fdroid.database.Repository;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.CompatibilityChecker;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.installer.ApkCache;
import org.fdroid.fdroid.net.TreeUriDownloader;
import org.fdroid.index.v2.PermissionV2;
import org.fdroid.index.v2.SignerV2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipFile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Represents a single package of an application. This represents one particular
 * package of a given application, for info about the app in general, see
 * {@link App}.
 * <p>
 * <b>Do not rename these instance variables without careful consideration!</b>
 * They are mapped to JSON field names, the {@code fdroidserver} internal variable
 * names, and the {@code fdroiddata} YAML field names.  Only the instance variables
 * decorated with {@code @JsonIgnore} are not directly mapped.
 *
 * @see <a href="https://gitlab.com/fdroid/fdroiddata">fdroiddata</a>
 * @see <a href="https://gitlab.com/fdroid/fdroidserver">fdroidserver</a>
 */
public class Apk implements Comparable<Apk>, Parcelable {

    // Using only byte-range keeps it only 8-bits in the SQLite database
    public static final int SDK_VERSION_MAX_VALUE = Byte.MAX_VALUE;
    public static final int SDK_VERSION_MIN_VALUE = 0;
    public static final String RELEASE_CHANNEL_BETA = "Beta";
    public static final String RELEASE_CHANNEL_STABLE = "Stable";

    // these are never set by the Apk/package index metadata
    public String repoAddress;
    long repoVersion;
    public SanitizedFile installedFile; // the .apk file on this device's filesystem
    public boolean compatible; // True if compatible with the device.
    public long repoId; // the database ID of the repo it comes from

    // these come directly from the index metadata
    public String packageName;
    @Nullable
    public String versionName;
    public long versionCode;
    public long size; // Size in bytes - 0 means we don't know!
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
    public List<String> releaseChannels;
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
     * Standard SHA-256 fingerprint of the X.509 signing certificate.  This can
     * be fetched in a few different ways:
     * <ul>
     *     <li><code>apksigner verify --print-certs example.apk</code></li>
     *     <li><code>jarsigner -verify -verbose -certs index-v1.jar</code></li>
     *     <li><code>keytool -list -v -keystore keystore.jks</code></li>
     * </ul>
     *
     * @see <a href="https://source.android.com/security/apksigning/v3#apk-signature-scheme-v3-block"><tt>signer</tt> in APK Signature Scheme v3</a>
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

    public String whatsNew;

    /**
     * The numeric primary key of the Metadata table, which is used to join apks.
     */
    public long appId;

    public Apk() {
    }

    /**
     * Creates a dummy APK from what is currently installed.
     */
    public Apk(@NonNull PackageInfo packageInfo) {
        packageName = packageInfo.packageName;
        versionName = packageInfo.versionName;
        versionCode = packageInfo.versionCode;
        releaseChannels = Collections.emptyList();

        // zero for "we don't know". If we require this in the future,
        // then we could look up the file on disk if required.
        size = 0;

        // Same as size. We could look this up if required but not needed at time of writing.
        installedFile = null;

        // We couldn't load it from the database, indicating it is not available in any of our repos.
        repoId = 0;
    }

    public Apk(AppVersion v) {
        Repository repo = Objects.requireNonNull(FDroidApp.getRepo(v.getRepoId()));
        repoAddress = repo.getAddress();
        repoVersion = repo.getVersion();
        hash = v.getFile().getSha256();
        hashType = "sha256";
        added = new Date(v.getAdded());
        features = v.getFeatureNames().toArray(new String[0]);
        setPackageName(v.getPackageName());
        compatible = v.isCompatible();
        AppManifest manifest = v.getManifest();
        minSdkVersion = manifest.getUsesSdk() == null ?
                SDK_VERSION_MIN_VALUE : manifest.getUsesSdk().getMinSdkVersion();
        targetSdkVersion = manifest.getUsesSdk() == null ?
                minSdkVersion : manifest.getUsesSdk().getTargetSdkVersion();
        maxSdkVersion = manifest.getMaxSdkVersion() == null ? SDK_VERSION_MAX_VALUE : manifest.getMaxSdkVersion();
        List<String> channels = v.getReleaseChannels();
        if (channels.isEmpty()) {
            // no channels means stable
            releaseChannels = Collections.singletonList(RELEASE_CHANNEL_STABLE);
        } else {
            releaseChannels = channels;
        }
        // obbMainFile = cursor.getString(i);
        // obbMainFileSha256 = cursor.getString(i);
        // obbPatchFile = cursor.getString(i);
        // obbPatchFileSha256 = cursor.getString(i);
        apkName = v.getFile().getName();
        setRequestedPermissions(v.getUsesPermission(), 0);
        setRequestedPermissions(v.getUsesPermissionSdk23(), 23);
        nativecode = v.getNativeCode().toArray(new String[0]);
        repoId = v.getRepoId();
        SignerV2 signer = v.getManifest().getSigner();
        sig = signer == null ? null : signer.getSha256().get(0);
        size = v.getFile().getSize() == null ? 0 : v.getFile().getSize();
        srcname = v.getSrc() == null ? null : v.getSrc().getName();
        versionName = manifest.getVersionName();
        versionCode = manifest.getVersionCode();
        antiFeatures = v.getAntiFeatureKeys().toArray(new String[0]);
        whatsNew = v.getWhatsNew(App.getLocales());
    }

    public void setCompatibility(CompatibilityChecker checker) {
        final List<String> reasons = checker.getIncompatibleReasons(this);
        if (reasons.isEmpty()) {
            compatible = true;
            incompatibleReasons = null;
        } else {
            compatible = false;
            incompatibleReasons = reasons.toArray(new String[reasons.size()]);
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

    /**
     * Get the URL that points to the canonical download source for this
     * package.  This is also used as the unique ID for tracking downloading,
     * progress, and notifications throughout the whole install process.  It
     * is guaranteed to uniquely represent this file since it points to a file
     * on the file system of the canonical webserver.
     *
     * @see org.fdroid.fdroid.installer.InstallManagerService
     */
    public String getCanonicalUrl() {
        checkRepoAddress();
        String address = repoAddress;
        /* Each String in pathElements might contain a /, should keep these as path elements */
        List<String> elements = new ArrayList<>();
        Collections.addAll(elements, apkName.split("/"));
        /*
         * Storage Access Framework URLs have this wacky URL-encoded path within the URL path.
         *
         * i.e.
         * content://authority/tree/313E-1F1C%3A/document/313E-1F1C%3Aguardianproject.info%2Ffdroid%2Frepo
         *
         * Currently don't know a better way to identify these than by content:// prefix,
         * seems the Android SDK expects apps to consider them as opaque identifiers.
         */
        if (address.startsWith("content://")) {
            StringBuilder result = new StringBuilder(address);
            for (String element : elements) {
                result.append(TreeUriDownloader.ESCAPED_SLASH);
                result.append(element);
            }
            return result.toString();
        } else { // Normal URL
            Uri.Builder result = Uri.parse(address).buildUpon();
            for (String element : elements) {
                result.appendPath(element);
            }
            return result.build().toString();
        }
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
    @TargetApi(19)
    public int compareTo(@NonNull Apk apk) {
        return Long.compare(versionCode, apk.versionCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.versionName);
        dest.writeLong(this.versionCode);
        dest.writeLong(this.size);
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
        dest.writeLong(this.repoVersion);
        dest.writeString(this.repoAddress);
        dest.writeStringArray(this.incompatibleReasons);
        dest.writeStringArray(this.antiFeatures);
        dest.writeLong(this.appId);
    }

    protected Apk(Parcel in) {
        this.packageName = in.readString();
        this.versionName = in.readString();
        this.versionCode = in.readLong();
        this.size = in.readLong();
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
        this.repoVersion = in.readLong();
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

    /**
     * Set the Package Name property while ensuring it is sanitized.
     */
    void setPackageName(String packageName) {
        if (Utils.isSafePackageName(packageName)) {
            this.packageName = packageName;
        } else {
            throw new IllegalArgumentException("Repo index package entry includes unsafe packageName: '"
                    + packageName + "'");
        }
    }

    /**
     * Generate the set of requested permissions for the current Android version.
     * <p>
     * There are also a bunch of crazy rules where having one permission will imply
     * another permission, for example:
     * {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} implies
     * {@link Manifest.permission#READ_EXTERNAL_STORAGE}.
     * Many of these rules are for quite old Android versions,
     * so they are not included here.
     *
     * @see Manifest.permission#READ_EXTERNAL_STORAGE
     * @see <a href="https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/data/etc/platform.xml">platform.xml</a>
     */
    @VisibleForTesting
    public void setRequestedPermissions(List<PermissionV2> permissions, int minSdk) {
        HashSet<String> set = new HashSet<>();
        if (requestedPermissions != null) {
            Collections.addAll(set, requestedPermissions);
        }
        for (PermissionV2 versions : permissions) {
            int maxSdk = Integer.MAX_VALUE;
            if (versions.getMaxSdkVersion() != null) {
                maxSdk = versions.getMaxSdkVersion();
            }
            if (minSdk <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT <= maxSdk) {
                set.add(versions.getName());
            }
        }
        if (set.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            set.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            if (set.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                set.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (targetSdkVersion < 29) {
                if (set.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    set.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                }
                if (set.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    set.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                }
                if (set.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    set.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
                }
            }
            // Else do nothing. The targetSdk for the below split-permissions is set to 29,
            // so we don't make any changes for apps targetting 29 or above
        }
        if (Build.VERSION.SDK_INT >= 31) {
            if (targetSdkVersion < 31) {
                if (set.contains(Manifest.permission.BLUETOOTH) ||
                        set.contains(Manifest.permission.BLUETOOTH_ADMIN)) {
                    set.add(Manifest.permission.BLUETOOTH_SCAN);
                    set.add(Manifest.permission.BLUETOOTH_CONNECT);
                    set.add(Manifest.permission.BLUETOOTH_ADVERTISE);
                }
            }
            // Else do nothing. The targetSdk for the above split-permissions is set to 31,
            // so we don't make any changes for apps targetting 31 or above
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (targetSdkVersion < 33) {
                // TODO: Change the strings below to Manifest.permission once we compile with SDK 33
                if (set.contains(Manifest.permission.BODY_SENSORS)) {
                    set.add("android.permission.BODY_SENSORS_BACKGROUND");
                }
                if (set.contains(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                        set.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    set.add("android.permission.READ_MEDIA_AUDIO");
                    set.add("android.permission.READ_MEDIA_VIDEO");
                    set.add("android.permission.READ_MEDIA_IMAGES");
                }
            }
            // Else do nothing. The targetSdk for the above split-permissions is set to 33,
            // so we don't make any changes for apps targetting 33 or above
        }

        requestedPermissions = set.toArray(new String[set.size()]);
    }

    /**
     * Get the install path for a "non-apk" media file, with special cases for
     * files that can be usefully installed without PrivilegedExtension.
     * Defaults to {@link android.os.Environment#DIRECTORY_DOWNLOADS}.
     *
     * @return the install path for this {@link Apk}
     * @link <a href="https://source.android.com/devices/tech/ota/nonab/inside_packages">Inside OTA Packages</a>
     */

    public File getMediaInstallPath(Context context) {
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS); // Default for all other non-apk/media files
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(this.getCanonicalUrl());
        if (TextUtils.isEmpty(fileExtension)) return path;
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeType = mimeTypeMap.getMimeTypeFromExtension(fileExtension);
        String topLevelType = null;
        if (!TextUtils.isEmpty(mimeType)) {
            String[] mimeTypeSections = mimeType.split("/");
            if (mimeTypeSections.length == 0) {
                topLevelType = "";
            } else {
                topLevelType = mimeTypeSections[0];
            }
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
        } else if ("zip".equals(fileExtension)) {
            try {
                File cachedFile = ApkCache.getApkDownloadPath(context, this.getCanonicalUrl());
                ZipFile zipFile = new ZipFile(cachedFile);
                if (zipFile.size() == 1) {
                    String name = zipFile.entries().nextElement().getName();
                    if (name != null && name.endsWith(".obf")) {
                        // temporarily cache this, it will be deleted after unzipping
                        return context.getCacheDir();
                    }
                } else if (zipFile.getEntry("META-INF/com/google/android/update-binary") != null) {
                    // Over-The-Air update ZIP files
                    return new File(context.getApplicationInfo().dataDir + "/ota");
                }
            } catch (IOException e) {
                // this should happen when running isMediaInstalled() and the file isn't installed
                // other cases are probably bugs
                if (BuildConfig.DEBUG) e.printStackTrace();
            }
            return path;
        } else if ("apk".equals(fileExtension)) {
            throw new IllegalStateException("APKs should not be handled in the media install path!");
        }
        return path;
    }

    public File getInstalledMediaFile(Context context) {
        return new File(this.getMediaInstallPath(context), SanitizedFile.sanitizeFileName(this.apkName));
    }

    /**
     * Check whether a media file is "installed" as based on the file type's
     * install path, derived in {@link #getMediaInstallPath(Context)}
     */
    public boolean isMediaInstalled(Context context) {
        return getInstalledMediaFile(context).isFile();
    }

    /**
     * Default to assuming apk if apkName is null since that has always been
     * what we had.
     *
     * @return true if this is an apk instead of a non-apk/media file
     */
    public boolean isApk() {
        return apkName == null
                || apkName.substring(apkName.length() - 4).toLowerCase(Locale.ENGLISH).endsWith(".apk");
    }
}
