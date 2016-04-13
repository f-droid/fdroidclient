/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.fdroid.fdroid.installer;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.AndroidXMLDecompress;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.FileCompat;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.fdroid.privileged.install.InstallExtensionDialogActivity;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Abstract Installer class. Also provides static methods to automatically
 * instantiate a working Installer based on F-Droids granted permissions.
 */
public abstract class Installer {
    final Context mContext;
    final PackageManager mPm;
    final InstallerCallback mCallback;

    private static final String TAG = "Installer";

    /**
     * This is thrown when an Installer is not compatible with the Android OS it
     * is running on. This could be due to a broken superuser in case of
     * RootInstaller or due to an incompatible Android version in case of
     * SystemPermissionInstaller
     */
    public static class InstallFailedException extends Exception {

        private static final long serialVersionUID = -8343133906463328027L;

        public InstallFailedException(String message) {
            super(message);
        }

        public InstallFailedException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Callback from Installer. NOTE: This callback can be in a different thread
     * than the UI thread
     */
    public interface InstallerCallback {

        int OPERATION_INSTALL = 1;
        int OPERATION_DELETE  = 2;

        // Avoid using [-1,1] as they may conflict with Activity.RESULT_*
        int ERROR_CODE_CANCELED     = 2;
        int ERROR_CODE_OTHER        = 3;
        int ERROR_CODE_CANNOT_PARSE = 4;

        void onSuccess(int operation);

        void onError(int operation, int errorCode);
    }

    Installer(Context context, PackageManager pm, InstallerCallback callback)
            throws InstallFailedException {
        this.mContext = context;
        this.mPm = pm;
        this.mCallback = callback;
    }

    public static Installer getActivityInstaller(Activity activity, InstallerCallback callback) {
        return getActivityInstaller(activity, activity.getPackageManager(), callback);
    }

    /**
     * Creates a new Installer for installing/deleting processes starting from
     * an Activity
     */
    public static Installer getActivityInstaller(Activity activity, PackageManager pm,
            InstallerCallback callback) {

        // system permissions and pref enabled -> SystemInstaller
        boolean isSystemInstallerEnabled = Preferences.get().isPrivilegedInstallerEnabled();
        if (isSystemInstallerEnabled) {
            if (PrivilegedInstaller.isExtensionInstalledCorrectly(activity)
                    == PrivilegedInstaller.IS_EXTENSION_INSTALLED_YES) {
                Utils.debugLog(TAG, "system permissions -> SystemInstaller");

                try {
                    return new PrivilegedInstaller(activity, pm, callback);
                } catch (InstallFailedException e) {
                    Log.e(TAG, "Android not compatible with SystemInstaller!", e);
                }
            } else {
                Log.e(TAG, "SystemInstaller is enabled in prefs, but system-perms are not granted!");
            }
        }

        // else -> DefaultInstaller
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            // Default installer on Android >= 4.0
            try {
                Utils.debugLog(TAG, "try default installer for android >= 14");

                return new DefaultSdk14Installer(activity, pm, callback);
            } catch (InstallFailedException e) {
                Log.e(TAG, "Android not compatible with DefaultInstallerSdk14!", e);
            }
        } else {
            // Default installer on Android < 4.0 (android-14)
            try {
                Utils.debugLog(TAG, "try default installer for android < 14");

                return new DefaultInstaller(activity, pm, callback);
            } catch (InstallFailedException e) {
                Log.e(TAG, "Android not compatible with DefaultInstaller!", e);
            }
        }

        // this should not happen!
        return null;
    }

    /**
     * Checks the APK file against the provided hash, returning whether it is a match.
     */
    private static boolean verifyApkFile(File apkFile, String hash, String hashType)
            throws NoSuchAlgorithmException {
        if (!apkFile.exists()) {
            return false;
        }
        Hasher hasher = new Hasher(hashType, apkFile);
        return hasher.match(hash);
    }

    /**
     * This is the safe, single point of entry for submitting an APK file to be installed.
     */
    public void installPackage(File apkFile, String packageName, String urlString)
            throws InstallFailedException {
        SanitizedFile apkToInstall;
        try {
            Map<String, Object> attributes = AndroidXMLDecompress.getManifestHeaderAttributes(apkFile.getAbsolutePath());

            /* This isn't really needed, but might as well since we have the data already */
            if (attributes.containsKey("packageName") && !TextUtils.equals(packageName, (String) attributes.get("packageName"))) {
                throw new InstallFailedException(apkFile + " has packageName that clashes with " + packageName);
            }

            if (!attributes.containsKey("versionCode")) {
                throw new InstallFailedException(apkFile + " is missing versionCode!");
            }
            int versionCode = (Integer) attributes.get("versionCode");
            Apk apk = ApkProvider.Helper.find(mContext, packageName, versionCode, new String[]{
                    ApkProvider.DataColumns.HASH,
                    ApkProvider.DataColumns.HASH_TYPE,
            });
            /* Always copy the APK to the safe location inside of the protected area
             * of the app to prevent attacks based on other apps swapping the file
             * out during the install process. Most likely, apkFile was just downloaded,
             * so it should still be in the RAM disk cache */
            apkToInstall = SanitizedFile.knownSanitized(File.createTempFile("install-", ".apk", mContext.getFilesDir()));
            FileUtils.copyFile(apkFile, apkToInstall);
            if (!verifyApkFile(apkToInstall, apk.hash, apk.hashType)) {
                FileUtils.deleteQuietly(apkFile);
                throw new InstallFailedException(apkFile + " failed to verify!");
            }
            apkFile = null; // ensure this is not used now that its copied to apkToInstall

            // special case: F-Droid Privileged Extension
            if (packageName != null && packageName.equals(PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME)) {

                // extension must be signed with the same public key as main F-Droid
                // NOTE: Disabled for debug builds to be able to use official extension from repo
                ApkSignatureVerifier signatureVerifier = new ApkSignatureVerifier(mContext);
                if (!BuildConfig.DEBUG && !signatureVerifier.hasFDroidSignature(apkToInstall)) {
                    throw new InstallFailedException("APK signature of extension not correct!");
                }

                Activity activity = (Activity) mContext;
                Intent installIntent = new Intent(activity, InstallExtensionDialogActivity.class);
                installIntent.setAction(InstallExtensionDialogActivity.ACTION_INSTALL);
                installIntent.putExtra(InstallExtensionDialogActivity.EXTRA_INSTALL_APK, apkToInstall.getAbsolutePath());
                activity.startActivity(installIntent);
                return;
            }

            // Need the apk to be world readable, so that the installer is able to read it.
            // Note that saving it into external storage for the purpose of letting the installer
            // have access is insecure, because apps with permission to write to the external
            // storage can overwrite the app between F-Droid asking for it to be installed and
            // the installer actually installing it.
            FileCompat.setReadable(apkToInstall, true);
            installPackageInternal(apkToInstall);

            NotificationManager nm = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(Utils.getApkUrlNotificationId(urlString));
        } catch (NumberFormatException | NoSuchAlgorithmException | IOException e) {
            throw new InstallFailedException(e);
        } catch (ClassCastException e) {
            throw new InstallFailedException("F-Droid Privileged can only be updated using an activity!");
        }
    }

    public void deletePackage(String packageName) throws InstallFailedException {
        // check if package exists before proceeding...
        try {
            mPm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Couldn't find package " + packageName + " to delete.");
            return;
        }

        // special case: F-Droid Privileged Extension
        if (packageName != null && packageName.equals(PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME)) {
            Activity activity;
            try {
                activity = (Activity) mContext;
            } catch (ClassCastException e) {
                Utils.debugLog(TAG, "F-Droid Privileged can only be uninstalled using an activity!");
                return;
            }

            Intent uninstallIntent = new Intent(activity, InstallExtensionDialogActivity.class);
            uninstallIntent.setAction(InstallExtensionDialogActivity.ACTION_UNINSTALL);
            activity.startActivity(uninstallIntent);
            return;
        }

        deletePackageInternal(packageName);
    }

    protected abstract void installPackageInternal(File apkFile)
            throws InstallFailedException;

    protected abstract void deletePackageInternal(String packageName)
            throws InstallFailedException;

    public abstract boolean handleOnActivityResult(int requestCode, int resultCode, Intent data);
}
