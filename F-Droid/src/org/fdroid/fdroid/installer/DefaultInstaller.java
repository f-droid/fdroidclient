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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;
import java.util.List;

/**
 * For Android < 4: Default Installer using the public PackageManager API of
 * Android to install/delete packages. This starts a Activity from the Android
 * OS showing all permissions/changed permissions. The the user needs to
 * manually press an install button, this Installer cannot be used for
 * unattended installations.
 */
public class DefaultInstaller extends Installer {
    private final Activity mActivity;

    public DefaultInstaller(Activity activity, PackageManager pm, InstallerCallback callback)
            throws AndroidNotCompatibleException {
        super(activity, pm, callback);
        this.mActivity = activity;
    }

    private static final int REQUEST_CODE_INSTALL = 0;
    private static final int REQUEST_CODE_DELETE = 1;

    @Override
    protected void installPackageInternal(File apkFile) throws AndroidNotCompatibleException {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile),
                "application/vnd.android.package-archive");
        try {
            mActivity.startActivityForResult(intent, REQUEST_CODE_INSTALL);
        } catch (ActivityNotFoundException e) {
            throw new AndroidNotCompatibleException(e);
        }
    }

    @Override
    protected void installPackageInternal(List<File> apkFiles) throws AndroidNotCompatibleException {
        // not used
    }

    @Override
    protected void deletePackageInternal(String packageName) throws AndroidNotCompatibleException {
        try {
            PackageInfo pkgInfo = mPm.getPackageInfo(packageName, 0);

            Uri uri = Uri.fromParts("package", pkgInfo.packageName, null);
            Intent intent = new Intent(Intent.ACTION_DELETE, uri);
            try {
                mActivity.startActivityForResult(intent, REQUEST_CODE_DELETE);
            } catch (ActivityNotFoundException e) {
                throw new AndroidNotCompatibleException(e);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // already checked in super class
        }
    }

    @Override
    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        /**
         * resultCode is always 0 on Android < 4.0. See
         * com.android.packageinstaller.PackageInstallerActivity: setResult is
         * never executed on Androids before 4.0
         */
        switch (requestCode) {
        case REQUEST_CODE_INSTALL:
            mCallback.onSuccess(InstallerCallback.OPERATION_INSTALL);

            return true;
        case REQUEST_CODE_DELETE:
            mCallback.onSuccess(InstallerCallback.OPERATION_DELETE);

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
