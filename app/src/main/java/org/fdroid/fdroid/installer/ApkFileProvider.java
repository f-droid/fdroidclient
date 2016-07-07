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
import android.net.Uri;
import android.support.v4.content.FileProvider;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.io.IOException;

/**
 * This class has helper methods for preparing apks for installation.
 * <p/>
 * APK handling for installations:
 * 1. APKs are downloaded into a cache directory that is either created on SD card
 * <i>"/Android/data/[app_package_name]/cache/apks"</i> (if card is mounted and app has
 * appropriate permission) or on device's file system depending incoming parameters.
 * 2. Before installation, the APK is copied into the private data directory of the F-Droid,
 * <i>"/data/data/[app_package_name]/files/install-$random.apk"</i>.
 * 3. The hash of the file is checked against the expected hash from the repository
 * 4. For Android < 7, a file Uri pointing to the File is returned, for Android >= 7,
 * a content Uri is returned using support lib's FileProvider.
 */
public class ApkFileProvider extends FileProvider {

    private static final String AUTHORITY = "org.fdroid.fdroid.installer.ApkFileProvider";

    /**
     * Copies the APK into private data directory of F-Droid and returns a "file" or "content" Uri
     * to be used for installation.
     */
    public static Uri getSafeUri(Context context, Uri localApkUri, Apk expectedApk, boolean useContentUri)
            throws IOException {
        File apkFile = new File(localApkUri.getPath());

        SanitizedFile sanitizedApkFile =
                ApkCache.copyApkFromCacheToFiles(context, apkFile, expectedApk);

        if (useContentUri) {
            // return a content Uri using support libs FileProvider

            return getUriForFile(context, AUTHORITY, sanitizedApkFile);
        }

        // Need the apk to be world readable, so that the installer is able to read it.
        // Note that saving it into external storage for the purpose of letting the installer
        // have access is insecure, because apps with permission to write to the external
        // storage can overwrite the app between F-Droid asking for it to be installed and
        // the installer actually installing it.
        sanitizedApkFile.setReadable(true, false);

        return Uri.fromFile(sanitizedApkFile);
    }

}
