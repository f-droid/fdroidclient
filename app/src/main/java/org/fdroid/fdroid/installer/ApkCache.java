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

import com.nostra13.universalimageloader.utils.StorageUtils;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ApkCache {

    private static final String CACHE_DIR = "apks";

    /**
     * Copy the APK to the safe location inside of the protected area
     * of the app to prevent attacks based on other apps swapping the file
     * out during the install process. Most likely, apkFile was just downloaded,
     * so it should still be in the RAM disk cache.
     */
    public static SanitizedFile copyApkFromCacheToFiles(Context context, File apkFile, Apk expectedApk)
            throws IOException {
        SanitizedFile sanitizedApkFile = null;

        try {
            sanitizedApkFile = SanitizedFile.knownSanitized(
                    File.createTempFile("install-", ".apk", context.getFilesDir()));
            FileUtils.copyFile(apkFile, sanitizedApkFile);

            // verify copied file's hash with expected hash from Apk class
            if (!verifyApkFile(sanitizedApkFile, expectedApk.hash, expectedApk.hashType)) {
                FileUtils.deleteQuietly(apkFile);
                throw new IOException(apkFile + " failed to verify!");
            }

            return sanitizedApkFile;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
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
        }
    }

    /**
     * Checks the APK file against the provided hash, returning whether it is a match.
     */
    private static boolean verifyApkFile(File apkFile, String hash, String hashType)
            throws NoSuchAlgorithmException {
        if (!apkFile.exists()) {
            return false;
        }
        Hasher hasher = new Hasher(hashType, apkFile);
        return hasher.match(hash);
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
        try {
            return apkFile.length() == apkToCheck.size &&
                    verifyApkFile(apkFile, apkToCheck.hash, apkToCheck.hashType);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This location is only for caching, do not install directly from this location
     * because if the file is on the External Storage, any other app could swap out
     * the APK while the install was in process, allowing malware to install things.
     * Using {@link Installer#installPackage(Uri, Uri, Apk)}
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
