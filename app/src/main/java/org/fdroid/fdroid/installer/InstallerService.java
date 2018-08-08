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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.fdroid.fdroid.AppDetails2;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;

import java.io.File;
import java.io.FileFilter;
import java.util.Objects;

/**
 * This service handles the install process of apk files and
 * uninstall process of apps.
 * <p>
 * This service is based on an JobIntentService because:
 * <ul>
 * <li>no parallel installs/uninstalls should be allowed,
 * i.e., runs sequentially</li>
 * <li>no cancel operation is needed. Cancelling an installation
 * would be the same as starting uninstall afterwards</li>
 * </ul>
 * <p>
 * The download URL is only used as the unique ID that represents this
 * particular apk throughout the whole install process in
 * {@link InstallManagerService}.
 * <p>
 * This also handles deleting any associated OBB files when an app is
 * uninstalled, as per the
 * <a href="https://developer.android.com/google/play/expansion-files.html">
 * APK Expansion Files</a> spec.
 */
public class InstallerService extends JobIntentService {
    public static final String TAG = "InstallerService";

    private static final String ACTION_INSTALL = "org.fdroid.fdroid.installer.InstallerService.action.INSTALL";
    private static final String ACTION_UNINSTALL = "org.fdroid.fdroid.installer.InstallerService.action.UNINSTALL";

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);
        if (apk == null) {
            return;
        }
        Installer installer = InstallerFactory.create(this, apk);

        if (ACTION_INSTALL.equals(intent.getAction())) {
            Uri uri = intent.getData();
            Uri downloadUri = intent.getParcelableExtra(Installer.EXTRA_DOWNLOAD_URI);
            installer.installPackage(uri, downloadUri);
        } else if (ACTION_UNINSTALL.equals(intent.getAction())) {
            installer.uninstallPackage();
            new Thread() {
                @Override
                public void run() {
                    setPriority(MIN_PRIORITY);
                    File mainObbFile = apk.getMainObbFile();
                    if (mainObbFile == null) {
                        return;
                    }
                    File obbDir = mainObbFile.getParentFile();
                    if (obbDir == null) {
                        return;
                    }
                    FileFilter filter = new WildcardFileFilter("*.obb");
                    File[] obbFiles = obbDir.listFiles(filter);
                    if (obbFiles == null) {
                        return;
                    }
                    for (File f : obbFiles) {
                        Utils.debugLog(TAG, "Uninstalling OBB " + f);
                        FileUtils.deleteQuietly(f);
                    }
                }
            }.start();
        }
    }

    /**
     * Install an apk from {@link Uri}.
     * <p>
     * This does not include the same level of input validation as
     * {@link #uninstall(Context, Apk)} since this is called in one place where
     * the input has already been validated.
     *
     * @param context     this app's {@link Context}
     * @param localApkUri {@link Uri} pointing to (downloaded) local apk file
     * @param downloadUri {@link Uri} where the apk has been downloaded from
     * @param apk         apk object of app that should be installed
     * @see #uninstall(Context, Apk)
     */
    public static void install(Context context, Uri localApkUri, Uri downloadUri, Apk apk) {
        Installer.sendBroadcastInstall(context, downloadUri, Installer.ACTION_INSTALL_STARTED, apk,
                null, null);
        Intent intent = new Intent(context, InstallerService.class);
        intent.setAction(ACTION_INSTALL);
        intent.setData(localApkUri);
        intent.putExtra(Installer.EXTRA_DOWNLOAD_URI, downloadUri);
        intent.putExtra(Installer.EXTRA_APK, apk);
        enqueueWork(context, intent);
    }

    /**
     * Uninstall an app.  {@link Objects#requireNonNull(Object)} is used to
     * enforce the {@code @NonNull} requirement, since that annotation alone
     * is not enough to catch all possible nulls.
     * <p>
     * If you quickly cycle between installing an app and uninstalling it, then
     * {@link App#installedApk} will still be null when
     * {@link AppDetails2#startUninstall()} calls
     * this method.  It is better to crash earlier here, before the {@link Intent}
     * is sent with a null {@link Apk} instance since this service is set to
     * receive Sticky Intents.  That means they will automatically be resent
     * by the system until they successfully complete.  If an {@code Intent}
     * with a null {@code Apk} is sent, it'll crash.
     *
     * @param context this app's {@link Context}
     * @param apk     {@link Apk} instance of the app that will be uninstalled
     */
    public static void uninstall(Context context, @NonNull Apk apk) {
        if (Build.VERSION.SDK_INT >= 19) {
            Objects.requireNonNull(apk);
        }

        Installer.sendBroadcastUninstall(context, apk, Installer.ACTION_UNINSTALL_STARTED);

        Intent intent = new Intent(context, InstallerService.class);
        intent.setAction(ACTION_UNINSTALL);
        intent.putExtra(Installer.EXTRA_APK, apk);
        enqueueWork(context, intent);
    }

    private static void enqueueWork(Context context, @NonNull Intent intent) {
        enqueueWork(context, InstallerService.class, 0x872394, intent);
    }
}
