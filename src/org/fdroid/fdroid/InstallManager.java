/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.fdroid.fdroid;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

public class InstallManager {
    private Activity mActivity;
    private PackageManager mPm;

    private InstallCallback mInstallCallback;

    public static final String TAG = "FDroid";

    private static final int REQUEST_INSTALL = 0;
    private static final int REQUEST_UNINSTALL = 1;

    public interface InstallCallback {

        public static final int RETURN_SUCCESS = 1;
        public static final int RETURN_CANCEL = 0;

        public void onPackageInstalled(int returnCode, boolean unattended);

        public void onPackageDeleted(int returnCode, boolean unattended);
    }

    public InstallManager(Activity activity, PackageManager pm, InstallCallback installCallback) {
        super();
        this.mActivity = activity;
        this.mPm = pm;
        this.mInstallCallback = installCallback;
    }

    public void removeApk(String id) {
        PackageInfo pkginfo;
        try {
            pkginfo = mPm.getPackageInfo(id, 0);
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Couldn't find package " + id + " to uninstall.");
            return;
        }

        // try unattended delete using internal API. This only works when F-Droid is installed as
        // system app
        try {
            final InstallSystemManager systemInstall = new InstallSystemManager(mActivity,
                    mySystemCallback);
            systemInstall.deletePackage(pkginfo.packageName);

            return;
        } catch (Exception e) {
            Log.d(TAG, "Unattended delete failed, falling back to normal delete method...", e);
        }

        Uri uri = Uri.fromParts("package", pkginfo.packageName, null);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        mActivity.startActivityForResult(intent, REQUEST_UNINSTALL);
    }

    public void installApk(File file, String id) {
        // try unattended install using internal API. This only works when F-Droid is installed as
        // system app
        try {
            final InstallSystemManager systemInstall = new InstallSystemManager(mActivity,
                    mySystemCallback);
            systemInstall.installPackage(file);

            return;
        } catch (Exception e) {
            Log.d(TAG, "Unattended install failed, falling back to normal install method...", e);
        }

        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + file.getPath()),
                "application/vnd.android.package-archive");
        mActivity.startActivityForResult(intent, REQUEST_INSTALL);
    }

    private InstallSystemManager.InstallSystemCallback mySystemCallback = new InstallSystemManager.InstallSystemCallback() {

        @Override
        public void onPackageInstalled(String packageName, int returnCode) {
            if (returnCode == InstallSystemManager.INSTALL_SUCCEEDED) {
                Log.d(TAG, "Install succeeded");
                mInstallCallback.onPackageInstalled(InstallCallback.RETURN_SUCCESS,
                        true);
            } else {
                Log.d(TAG, "Install failed: " + returnCode);
                mInstallCallback.onPackageInstalled(InstallCallback.RETURN_CANCEL,
                        true);
            }

        }

        @Override
        public void onPackageDeleted(String packageName, int returnCode) {
            if (returnCode == InstallSystemManager.DELETE_SUCCEEDED) {
                Log.d(TAG, "Delete succeeded");
                mInstallCallback
                        .onPackageDeleted(InstallCallback.RETURN_SUCCESS, true);
            } else {
                Log.d(TAG, "Delete failed: " + returnCode);
                mInstallCallback.onPackageDeleted(InstallCallback.RETURN_CANCEL, true);
            }

        }

    };

    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_INSTALL:
            if (resultCode == Activity.RESULT_OK) {
                mInstallCallback.onPackageInstalled(InstallCallback.RETURN_SUCCESS, false);
            } else {
                mInstallCallback.onPackageInstalled(InstallCallback.RETURN_CANCEL, false);
            }
            break;
        case REQUEST_UNINSTALL:
            if (resultCode == Activity.RESULT_OK) {
                mInstallCallback.onPackageDeleted(InstallCallback.RETURN_SUCCESS, false);
            } else {
                mInstallCallback.onPackageDeleted(InstallCallback.RETURN_CANCEL, false);
            }
            break;
        }
    }

}
