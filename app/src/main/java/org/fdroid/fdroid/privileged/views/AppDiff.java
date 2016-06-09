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

import org.fdroid.fdroid.data.Apk;

public class AppDiff {

    private final PackageManager pm;
    public final PackageInfo pkgInfo;
    public ApplicationInfo installedAppInfo;

    /**
     * Constructor based on F-Droids Apk object
     */
    public AppDiff(PackageManager pm, Apk apk) {
        this.pm = pm;

        pkgInfo = new PackageInfo();
        pkgInfo.packageName = apk.packageName;
        pkgInfo.applicationInfo = new ApplicationInfo();
        pkgInfo.requestedPermissions = apk.getFullPermissionsArray();

        init();
    }

    private void init() {
        String pkgName = pkgInfo.packageName;
        // Check if there is already a package on the device with this name
        // but it has been renamed to something else.
        final String[] oldName = pm.canonicalToCurrentPackageNames(new String[]{pkgName});
        if (oldName != null && oldName.length > 0 && oldName[0] != null) {
            pkgName = oldName[0];
            pkgInfo.packageName = pkgName;
            pkgInfo.applicationInfo.packageName = pkgName;
        }
        // Check if package is already installed
        try {
            // This is a little convoluted because we want to get all uninstalled
            // apps, but this may include apps with just data, and if it is just
            // data we still want to count it as "installed".
            //noinspection WrongConstant (lint is actually wrong here!)
            installedAppInfo = pm.getApplicationInfo(pkgName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            if ((installedAppInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
                installedAppInfo = null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            installedAppInfo = null;
        }
    }
}
