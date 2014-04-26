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

import java.io.File;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

abstract public class Installer {
    protected Context mContext;
    protected PackageManager mPm;
    protected InstallerCallback mCallback;

    public static final String TAG = "FDroid";

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

    public interface InstallerCallback {

        public static final int RETURN_SUCCESS = 1;
        public static final int RETURN_CANCEL = 0;

        public void onPackageInstalled(int returnCode, boolean unattended);

        public void onPackageDeleted(int returnCode, boolean unattended);
    }

    public Installer(Context context, PackageManager pm, InstallerCallback callback)
            throws AndroidNotCompatibleException {
        this.mContext = context;
        this.mPm = pm;
        this.mCallback = callback;
    }

    /**
     * Creates a new Installer for installing/deleting processes starting from
     * an Activity
     * 
     * @param context
     * @param pm
     * @param callback
     * @return
     * @throws AndroidNotCompatibleException
     */
    public static Installer getActivityInstaller(Activity activity, PackageManager pm,
            InstallerCallback callback) {

        // system permissions -> SystemPermissionInstaller
        if (hasSystemPermissions(activity, pm)) {
            Log.d(TAG, "system permissions -> SystemPermissionInstaller");

            try {
                return new SystemPermissionInstaller(activity, pm, callback);
            } catch (AndroidNotCompatibleException e) {
                Log.e(TAG, "Android not compatible with SystemPermissionInstaller!", e);
            }
        }

        // try default installer
        try {
            Log.d(TAG, "try default installer");

            return new DefaultInstaller(activity, pm, callback);
        } catch (AndroidNotCompatibleException e) {
            Log.e(TAG,
                    "Android not compatible with DefaultInstaller! This should really not happen!",
                    e);
        }

        // this should not happen!
        return null;
    }

    public static Installer getUnattendedInstaller(Context context, PackageManager pm,
            InstallerCallback callback) throws AndroidNotCompatibleException {

        if (hasSystemPermissions(context, pm)) {
            // we have system permissions!
            return new SystemPermissionInstaller(context, pm, callback);
        } else {
            // nope!
            throw new AndroidNotCompatibleException();
        }
    }

    private static boolean hasSystemPermissions(Context context, PackageManager pm) {
        int checkInstallPermission =
                pm.checkPermission(permission.INSTALL_PACKAGES, context.getPackageName());
        int checkDeletePermission =
                pm.checkPermission(permission.DELETE_PACKAGES, context.getPackageName());
        boolean permissionsGranted = (checkInstallPermission == PackageManager.PERMISSION_GRANTED
                && checkDeletePermission == PackageManager.PERMISSION_GRANTED);

        boolean isSystemApp;
        try {
            isSystemApp = isSystemApp(pm.getApplicationInfo(context.getPackageName(), 0));
        } catch (NameNotFoundException e) {
            isSystemApp = false;
        }

        // TODO: is this right???
        // two ways to be able to get system permissions: somehow the
        // permissions where actually granted on install or the app has been
        // moved later to the system partition -> also access
        if (permissionsGranted || isSystemApp) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isSystemApp(ApplicationInfo ai) {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (ai.flags & mask) != 0;
    }

    public void installPackage(File apkFile) throws AndroidNotCompatibleException {
        // check if file exists...
        if (!apkFile.exists()) {
            Log.d(TAG, "Couldn't find file " + apkFile + " to install.");
            return;
        }

        // extended class now actually installs the package
    }

    public void deletePackage(String packageName) throws AndroidNotCompatibleException {
        // check if package exists before proceeding...
        try {
            mPm.getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Couldn't find package " + packageName + " to delete.");
            return;
        }

        // extended class now actually deletes the package
    }

    public abstract boolean handleOnActivityResult(int requestCode, int resultCode, Intent data);
}
