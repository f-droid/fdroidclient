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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.fdroid.fdroid.data.Apk;

/**
 * This service handles the install process of apk files and
 * uninstall process of apps.
 * <p/>
 * This service is based on an IntentService because:
 * - no parallel installs/uninstalls should be allowed,
 * i.e., runs sequentially
 * - no cancel operation is needed. Cancelling an installation
 * would be the same as starting uninstall afterwards
 * <p/>
 * The download URL is only used as the unique ID that represents this
 * particular apk throughout the whole install process in
 * {@link InstallManagerService}.
 */
public class InstallerService extends IntentService {

    private static final String ACTION_INSTALL = "org.fdroid.fdroid.installer.InstallerService.action.INSTALL";
    private static final String ACTION_UNINSTALL = "org.fdroid.fdroid.installer.InstallerService.action.UNINSTALL";

    public InstallerService() {
        super("InstallerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);

        Installer installer = InstallerFactory.create(this, apk);

        if (ACTION_INSTALL.equals(intent.getAction())) {
            Uri uri = intent.getData();
            Uri downloadUri = intent.getParcelableExtra(Installer.EXTRA_DOWNLOAD_URI);
            installer.installPackage(uri, downloadUri, apk);
        } else if (ACTION_UNINSTALL.equals(intent.getAction())) {
            String packageName = intent.getStringExtra(Installer.EXTRA_PACKAGE_NAME);
            installer.uninstallPackage(packageName);
        }
    }

    /**
     * Install an apk from {@link Uri}
     *
     * @param context     this app's {@link Context}
     * @param localApkUri {@link Uri} pointing to (downloaded) local apk file
     * @param downloadUri {@link Uri} where the apk has been downloaded from
     * @param apk         apk object of app that should be installed
     */
    public static void install(Context context, Uri localApkUri, Uri downloadUri, Apk apk) {
        Intent intent = new Intent(context, InstallerService.class);
        intent.setAction(ACTION_INSTALL);
        intent.setData(localApkUri);
        intent.putExtra(Installer.EXTRA_DOWNLOAD_URI, downloadUri);
        intent.putExtra(Installer.EXTRA_APK, apk);
        context.startService(intent);
    }

    /**
     * Uninstall an app
     *
     * @param context     this app's {@link Context}
     * @param packageName package name of the app that will be uninstalled
     */
    public static void uninstall(Context context, String packageName) {
        Intent intent = new Intent(context, InstallerService.class);
        intent.setAction(ACTION_UNINSTALL);
        intent.putExtra(Installer.EXTRA_PACKAGE_NAME, packageName);
        context.startService(intent);
    }

}
