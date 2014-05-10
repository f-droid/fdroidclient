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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;

/**
 * For Android >= 4.0 Default Installer using the public PackageManager API of
 * Android to install/delete packages. This starts a Activity from the Android
 * OS showing all permissions/changed permissions. The the user needs to
 * manually press an install button, this Installer cannot be used for
 * unattended installations.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DefaultInstallerSdk14 extends Installer {
    private Activity mActivity;

    public DefaultInstallerSdk14(Activity activity, PackageManager pm, InstallerCallback callback)
            throws AndroidNotCompatibleException {
        super(activity, pm, callback);
        this.mActivity = activity;
    }

    private static final int REQUEST_CODE_INSTALL = 0;
    private static final int REQUEST_CODE_DELETE = 1;

    @SuppressWarnings("deprecation")
    @Override
    public void installPackage(File apkFile) throws AndroidNotCompatibleException {
        super.installPackage(apkFile);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(Uri.fromFile(apkFile));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);

        // following extras only work when being installed as system-app
        // https://code.google.com/p/android/issues/detail?id=42253
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            // deprecated in Android 4.1
            intent.putExtra(Intent.EXTRA_ALLOW_REPLACE, true);
        }
        try {
            mActivity.startActivityForResult(intent, REQUEST_CODE_INSTALL);
        } catch (ActivityNotFoundException e) {
            throw new AndroidNotCompatibleException(e);
        }
    }

    @Override
    public void deletePackage(String packageName) throws AndroidNotCompatibleException {
        super.deletePackage(packageName);

        PackageInfo pkgInfo = null;
        try {
            pkgInfo = mPm.getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            // already checked in super class
        }

        Uri uri = Uri.fromParts("package", pkgInfo.packageName, null);
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        try {
            mActivity.startActivityForResult(intent, REQUEST_CODE_DELETE);
        } catch (ActivityNotFoundException e) {
            throw new AndroidNotCompatibleException(e);
        }
    }

    @Override
    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        /**
         * resultCode is always 0 on Android < 4.0. See
         * com.android.packageinstaller.PackageInstallerActivity: setResult is
         * never executed!
         */
        switch (requestCode) {
            case REQUEST_CODE_INSTALL:
                if (resultCode == Activity.RESULT_OK) {
                    mCallback.onSuccess(InstallerCallback.OPERATION_INSTALL, false);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // TODO
                    mCallback.onError(InstallerCallback.OPERATION_INSTALL, false, "canceled");
                } else {
                    mCallback.onError(InstallerCallback.OPERATION_INSTALL, false, "todo");
                }

                return true;
            case REQUEST_CODE_DELETE:
                if (resultCode == Activity.RESULT_OK) {
                    mCallback.onSuccess(InstallerCallback.OPERATION_DELETE, false);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // TODO
                    mCallback.onError(InstallerCallback.OPERATION_DELETE, false, "canceled");
                } else {
                    mCallback.onError(InstallerCallback.OPERATION_DELETE, false, "todo");
                }

                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean supportsUnattendedOperations() {
        return false;
    }

}
