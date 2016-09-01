/*
 * Copyright (C) 2016 Blue Jay Wireless
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Context;
import android.text.TextUtils;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;

public class InstallerFactory {

    private static final String TAG = "InstallerFactory";

    /**
     * Returns an instance of an appropriate installer.
     * Either DefaultInstaller, PrivilegedInstaller, or in the special
     * case to install the "F-Droid Privileged Extension" ExtensionInstaller.
     *
     * @param context current {@link Context}
     * @param apk     to be installed, always required.
     * @return instance of an Installer
     */
    public static Installer create(Context context, Apk apk) {
        if (apk == null || TextUtils.isEmpty(apk.packageName)) {
            throw new IllegalArgumentException("packageName must not be empty!");
        }

        Installer installer;
        if (apk.packageName.equals(PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME)) {
            // special case for "F-Droid Privileged Extension"
            installer = new ExtensionInstaller(context, apk);
        } else if (PrivilegedInstaller.isDefault(context)) {
            Utils.debugLog(TAG, "privileged extension correctly installed -> PrivilegedInstaller");
            installer = new PrivilegedInstaller(context, apk);
        } else {
            installer = new DefaultInstaller(context, apk);
        }

        return installer;
    }
}
