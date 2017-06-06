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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import com.nostra13.universalimageloader.utils.StorageUtils;
import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.io.IOException;

public class ApkCache {

    private static final String CACHE_DIR = "apks";

    /**
     * Same as {@link #copyApkFromCacheToFiles(Context, File, Apk)}, except it does not need to
     * verify the hash after copying. This is because we are copying from an installed apk, which
     * other apps do not have permission to modify.
     */
    public static SanitizedFile copyInstalledApkToFiles(Context context, PackageInfo packageInfo)
            throws IOException {
        ApplicationInfo appInfo = packageInfo.applicationInfo;
        CharSequence name = context.getPackageManager().getApplicationLabel(appInfo);
        String apkFileName = name + "-" + packageInfo.versionName + ".apk";
        return copyApkToFiles(context, new File(appInfo.publicSourceDir), apkFileName, false, null, null);
    }

    /**
     * Copy the APK to the safe location inside of the protected area
     * of the app to prevent attacks based on other apps swapping the file
     * out during the install process. Most likely, apkFile was just downloaded,
     * so it should still be in the RAM disk cache.
     */
    public static SanitizedFile copyApkFromCacheToFiles(Context context, File apkFile, Apk expectedApk)
            throws IOException {
        App app = AppProvider.Helper.findHighestPriorityMetadata(context.getContentResolver(),
                expectedApk.packageName);
        String name = app == null ? expectedApk.packageName : app.name;
        String apkFileName = name + "-" + expectedApk.versionName + ".apk";
        return copyApkToFiles(context, apkFile, apkFileName, true, expectedApk.hash, expectedApk.hashType);
    }

    /**
     * Copy an APK from {@param apkFile} to our internal files directory for 20 minutes.
     *
     * @param verifyHash If the file was just downloaded, then you should mark this as true and
     *                   request the file to be verified once it has finished copying. Otherwise,
     *                   if the app was installed from part of the system where it can't be tampered
     *                   with (e.g. installed apks on disk) then
     */
    private static SanitizedFile copyApkToFiles(Context context, File apkFile, String destinationName,
                                                boolean verifyHash, String hash, String hashType)
            throws IOException {
        SanitizedFile sanitizedApkFile = new SanitizedFile(context.getFilesDir(), destinationName);

        // Don't think this is necessary, but the docs for FileUtils#copyFile() are not clear
        // on whether it overwrites destination files (pretty confident it does, as per the docs
        // in FileUtils#copyFileToDirectory() - which delegates to copyFile()).
        if (sanitizedApkFile.exists()) {
            sanitizedApkFile.delete();
        }

        FileUtils.copyFile(apkFile, sanitizedApkFile);

        // verify copied file's hash with expected hash from Apk class
        if (verifyHash && !Hasher.isFileMatchingHash(sanitizedApkFile, hash, hashType)) {
            FileUtils.deleteQuietly(apkFile);
            throw new IOException(apkFile + " failed to verify!");
        }

        // 20 minutes the start of the install process, delete the file
        final File apkToDelete = sanitizedApkFile;
        new Thread() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
                try {
                    Thread.sleep(1200000);
                } catch (InterruptedException ignored) {
                } finally {
                    FileUtils.deleteQuietly(apkToDelete);
                }
            }
        }.start();

        return sanitizedApkFile;
    }

    /**
     * Get the full path for where an APK URL will be downloaded into.
     */
    public static SanitizedFile getApkDownloadPath(Context context, Uri uri) {
        File dir = new File(getApkCacheDir(context), uri.getHost() + "-" + uri.getPort());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new SanitizedFile(dir, uri.getLastPathSegment());
    }

    /**
     * Verifies the size of the file on disk matches, and then hashes the file to compare with what
     * we received from the signed repo (i.e. {@link Apk#hash} and {@link Apk#hashType}).
     * Bails out if the file sizes don't match to prevent having to do the work of hashing the file.
     */
    public static boolean apkIsCached(File apkFile, Apk apkToCheck) {
        return apkFile.length() == apkToCheck.size &&
                Hasher.isFileMatchingHash(apkFile, apkToCheck.hash, apkToCheck.hashType);
    }

    /**
     * This location is only for caching, do not install directly from this location
     * because if the file is on the External Storage, any other app could swap out
     * the APK while the install was in process, allowing malware to install things.
     * Using {@link Installer#installPackage(Uri, Uri)}
     * is fine since that does the right thing.
     */
    public static File getApkCacheDir(Context context) {
        File apkCacheDir = new File(StorageUtils.getCacheDirectory(context, true), CACHE_DIR);
        if (apkCacheDir.isFile()) {
            apkCacheDir.delete();
        }
        if (!apkCacheDir.exists()) {
            apkCacheDir.mkdir();
        }
        return apkCacheDir;
    }
}
