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
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.PatternMatcher;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.AndroidXMLDecompress;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;
import org.fdroid.fdroid.privileged.views.InstallConfirmActivity;
import org.fdroid.fdroid.privileged.views.UninstallDialogActivity;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public abstract class Installer {
    final Context mContext;
    final PackageManager mPm;
    private LocalBroadcastManager localBroadcastManager;

    public static final String ACTION_INSTALL_STARTED = "org.fdroid.fdroid.installer.Installer.action.INSTALL_STARTED";
    public static final String ACTION_INSTALL_COMPLETE = "org.fdroid.fdroid.installer.Installer.action.INSTALL_COMPLETE";
    public static final String ACTION_INSTALL_INTERRUPTED = "org.fdroid.fdroid.installer.Installer.action.INSTALL_INTERRUPTED";
    public static final String ACTION_INSTALL_USER_INTERACTION = "org.fdroid.fdroid.installer.Installer.action.INSTALL_USER_INTERACTION";

    public static final String ACTION_UNINSTALL_STARTED = "org.fdroid.fdroid.installer.Installer.action.UNINSTALL_STARTED";
    public static final String ACTION_UNINSTALL_COMPLETE = "org.fdroid.fdroid.installer.Installer.action.UNINSTALL_COMPLETE";
    public static final String ACTION_UNINSTALL_INTERRUPTED = "org.fdroid.fdroid.installer.Installer.action.UNINSTALL_INTERRUPTED";
    public static final String ACTION_UNINSTALL_USER_INTERACTION = "org.fdroid.fdroid.installer.Installer.action.UNINSTALL_USER_INTERACTION";

    /**
     * Same as http://developer.android.com/reference/android/content/Intent.html#EXTRA_ORIGINATING_URI
     * In InstallManagerService often called urlString
     */
    public static final String EXTRA_ORIGINATING_URI = "org.fdroid.fdroid.installer.InstallerService.extra.ORIGINATING_URI";
    public static final String EXTRA_PACKAGE_NAME = "org.fdroid.fdroid.installer.InstallerService.extra.PACKAGE_NAME";
    public static final String EXTRA_USER_INTERACTION_PI = "org.fdroid.fdroid.installer.InstallerService.extra.USER_INTERACTION_PI";
    public static final String EXTRA_ERROR_MESSAGE = "org.fdroid.fdroid.net.Downloader.extra.ERROR_MESSAGE";

    public static class InstallFailedException extends Exception {

        private static final long serialVersionUID = -8343133906463328027L;

        public InstallFailedException(String message) {
            super(message);
        }

        public InstallFailedException(Throwable cause) {
            super(cause);
        }
    }

    Installer(Context context) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public static Uri prepareApkFile(Context context, Uri uri, String packageName)
            throws InstallFailedException {

        File apkFile = new File(uri.getPath());

        SanitizedFile sanitizedApkFile = null;
        try {
            Map<String, Object> attributes = AndroidXMLDecompress.getManifestHeaderAttributes(apkFile.getAbsolutePath());

            /* This isn't really needed, but might as well since we have the data already */
            if (attributes.containsKey("packageName") && !TextUtils.equals(packageName, (String) attributes.get("packageName"))) {
                throw new InstallFailedException(uri + " has packageName that clashes with " + packageName);
            }

            if (!attributes.containsKey("versionCode")) {
                throw new InstallFailedException(uri + " is missing versionCode!");
            }
            int versionCode = (Integer) attributes.get("versionCode");
            Apk apk = ApkProvider.Helper.find(context, packageName, versionCode, new String[]{
                    ApkProvider.DataColumns.HASH,
                    ApkProvider.DataColumns.HASH_TYPE,
            });
            /* Always copy the APK to the safe location inside of the protected area
             * of the app to prevent attacks based on other apps swapping the file
             * out during the install process. Most likely, apkFile was just downloaded,
             * so it should still be in the RAM disk cache */
            sanitizedApkFile = SanitizedFile.knownSanitized(File.createTempFile("install-", ".apk",
                    context.getFilesDir()));
            FileUtils.copyFile(apkFile, sanitizedApkFile);
            if (!verifyApkFile(sanitizedApkFile, apk.hash, apk.hashType)) {
                FileUtils.deleteQuietly(apkFile);
                throw new InstallFailedException(apkFile + " failed to verify!");
            }
            apkFile = null; // ensure this is not used now that its copied to apkToInstall

            // Need the apk to be world readable, so that the installer is able to read it.
            // Note that saving it into external storage for the purpose of letting the installer
            // have access is insecure, because apps with permission to write to the external
            // storage can overwrite the app between F-Droid asking for it to be installed and
            // the installer actually installing it.
            sanitizedApkFile.setReadable(true, false);

        } catch (NumberFormatException | IOException | NoSuchAlgorithmException e) {
            throw new InstallFailedException(e);
        } catch (ClassCastException e) {
            throw new InstallFailedException("F-Droid Privileged can only be updated using an activity!");
        } finally {
            // 20 minutes the start of the install process, delete the file
            final File apkToDelete = sanitizedApkFile;
            new Thread() {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
                    try {
                        Thread.sleep(1200000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.deleteQuietly(apkToDelete);
                    }
                }
            }.start();
        }

        return Uri.fromFile(sanitizedApkFile);
    }

    public Intent getPermissionScreen(Apk apk) {
        if (!isUnattended()) {
            return null;
        }

        int count = newPermissionCount(apk);
        if (count > 0) {
            Uri uri = ApkProvider.getContentUri(apk);
            Intent intent = new Intent(mContext, InstallConfirmActivity.class);
            intent.setData(uri);

            return intent;
        } else {
            // no permission screen needed!
            return null;
        }
    }

    private int newPermissionCount(Apk apk) {
        // TODO: requires targetSdk in Apk class/database
        //boolean supportsRuntimePermissions = mPkgInfo.applicationInfo.targetSdkVersion
        //        >= Build.VERSION_CODES.M;
        //if (supportsRuntimePermissions) {
        //    return 0;
        //}

        AppDiff appDiff = new AppDiff(mContext.getPackageManager(), apk);
        if (appDiff.mPkgInfo == null) {
            // could not get diff because we couldn't parse the package
            throw new RuntimeException("cannot parse!");
        }
        AppSecurityPermissions perms = new AppSecurityPermissions(mContext, appDiff.mPkgInfo);
        if (appDiff.mInstalledAppInfo != null) {
            // update to an existing app
            return perms.getPermissionCount(AppSecurityPermissions.WHICH_NEW);
        }
        // new app install
        return perms.getPermissionCount(AppSecurityPermissions.WHICH_ALL);
    }

    public Intent getUninstallScreen(String packageName) {
        if (!isUnattended()) {
            return null;
        }

        Intent intent = new Intent(mContext, UninstallDialogActivity.class);
        intent.putExtra(Installer.EXTRA_PACKAGE_NAME, packageName);

        return intent;
    }

    /**
     * Checks the APK file against the provided hash, returning whether it is a match.
     */
    public static boolean verifyApkFile(File apkFile, String hash, String hashType)
            throws NoSuchAlgorithmException {
        if (!apkFile.exists()) {
            return false;
        }
        Hasher hasher = new Hasher(hashType, apkFile);
        return hasher.match(hash);
    }

    public void sendBroadcastInstall(Uri uri, Uri originatingUri, String action,
                                     PendingIntent pendingIntent) {
        sendBroadcastInstall(uri, originatingUri, action, pendingIntent, null);
    }

    public void sendBroadcastInstall(Uri uri, Uri originatingUri, String action) {
        sendBroadcastInstall(uri, originatingUri, action, null, null);
    }

    public void sendBroadcastInstall(Uri uri, Uri originatingUri, String action, String errorMessage) {
        sendBroadcastInstall(uri, originatingUri, action, null, errorMessage);
    }

    public void sendBroadcastInstall(Uri uri, Uri originatingUri, String action,
                                     PendingIntent pendingIntent, String errorMessage) {
        Intent intent = new Intent(action);
        intent.setData(uri);
        intent.putExtra(Installer.EXTRA_ORIGINATING_URI, originatingUri);
        intent.putExtra(Installer.EXTRA_USER_INTERACTION_PI, pendingIntent);
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(Installer.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        localBroadcastManager.sendBroadcast(intent);
    }

    public void sendBroadcastUninstall(String packageName, String action, String errorMessage) {
        sendBroadcastUninstall(packageName, action, null, errorMessage);
    }

    public void sendBroadcastUninstall(String packageName, String action) {
        sendBroadcastUninstall(packageName, action, null, null);
    }

    public void sendBroadcastUninstall(String packageName, String action,
                                       PendingIntent pendingIntent) {
        sendBroadcastUninstall(packageName, action, pendingIntent, null);
    }

    public void sendBroadcastUninstall(String packageName, String action,
                                       PendingIntent pendingIntent, String errorMessage) {
        Uri uri = Uri.fromParts("package", packageName, null);

        Intent intent = new Intent(action);
        intent.setData(uri); // for broadcast filtering
        intent.putExtra(Installer.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(Installer.EXTRA_USER_INTERACTION_PI, pendingIntent);
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(Installer.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        localBroadcastManager.sendBroadcast(intent);
    }

    public static IntentFilter getInstallIntentFilter(Uri uri) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Installer.ACTION_INSTALL_STARTED);
        intentFilter.addAction(Installer.ACTION_INSTALL_COMPLETE);
        intentFilter.addAction(Installer.ACTION_INSTALL_INTERRUPTED);
        intentFilter.addAction(Installer.ACTION_INSTALL_USER_INTERACTION);
        intentFilter.addDataScheme(uri.getScheme());
        intentFilter.addDataPath(uri.getPath(), PatternMatcher.PATTERN_LITERAL);
        return intentFilter;
    }

    public static IntentFilter getUninstallIntentFilter(String packageName) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Installer.ACTION_UNINSTALL_STARTED);
        intentFilter.addAction(Installer.ACTION_UNINSTALL_COMPLETE);
        intentFilter.addAction(Installer.ACTION_UNINSTALL_INTERRUPTED);
        intentFilter.addAction(Installer.ACTION_UNINSTALL_USER_INTERACTION);
        intentFilter.addDataScheme("package");
        intentFilter.addDataPath(packageName, PatternMatcher.PATTERN_LITERAL);
        return intentFilter;
    }

    protected abstract void installPackage(Uri uri, Uri originatingUri, String packageName);

    protected abstract void uninstallPackage(String packageName);

    protected abstract boolean isUnattended();

}
