/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2016 Blue Jay Wireless
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
import android.provider.Settings;

import androidx.annotation.NonNull;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.net.DownloaderService;

/**
 * The default installer of F-Droid. It uses the normal Intents APIs of Android
 * to install apks. Its main inner workings are encapsulated in DefaultInstallerActivity.
 * <p/>
 * This is installer requires user interaction and thus install/uninstall directly
 * return PendingIntents.
 */
public class DefaultInstaller extends Installer {

    public static final String TAG = "DefaultInstaller";

    DefaultInstaller(Context context, @NonNull App app, @NonNull Apk apk) {
        super(context, app, apk);
    }

    @Override
    protected void installPackageInternal(Uri localApkUri, Uri canonicalUri) {
        // ask to enable unknown sources on old Android versions (needs to target at least 26 for this to work)
        if (Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT < 31 &&
                context.getApplicationInfo().targetSdkVersion >= 26) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(i);
            }
        }

        Intent installIntent = new Intent(context, DefaultInstallerActivity.class);
        installIntent.setAction(DefaultInstallerActivity.ACTION_INSTALL_PACKAGE);
        installIntent.putExtra(DownloaderService.EXTRA_CANONICAL_URL, canonicalUri.toString());
        installIntent.putExtra(Installer.EXTRA_APP, app);
        installIntent.putExtra(Installer.EXTRA_APK, apk);
        installIntent.setData(localApkUri);

        PendingIntent installPendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                localApkUri.hashCode(),
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_USER_INTERACTION,
                installPendingIntent);
    }

    @Override
    protected void uninstallPackage() {
        Intent uninstallIntent = new Intent(context, DefaultInstallerActivity.class);
        uninstallIntent.setAction(DefaultInstallerActivity.ACTION_UNINSTALL_PACKAGE);
        uninstallIntent.putExtra(Installer.EXTRA_APP, app);
        uninstallIntent.putExtra(Installer.EXTRA_APK, apk);
        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                apk.packageName.hashCode(),
                uninstallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        sendBroadcastUninstall(Installer.ACTION_UNINSTALL_USER_INTERACTION, uninstallPendingIntent);
    }

    @Override
    protected boolean isUnattended() {
        return false;
    }
}
