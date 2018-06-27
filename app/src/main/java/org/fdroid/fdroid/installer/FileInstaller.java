/*
 * Copyright (C) 2017 Chirayu Desai <chirayudesai1@gmail.com>
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
import org.fdroid.fdroid.data.Apk;

public class FileInstaller extends Installer {

    public FileInstaller(Context context, @NonNull Apk apk) {
        super(context, apk);
    }

    @Override
    public Intent getPermissionScreen() {
        return null;
    }

    @Override
    public Intent getUninstallScreen() {
        return null;
    }

    @Override
    public void installPackage(Uri localApkUri, Uri downloadUri) {
        installPackageInternal(localApkUri, downloadUri);
    }

    @Override
    protected void installPackageInternal(Uri localApkUri, Uri downloadUri) {
        Intent installIntent = new Intent(context, FileInstallerActivity.class);
        installIntent.setAction(FileInstallerActivity.ACTION_INSTALL_FILE);
        installIntent.putExtra(Installer.EXTRA_DOWNLOAD_URI, downloadUri);
        installIntent.putExtra(Installer.EXTRA_APK, apk);
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
    protected void uninstallPackage() {
        Intent uninstallIntent = new Intent(context, FileInstallerActivity.class);
        uninstallIntent.setAction(FileInstallerActivity.ACTION_UNINSTALL_FILE);
        uninstallIntent.putExtra(Installer.EXTRA_APK, apk);
        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                apk.packageName.hashCode(),
                uninstallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastUninstall(Installer.ACTION_UNINSTALL_USER_INTERACTION, uninstallPendingIntent);
    }

    @Override
    protected boolean isUnattended() {
        return false;
    }
}