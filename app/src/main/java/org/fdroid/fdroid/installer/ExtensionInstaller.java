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

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.privileged.install.InstallExtensionDialogActivity;

import java.io.File;

/**
 * Special Installer that is only useful to install the Privileged Extension apk
 */
public class ExtensionInstaller extends Installer {

    private static final String TAG = "ExtensionInstaller";

    ExtensionInstaller(Context context) {
        super(context);
    }

    @Override
    protected void installPackage(Uri uri, Uri originatingUri, String packageName) {
        sendBroadcastInstall(uri, originatingUri, Installer.ACTION_INSTALL_STARTED);

        Uri sanitizedUri;
        try {
            sanitizedUri = Installer.prepareApkFile(mContext, uri, packageName);
        } catch (InstallFailedException e) {
            Log.e(TAG, "prepareApkFile failed", e);
            return;
        }

        // extension must be signed with the same public key as main F-Droid
        // NOTE: Disabled for debug builds to be able to use official extension from repo
        ApkSignatureVerifier signatureVerifier = new ApkSignatureVerifier(mContext);
        if (!BuildConfig.DEBUG && !signatureVerifier.hasFDroidSignature(new File(sanitizedUri.getPath()))) {
            throw new RuntimeException("APK signature of extension not correct!");
        }
        Intent installIntent;
        installIntent = new Intent(mContext, InstallExtensionDialogActivity.class);
        installIntent.setAction(InstallExtensionDialogActivity.ACTION_INSTALL);
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

        Intent uninstallIntent;
        uninstallIntent = new Intent(mContext, InstallExtensionDialogActivity.class);
        uninstallIntent.setAction(InstallExtensionDialogActivity.ACTION_UNINSTALL);

        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(
                mContext.getApplicationContext(),
                packageName.hashCode(),
                uninstallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastUninstall(packageName,
                Installer.ACTION_UNINSTALL_USER_INTERACTION, uninstallPendingIntent);
    }
}
