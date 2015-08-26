/*
**
** Copyright 2007, The Android Open Source Project
** Copyright 2015 Dominik Schürmann <dominik@dominikschuermann.de>
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package org.fdroid.fdroid.privileged.views;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

public class AppDiff {

    public final PackageManager mPm;
    public final PackageInfo mPkgInfo;

    public ApplicationInfo mInstalledAppInfo = null;

    public AppDiff(PackageManager mPm, Uri mPackageURI) {
        this.mPm = mPm;

        final String pkgPath = mPackageURI.getPath();

        mPkgInfo = mPm.getPackageArchiveInfo(pkgPath, PackageManager.GET_PERMISSIONS);
        // We could not get the package info from the file. This means that we
        // could not parse the file, which can happen if the file cannot be
        // read or the minSdk is not satisfied.
        // Since we can't return an error from a constructor, we refuse to
        // continue. The caller must check if mPkgInfo is null to see if the
        // AppDiff was initialised correctly.
        if (mPkgInfo == null) {
            return;
        }
        mPkgInfo.applicationInfo.sourceDir = pkgPath;
        mPkgInfo.applicationInfo.publicSourceDir = pkgPath;

        init();
    }

    private void init() {
        String pkgName = mPkgInfo.packageName;
        // Check if there is already a package on the device with this name
        // but it has been renamed to something else.
        final String[] oldName = mPm.canonicalToCurrentPackageNames(new String[] { pkgName });
        if (oldName != null && oldName.length > 0 && oldName[0] != null) {
            pkgName = oldName[0];
            mPkgInfo.packageName = pkgName;
            mPkgInfo.applicationInfo.packageName = pkgName;
        }
        // Check if package is already installed
        try {
            // This is a little convoluted because we want to get all uninstalled
            // apps, but this may include apps with just data, and if it is just
            // data we still want to count it as "installed".
            mInstalledAppInfo = mPm.getApplicationInfo(pkgName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            if ((mInstalledAppInfo.flags&ApplicationInfo.FLAG_INSTALLED) == 0) {
                mInstalledAppInfo = null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            mInstalledAppInfo = null;
        }
    }
}
