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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.io.IOException;

/**
 * Helper methods for preparing APKs and arbitrary files for installation,
 * either locally or for sending via bluetooth.
 * <p/>
 * APK handling for installations:
 * <ol>
 * <li>APKs are downloaded into a cache directory that is either created on SD card
 * <i>"/Android/data/[app_package_name]/cache/apks"</i> (if card is mounted and app has
 * appropriate permission) or on device's file system depending incoming parameters</li>
 * <li>Before installation, the APK is copied into the private data directory of the F-Droid,
 * <i>"/data/data/[app_package_name]/files/install-$random.apk"</i> so that the install
 * process broken if the user clears the cache while it is running.</li>
 * <li>The hash of the file is checked against the expected hash from the repository</li>
 * <li>For {@link Build.VERSION_CODES#M < android-23}, a {@code file://} {@link Uri}
 * pointing to the {@link File} is returned, for {@link Build.VERSION_CODES#M >= android-23},
 * a {@code content://} {@code Uri} is returned using support lib's
 * {@link FileProvider}</li>
 * </ol>
 *
 * @see org.fdroid.fdroid.work.CleanCacheWorker
 */
public class ApkFileProvider extends FileProvider {

    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".installer.ApkFileProvider";

    public static Uri getSafeUri(Context context, PackageInfo packageInfo) throws IOException {
        SanitizedFile tempApkFile = ApkCache.copyInstalledApkToFiles(context, packageInfo);
        return getSafeUri(context, tempApkFile, true);
    }

    /**
     * Copies the APK into private data directory of F-Droid and returns a
     * {@code file://} or {@code content://} URI to be used for the
     * actual installation process.  Only APKs will ever use a {@code content://}
     * URI, any other file will always use a {@code file://} URI since F-Droid
     * itself handles their whole installation process.
     */
    static Uri getSafeUri(Context context, Uri localApkUri, Apk expectedApk)
            throws IOException {
        File apkFile = new File(localApkUri.getPath());
        SanitizedFile tempApkFile = ApkCache.copyApkFromCacheToFiles(context, apkFile, expectedApk);
        return getSafeUri(context, tempApkFile,
                Build.VERSION.SDK_INT >= 24 && expectedApk.isApk());

    }

    /**
     * Return a {@link Uri} for all install processes to install this package
     * from.  This supports APKs and all other supported files.  It also
     * supports all installation methods, e.g. default, privileged, etc.
     * It can return either a {@code content://} or {@code file://} URI.
     * <p>
     * APKs need to be world readable, so that the Android system installer
     * is able to read it.  Saving it into external storage to send it to the
     * installer have access is insecure, because apps with permission to write
     * to the external storage can overwrite the app between F-Droid asking for
     * it to be installed and the installer actually installing it.
     */
    private static Uri getSafeUri(Context context, SanitizedFile tempFile, boolean useContentUri) {
        if (useContentUri) {
            Uri apkUri = getUriForFile(context, AUTHORITY, tempFile);
            context.grantUriPermission(PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME,
                    apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.grantUriPermission("com.android.bluetooth", apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.grantUriPermission("com.mediatek.bluetooth", apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return apkUri;
        } else {
            tempFile.setReadable(true, false);
            return Uri.fromFile(tempFile);
        }
    }

}
