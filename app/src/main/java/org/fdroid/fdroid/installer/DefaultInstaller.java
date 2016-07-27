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
import android.os.Build;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;

import java.io.File;

/**
 * The default installer of F-Droid. It uses the normal Intents APIs of Android
 * to install apks. Its main inner workings are encapsulated in DefaultInstallerActivity.
 * <p/>
 * This is installer requires user interaction and thus install/uninstall directly
 * return PendingIntents.
 */
public class DefaultInstaller extends Installer {

    private static final String TAG = "DefaultInstaller";

    DefaultInstaller(Context context) {
        super(context);
    }

    @Override
    protected void installPackageInternal(Uri localApkUri, Uri downloadUri, Apk apk) {
        sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_STARTED);

        Utils.debugLog(TAG, "DefaultInstaller uri: " + localApkUri + " file: " + new File(localApkUri.getPath()));

        Intent installIntent = new Intent(context, DefaultInstallerActivity.class);
        installIntent.setAction(DefaultInstallerActivity.ACTION_INSTALL_PACKAGE);
        installIntent.putExtra(Installer.EXTRA_DOWNLOAD_URI, downloadUri);
        installIntent.setData(localApkUri);

        PendingIntent installPendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                localApkUri.hashCode(),
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_USER_INTERACTION,
                installPendingIntent);
    }

    @Override
    protected void uninstallPackage(String packageName) {
        sendBroadcastUninstall(packageName, Installer.ACTION_UNINSTALL_STARTED);

        Intent uninstallIntent = new Intent(context, DefaultInstallerActivity.class);
        uninstallIntent.setAction(DefaultInstallerActivity.ACTION_UNINSTALL_PACKAGE);
        uninstallIntent.putExtra(
                DefaultInstallerActivity.EXTRA_UNINSTALL_PACKAGE_NAME, packageName);
        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                packageName.hashCode(),
                uninstallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastUninstall(packageName,
                Installer.ACTION_UNINSTALL_USER_INTERACTION, uninstallPendingIntent);
    }

    @Override
    protected boolean isUnattended() {
        return false;
    }

    @Override
    protected boolean supportsContentUri() {
        // Android N only supports content Uris
        return Build.VERSION.SDK_INT >= 24;
    }
}
