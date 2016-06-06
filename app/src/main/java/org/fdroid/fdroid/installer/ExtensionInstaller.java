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

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.privileged.install.InstallExtensionDialogActivity;

import java.io.File;

/**
 * Special Installer that is only useful to install the Privileged Extension apk
 * as a privileged app into the system partition of Android.
 * <p/>
 * This is installer requires user interaction and thus install/uninstall directly
 * return PendingIntents.
 */
public class ExtensionInstaller extends Installer {

    private static final String TAG = "ExtensionInstaller";

    ExtensionInstaller(Context context) {
        super(context);
    }

    @Override
    protected void installPackage(Uri localApkUri, Uri downloadUri, String packageName) {
        // extension must be signed with the same public key as main F-Droid
        // NOTE: Disabled for debug builds to be able to test official extension from repo
        ApkSignatureVerifier signatureVerifier = new ApkSignatureVerifier(context);
        if (!BuildConfig.DEBUG &&
                !signatureVerifier.hasFDroidSignature(new File(localApkUri.getPath()))) {
            sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED,
                    "APK signature of extension not correct!");
        }
        Intent installIntent = new Intent(context, InstallExtensionDialogActivity.class);
        installIntent.setAction(InstallExtensionDialogActivity.ACTION_INSTALL);
        installIntent.setData(localApkUri);

        PendingIntent installPendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                localApkUri.hashCode(),
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastInstall(downloadUri,
                Installer.ACTION_INSTALL_USER_INTERACTION, installPendingIntent);

        // don't use broadcasts for the rest of this special installer
        sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_COMPLETE);
    }

    @Override
    protected void uninstallPackage(String packageName) {
        sendBroadcastUninstall(packageName, Installer.ACTION_UNINSTALL_STARTED);

        Intent uninstallIntent = new Intent(context, InstallExtensionDialogActivity.class);
        uninstallIntent.setAction(InstallExtensionDialogActivity.ACTION_UNINSTALL);

        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                packageName.hashCode(),
                uninstallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastUninstall(packageName,
                Installer.ACTION_UNINSTALL_USER_INTERACTION, uninstallPendingIntent);

        // don't use broadcasts for the rest of this special installer
        sendBroadcastUninstall(packageName, Installer.ACTION_UNINSTALL_COMPLETE);
    }

    @Override
    protected boolean isUnattended() {
        return false;
    }
}
