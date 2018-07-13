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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.privileged.install.InstallExtensionDialogActivity;

import java.io.File;

/**
 * Special Installer that is only useful to install the Privileged Extension apk
 * as a privileged app into the system partition of Android.  It is deprecated
 * because it cannot work on Android versions newer than {@code android-20} or so,
 * due to increased SELinux enforcement that restricts what even root can do.
 * <p/>
 * This is installer requires user interaction and thus install/uninstall directly
 * return PendingIntents.
 *
 * @see <a href="https://www.androidauthority.com/chainfire-rooting-android-lollipop-541458/">Chainfire talks Android Lollipop and the future of rooting</a>
 */
@Deprecated
public class ExtensionInstaller extends Installer {

    ExtensionInstaller(Context context, @NonNull Apk apk) {
        super(context, apk);
    }

    @Override
    protected void installPackageInternal(Uri localApkUri, Uri downloadUri) {
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
    protected void uninstallPackage() {
        Intent uninstallIntent = new Intent(context, InstallExtensionDialogActivity.class);
        uninstallIntent.setAction(InstallExtensionDialogActivity.ACTION_UNINSTALL);

        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                apk.packageName.hashCode(),
                uninstallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastUninstall(Installer.ACTION_UNINSTALL_USER_INTERACTION, uninstallPendingIntent);

        // don't use broadcasts for the rest of this special installer
        sendBroadcastUninstall(Installer.ACTION_UNINSTALL_COMPLETE);
    }

    @Override
    protected boolean isUnattended() {
        return false;
    }
}
