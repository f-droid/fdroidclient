/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.fdroid.fdroid.privileged;

import org.fdroid.fdroid.privileged.IPrivilegedCallback;

interface IPrivilegedService {

    boolean hasPrivilegedPermissions();

    /**
     * - Docs based on PackageManager.installPackage()
     * - Asynchronous (oneway) IPC calls!
     *
     * Install a package. Since this may take a little while, the result will
     * be posted back to the given callback. An installation will fail if the
     * package named in the package file's manifest is already installed, or if there's no space
     * available on the device.
     *
     * @param packageURI The location of the package file to install.  This can be a 'file:' or a
     * 'content:' URI.
     * @param flags - possible values: {@link #INSTALL_FORWARD_LOCK},
     * {@link #INSTALL_REPLACE_EXISTING}, {@link #INSTALL_ALLOW_TEST}.
     * @param installerPackageName Optional package name of the application that is performing the
     * installation. This identifies which market the package came from.
     * @param callback An callback to get notified when the package installation is
     * complete.
     */
    oneway void installPackage(in Uri packageURI, in int flags, in String installerPackageName,
                        in IPrivilegedCallback callback);


    /**
     * - Docs based on PackageManager.deletePackage()
     * - Asynchronous (oneway) IPC calls!
     *
     * Attempts to delete a package.  Since this may take a little while, the result will
     * be posted back to the given observer.  A deletion will fail if the
     * named package cannot be found, or if the named package is a "system package".
     *
     * @param packageName The name of the package to delete
     * @param flags - possible values: {@link #DELETE_KEEP_DATA},
     * {@link #DELETE_ALL_USERS}.
     * @param callback An callback to get notified when the package deletion is
     * complete.
     */
    oneway void deletePackage(in String packageName, in int flags, in IPrivilegedCallback callback);

}