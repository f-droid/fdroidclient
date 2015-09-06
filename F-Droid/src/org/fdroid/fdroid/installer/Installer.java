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

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;

import java.io.File;
import java.util.List;

/**
 * Abstract Installer class. Also provides static methods to automatically
 * instantiate a working Installer based on F-Droids granted permissions.
 */
abstract public class Installer {
    protected final Context mContext;
    protected final PackageManager mPm;
    protected final InstallerCallback mCallback;

    private static final String TAG = "Installer";

    /**
     * This is thrown when an Installer is not compatible with the Android OS it
     * is running on. This could be due to a broken superuser in case of
     * RootInstaller or due to an incompatible Android version in case of
     * SystemPermissionInstaller
     */
    public static class AndroidNotCompatibleException extends Exception {

        private static final long serialVersionUID = -8343133906463328027L;

        public AndroidNotCompatibleException() {
        }

        public AndroidNotCompatibleException(String message) {
            super(message);
        }

        public AndroidNotCompatibleException(Throwable cause) {
            super(cause);
        }

        public AndroidNotCompatibleException(String message, Throwable cause) {
            super(message, cause);
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

    public Installer(Context context, PackageManager pm, InstallerCallback callback)
            throws AndroidNotCompatibleException {
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
        boolean isSystemInstallerEnabled = Preferences.get().isSystemInstallerEnabled();
        if (isSystemInstallerEnabled) {
            if (hasSystemPermissions(activity, pm)) {
                Utils.DebugLog(TAG, "system permissions -> SystemInstaller");

                try {
                    return new SystemInstaller(activity, pm, callback);
                } catch (AndroidNotCompatibleException e) {
                    Log.e(TAG, "Android not compatible with SystemInstaller!", e);
                }
            } else {
                Log.e(TAG, "SystemInstaller is enabled in prefs, but system-perms are not granted!");
            }
        }

        // else -> DefaultInstaller
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Default installer on Android >= 4.0
            try {
                Utils.DebugLog(TAG, "try default installer for Android >= 4");

                return new DefaultInstallerSdk14(activity, pm, callback);
            } catch (AndroidNotCompatibleException e) {
                Log.e(TAG, "Android not compatible with DefaultInstallerSdk14!", e);
            }
        } else {
            // Default installer on Android < 4.0
            try {
                Utils.DebugLog(TAG, "try default installer for Android < 4");

                return new DefaultInstaller(activity, pm, callback);
            } catch (AndroidNotCompatibleException e) {
                Log.e(TAG, "Android not compatible with DefaultInstaller!", e);
            }
        }

        // this should not happen!
        return null;
    }

    public static boolean hasSystemPermissions(Context context, PackageManager pm) {
        boolean hasInstallPermission =
                (pm.checkPermission(permission.INSTALL_PACKAGES, context.getPackageName())
                        == PackageManager.PERMISSION_GRANTED);
        boolean hasDeletePermission =
                (pm.checkPermission(permission.DELETE_PACKAGES, context.getPackageName())
                        == PackageManager.PERMISSION_GRANTED);

        return (hasInstallPermission && hasDeletePermission);
    }

    public void installPackage(File apkFile) throws AndroidNotCompatibleException {
        // check if file exists...
        if (!apkFile.exists()) {
            Log.e(TAG, "Couldn't find file " + apkFile + " to install.");
            return;
        }

        installPackageInternal(apkFile);
    }

    public void installPackage(List<File> apkFiles) throws AndroidNotCompatibleException {
        // check if files exist...
        for (File apkFile : apkFiles) {
            if (!apkFile.exists()) {
                Log.e(TAG, "Couldn't find file " + apkFile + " to install.");
                return;
            }
        }

        installPackageInternal(apkFiles);
    }

    public void deletePackage(String packageName) throws AndroidNotCompatibleException {
        // check if package exists before proceeding...
        try {
            mPm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Couldn't find package " + packageName + " to delete.");
            return;
        }

        deletePackageInternal(packageName);
    }

    protected abstract void installPackageInternal(File apkFile)
            throws AndroidNotCompatibleException;

    protected abstract void installPackageInternal(List<File> apkFiles)
            throws AndroidNotCompatibleException;

    protected abstract void deletePackageInternal(String packageName)
            throws AndroidNotCompatibleException;

    public abstract boolean handleOnActivityResult(int requestCode, int resultCode, Intent data);

    public abstract boolean supportsUnattendedOperations();
}
