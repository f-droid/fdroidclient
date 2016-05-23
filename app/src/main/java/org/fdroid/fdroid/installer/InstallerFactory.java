/*
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
import android.util.Log;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;

public class InstallerFactory {

    private static final String TAG = "InstallerFactory";

    public static Installer create(Context context) {
        Installer installer;

        if (isPrivilegedInstallerEnabled()) {
            if (PrivilegedInstaller.isExtensionInstalledCorrectly(context)
                    == PrivilegedInstaller.IS_EXTENSION_INSTALLED_YES) {
                Utils.debugLog(TAG, "privileged extension correctly installed -> PrivilegedInstaller");

                installer = new PrivilegedInstaller(context);
            } else {
                Log.e(TAG, "PrivilegedInstaller is enabled in prefs, but permissions are not granted!");
                // TODO: better error handling?

                // fallback to default installer
                installer = new DefaultInstaller(context);
            }
        } else {
            installer = new DefaultInstaller(context);
        }

        return installer;
    }

    /**
     * Extension has privileged permissions and preference is enabled?
     */
    private static boolean isPrivilegedInstallerEnabled() {
        return Preferences.get().isPrivilegedInstallerEnabled();
    }

}
