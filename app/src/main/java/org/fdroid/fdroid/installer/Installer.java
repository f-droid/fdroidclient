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
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PatternMatcher;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;
import org.fdroid.fdroid.privileged.views.InstallConfirmActivity;
import org.fdroid.fdroid.privileged.views.UninstallDialogActivity;

import java.io.IOException;

/**
 * Handles the actual install process.  Subclasses implement the details.
 */
@SuppressWarnings("LineLength")
public abstract class Installer {
    private static final String TAG = "Installer";

    final Context context;
    final Apk apk;

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".installer";

    public static final String ACTION_INSTALL_STARTED = "org.fdroid.fdroid.installer.Installer.action.INSTALL_STARTED";
    public static final String ACTION_INSTALL_COMPLETE = "org.fdroid.fdroid.installer.Installer.action.INSTALL_COMPLETE";
    public static final String ACTION_INSTALL_INTERRUPTED = "org.fdroid.fdroid.installer.Installer.action.INSTALL_INTERRUPTED";
    public static final String ACTION_INSTALL_USER_INTERACTION = "org.fdroid.fdroid.installer.Installer.action.INSTALL_USER_INTERACTION";

    public static final String ACTION_UNINSTALL_STARTED = "org.fdroid.fdroid.installer.Installer.action.UNINSTALL_STARTED";
    public static final String ACTION_UNINSTALL_COMPLETE = "org.fdroid.fdroid.installer.Installer.action.UNINSTALL_COMPLETE";
    public static final String ACTION_UNINSTALL_INTERRUPTED = "org.fdroid.fdroid.installer.Installer.action.UNINSTALL_INTERRUPTED";
    public static final String ACTION_UNINSTALL_USER_INTERACTION = "org.fdroid.fdroid.installer.Installer.action.UNINSTALL_USER_INTERACTION";

    /**
     * The URI where the APK was originally downloaded from. This is also used
     * as the unique ID representing this in the whole install process in
     * {@link InstallManagerService}, there is is generally known as the
     * "download URL" since it is the URL used to download the APK.
     *
     * @see Intent#EXTRA_ORIGINATING_URI
     */
    static final String EXTRA_DOWNLOAD_URI = "org.fdroid.fdroid.installer.Installer.extra.DOWNLOAD_URI";
    public static final String EXTRA_APK = "org.fdroid.fdroid.installer.Installer.extra.APK";
    public static final String EXTRA_USER_INTERACTION_PI = "org.fdroid.fdroid.installer.Installer.extra.USER_INTERACTION_PI";
    public static final String EXTRA_ERROR_MESSAGE = "org.fdroid.fdroid.net.installer.Installer.extra.ERROR_MESSAGE";

    /**
     * @param apk must be included so that all the phases of the install process
     *            can get all the data about the app, even after F-Droid was killed
     */
    Installer(Context context, @NonNull Apk apk) {
        this.context = context;
        this.apk = apk;
    }

    /**
     * Returns permission screen for given apk.
     *
     * @return Intent with Activity to show required permissions.
     * Returns null if Installer handles that on itself, e.g., with DefaultInstaller,
     * or if no new permissions have been introduced during an update
     */
    public Intent getPermissionScreen() {
        if (!isUnattended()) {
            return null;
        }

        int count = newPermissionCount();
        if (count == 0) {
            // no permission screen needed!
            return null;
        }
        Uri uri = ApkProvider.getApkFromAnyRepoUri(apk);
        Intent intent = new Intent(context, InstallConfirmActivity.class);
        intent.setData(uri);

        return intent;
    }

    /**
     * Return if this installation process has any new permissions that the user
     * should be aware of.  Starting in {@code android-23}, all new permissions
     * are requested when they are used, and the permissions prompt at time of
     * install is not used.  All permissions in a new install are considered new.
     *
     * @return the number of new permissions
     */
    private int newPermissionCount() {
        boolean supportsRuntimePermissions = apk.targetSdkVersion >= 23;
        if (supportsRuntimePermissions) {
            return 0;
        }

        AppDiff appDiff = new AppDiff(context, apk);
        AppSecurityPermissions perms = new AppSecurityPermissions(context, appDiff.apkPackageInfo);
        if (appDiff.installedApplicationInfo != null) {
            // update to an existing app
            return perms.getPermissionCount(AppSecurityPermissions.WHICH_NEW);
        }
        // new app install
        return perms.getPermissionCount(AppSecurityPermissions.WHICH_ALL);
    }

    /**
     * Returns an Intent to start a dialog wrapped in an activity
     * for uninstall confirmation.
     *
     * @return Intent with activity for uninstall confirmation
     * Returns null if Installer handles that on itself, e.g.,
     * with DefaultInstaller.
     */
    public Intent getUninstallScreen() {
        if (!isUnattended()) {
            return null;
        }

        PackageManager pm = context.getPackageManager();
        String installerPackageName = pm.getInstallerPackageName(apk.packageName);
        if (Build.VERSION.SDK_INT >= 24 &&
                ("com.android.packageinstaller".equals(installerPackageName)
                        || "com.google.android.packageinstaller".equals(installerPackageName))) {
            Utils.debugLog(TAG, "Falling back to default installer for uninstall");
            Intent intent = new Intent(context, DefaultInstallerActivity.class);
            intent.setAction(DefaultInstallerActivity.ACTION_UNINSTALL_PACKAGE);
            intent.putExtra(Installer.EXTRA_APK, apk);
            return intent;
        }

        Intent intent = new Intent(context, UninstallDialogActivity.class);
        intent.putExtra(Installer.EXTRA_APK, apk);

        return intent;
    }

    void sendBroadcastInstall(Uri downloadUri, String action, PendingIntent pendingIntent) {
        sendBroadcastInstall(context, downloadUri, action, apk, pendingIntent, null);
    }

    void sendBroadcastInstall(Uri downloadUri, String action) {
        sendBroadcastInstall(context, downloadUri, action, apk, null, null);
    }

    void sendBroadcastInstall(Uri downloadUri, String action, String errorMessage) {
        sendBroadcastInstall(context, downloadUri, action, apk, null, errorMessage);
    }

    static void sendBroadcastInstall(Context context,
                                     Uri downloadUri, String action, Apk apk,
                                     PendingIntent pendingIntent, String errorMessage) {
        Intent intent = new Intent(action);
        intent.setData(downloadUri);
        intent.putExtra(Installer.EXTRA_USER_INTERACTION_PI, pendingIntent);
        intent.putExtra(Installer.EXTRA_APK, apk);
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(Installer.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    void sendBroadcastUninstall(String action, String errorMessage) {
        sendBroadcastUninstall(action, null, errorMessage);
    }

    void sendBroadcastUninstall(String action) {
        sendBroadcastUninstall(action, null, null);
    }

    void sendBroadcastUninstall(String action, PendingIntent pendingIntent) {
        sendBroadcastUninstall(action, pendingIntent, null);
    }

    private void sendBroadcastUninstall(String action, PendingIntent pendingIntent, String errorMessage) {
        sendBroadcastUninstall(context, apk, action, pendingIntent, errorMessage);
    }

    static void sendBroadcastUninstall(Context context, Apk apk, String action) {
        sendBroadcastUninstall(context, apk, action, null, null);
    }

    private static void sendBroadcastUninstall(Context context, Apk apk, String action,
                                               PendingIntent pendingIntent, String errorMessage) {
        Uri uri = Uri.fromParts("package", apk.packageName, null);

        Intent intent = new Intent(action);
        intent.setData(uri); // for broadcast filtering
        intent.putExtra(Installer.EXTRA_APK, apk);
        intent.putExtra(Installer.EXTRA_USER_INTERACTION_PI, pendingIntent);
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(Installer.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Gets an {@link IntentFilter} for matching events from the install
     * process based on the original download URL as a {@link Uri}.
     */
    public static IntentFilter getInstallIntentFilter(Uri uri) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Installer.ACTION_INSTALL_STARTED);
        intentFilter.addAction(Installer.ACTION_INSTALL_COMPLETE);
        intentFilter.addAction(Installer.ACTION_INSTALL_INTERRUPTED);
        intentFilter.addAction(Installer.ACTION_INSTALL_USER_INTERACTION);
        intentFilter.addDataScheme(uri.getScheme());
        intentFilter.addDataAuthority(uri.getHost(), String.valueOf(uri.getPort()));
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

    /**
     * Install apk given the URI that points to the local APK file, and the
     * download URI to identify which session this belongs to.  This first
     * moves the APK file to private directory for the installation process
     * to read from.  Then the hash of the APK is checked against the
     * {@link Apk} instance provided when this {@code Installer} object was
     * instantiated.  The list of permissions in the APK file and the
     * {@code Apk} instance are compared, if they do not match, then the user
     * is prompted with the system installer dialog, which shows all the
     * permissions that the APK is requesting.
     *
     * @param localApkUri points to the local copy of the APK to be installed
     * @param downloadUri serves as the unique ID for all actions related to the
     *                    installation of that specific APK
     * @see InstallManagerService
     * @see <a href="https://issuetracker.google.com/issues/37091886">ACTION_INSTALL_PACKAGE Fails For Any Possible Uri</a>
     */
    public void installPackage(Uri localApkUri, Uri downloadUri) {
        Uri sanitizedUri;

        try {
            sanitizedUri = ApkFileProvider.getSafeUri(context, localApkUri, apk);
        } catch (IOException e) {
            Utils.debugLog(TAG, e.getMessage(), e);
            sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED, e.getMessage());
            return;
        }

        try {
            // verify that permissions of the apk file match the ones from the apk object
            ApkVerifier apkVerifier = new ApkVerifier(context, localApkUri, apk);
            apkVerifier.verifyApk();
        } catch (ApkVerifier.ApkVerificationException e) {
            Utils.debugLog(TAG, e.getMessage(), e);
            sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED, e.getMessage());
            return;
        } catch (ApkVerifier.ApkPermissionUnequalException e) {
            // if permissions of apk are not the ones listed in the repo
            // and an unattended installer is used, a wrong permission screen
            // has been shown, thus fallback to AOSP DefaultInstaller!
            if (isUnattended()) {
                Utils.debugLog(TAG, e.getMessage(), e);
                Utils.debugLog(TAG, "Falling back to AOSP DefaultInstaller!");
                DefaultInstaller defaultInstaller = new DefaultInstaller(context, apk);
                defaultInstaller.installPackageInternal(sanitizedUri, downloadUri);
                return;
            }
        }

        installPackageInternal(sanitizedUri, downloadUri);
    }

    protected abstract void installPackageInternal(Uri localApkUri, Uri downloadUri);

    /**
     * Uninstall app as defined by {@link Installer#apk} in
     * {@link Installer#Installer(Context, Apk)}
     */
    protected abstract void uninstallPackage();

    /**
     * This {@link Installer} instance is capable of "unattended" install and
     * uninstall activities, without the system enforcing a user prompt.
     */
    protected abstract boolean isUnattended();
}
