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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.fdroid.fdroid.Utils;

import java.io.File;

public class DefaultInstaller extends Installer {

    private static final String TAG = "DefaultInstaller";

    DefaultInstaller(Context context) {
        super(context);
    }

    @Override
    protected void installPackage(Uri uri, Uri originatingUri, String packageName) {
        sendBroadcastInstall(uri, originatingUri, Installer.ACTION_INSTALL_STARTED);

        Utils.debugLog(TAG, "DefaultInstaller uri: " + uri + " file: " + new File(uri.getPath()));

        Uri sanitizedUri;
        try {
            sanitizedUri = Installer.prepareApkFile(mContext, uri, packageName);
        } catch (Installer.InstallFailedException e) {
            Log.e(TAG, "prepareApkFile failed", e);
            sendBroadcastInstall(uri, originatingUri, Installer.ACTION_INSTALL_INTERRUPTED,
                    e.getMessage());
            return;
        }

        Intent installIntent = new Intent(mContext, DefaultInstallerActivity.class);
        installIntent.setAction(DefaultInstallerActivity.ACTION_INSTALL_PACKAGE);
        installIntent.putExtra(DefaultInstallerActivity.EXTRA_ORIGINATING_URI, originatingUri);
        installIntent.setData(sanitizedUri);

        PendingIntent installPendingIntent = PendingIntent.getActivity(
                mContext.getApplicationContext(),
                uri.hashCode(),
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastInstall(uri, originatingUri,
                Installer.ACTION_INSTALL_USER_INTERACTION, installPendingIntent);
    }

    @Override
    protected void uninstallPackage(String packageName) {
        sendBroadcastUninstall(packageName, Installer.ACTION_UNINSTALL_STARTED);

        Intent uninstallIntent = new Intent(mContext, DefaultInstallerActivity.class);
        uninstallIntent.setAction(DefaultInstallerActivity.ACTION_UNINSTALL_PACKAGE);
        uninstallIntent.putExtra(
                DefaultInstallerActivity.EXTRA_UNINSTALL_PACKAGE_NAME, packageName);
        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(
                mContext.getApplicationContext(),
                packageName.hashCode(),
                uninstallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastUninstall(packageName,
                Installer.ACTION_UNINSTALL_USER_INTERACTION, uninstallPendingIntent);
    }
}
