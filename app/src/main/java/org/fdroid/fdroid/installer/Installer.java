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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
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
    final App app;
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

    public static final String EXTRA_APP = "org.fdroid.fdroid.installer.Installer.extra.APP";
    public static final String EXTRA_APK = "org.fdroid.fdroid.installer.Installer.extra.APK";
    public static final String EXTRA_USER_INTERACTION_PI = "org.fdroid.fdroid.installer.Installer.extra.USER_INTERACTION_PI";
    public static final String EXTRA_ERROR_MESSAGE = "org.fdroid.fdroid.net.installer.Installer.extra.ERROR_MESSAGE";

    /**
     * @param apk must be included so that all the phases of the install process
     *            can get all the data about the app, even after F-Droid was killed
     */
    Installer(Context context, @NonNull App app, @NonNull Apk apk) {
        this.context = context;
        this.app = app;
        this.apk = apk;
    }

    /**
     * Returns permission screen for given apk.
     *
     * @return Intent with AppCompatActivity to show required permissions.
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
        Intent intent = new Intent(context, InstallConfirmActivity.class);
        intent.putExtra(Installer.EXTRA_APP, app);
        intent.putExtra(Installer.EXTRA_APK, apk);

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
        String installerPackageName = null;
        try {
            installerPackageName = pm.getInstallerPackageName(apk.packageName);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "App not installed: " + apk.packageName, e);
        }
        if (Build.VERSION.SDK_INT >= 24 &&
                ("com.android.packageinstaller".equals(installerPackageName)
                        || "com.google.android.packageinstaller".equals(installerPackageName))) {
            Utils.debugLog(TAG, "Falling back to default installer for uninstall");
            Intent intent = new Intent(context, DefaultInstallerActivity.class);
            intent.setAction(DefaultInstallerActivity.ACTION_UNINSTALL_PACKAGE);
            intent.putExtra(Installer.EXTRA_APP, app);
            intent.putExtra(Installer.EXTRA_APK, apk);
            return intent;
        }

        Intent intent = new Intent(context, UninstallDialogActivity.class);
        intent.putExtra(Installer.EXTRA_APP, app);
        intent.putExtra(Installer.EXTRA_APK, apk);

        return intent;
    }

    void sendBroadcastInstall(Uri canonicalUri, String action, PendingIntent pendingIntent) {
        sendBroadcastInstall(context, canonicalUri, action, app, apk, pendingIntent, null);
    }

    void sendBroadcastInstall(Uri canonicalUri, String action) {
        sendBroadcastInstall(context, canonicalUri, action, app, apk, null, null);
    }

    void sendBroadcastInstall(Uri canonicalUri, String action, String errorMessage) {
        sendBroadcastInstall(context, canonicalUri, action, app, apk, null, errorMessage);
    }

    static void sendBroadcastInstall(Context context,
                                     Uri canonicalUri, String action, App app, Apk apk,
                                     PendingIntent pendingIntent, String errorMessage) {
        Intent intent = new Intent(action);
        intent.setData(canonicalUri);
        intent.putExtra(Installer.EXTRA_USER_INTERACTION_PI, pendingIntent);
        intent.putExtra(Installer.EXTRA_APP, app);
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
        sendBroadcastUninstall(context, app, apk, action, pendingIntent, errorMessage);
    }

    static void sendBroadcastUninstall(Context context, App app, Apk apk, String action) {
        sendBroadcastUninstall(context, app, apk, action, null, null);
    }

    static void sendBroadcastUninstall(Context context, App app, Apk apk, String action,
                                       PendingIntent pendingIntent, String errorMessage) {
        Uri uri = Uri.fromParts("package", apk.packageName, null);

        Intent intent = new Intent(action);
        intent.setData(uri); // for broadcast filtering
        intent.putExtra(Installer.EXTRA_APP, app);
        intent.putExtra(Installer.EXTRA_APK, apk);
        intent.putExtra(Installer.EXTRA_USER_INTERACTION_PI, pendingIntent);
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(Installer.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Gets an {@link IntentFilter} for matching events from the install
     * process based on {@code canonicalUri}, which is the global unique
     * ID for a package going through the install process.
     *
     * @see InstallManagerService for more about {@code canonicalUri}
     */
    static IntentFilter getInstallIntentFilter(Uri canonicalUri) {
        IntentFilter intentFilter = getInstallInteractionIntentFilter(canonicalUri);
        intentFilter.addAction(Installer.ACTION_INSTALL_STARTED);
        intentFilter.addAction(Installer.ACTION_INSTALL_COMPLETE);
        intentFilter.addAction(Installer.ACTION_INSTALL_INTERRUPTED);
        return intentFilter;
    }

    /**
     * Gets an {@link IntentFilter} for user interaction needed events from the install
     * process based on {@code canonicalUri}, which is the global unique
     * ID for a package going through the install process.
     *
     * @see InstallManagerService for more about {@code canonicalUri}
     */
    public static IntentFilter getInstallInteractionIntentFilter(Uri canonicalUri) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Installer.ACTION_INSTALL_USER_INTERACTION);
        intentFilter.addDataScheme(canonicalUri.getScheme());
        intentFilter.addDataAuthority(canonicalUri.getHost(), String.valueOf(canonicalUri.getPort()));
        intentFilter.addDataPath(canonicalUri.getPath(), PatternMatcher.PATTERN_LITERAL);
        return intentFilter;
    }

    /**
     * Gets an {@link IntentFilter} for matching events from the install
     * process based on {@code canonicalUrl}, which is the global unique
     * ID for a package going through the install process.
     *
     * @see InstallManagerService for more about {@code canonicalUrl}
     */
    public static IntentFilter getInstallIntentFilter(String canonicalUrl) {
        return getInstallIntentFilter(Uri.parse(canonicalUrl));
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
     * @param localApkUri  points to the local copy of the APK to be installed
     * @param canonicalUri serves as the unique ID for all actions related to the
     *                     installation of that specific APK
     * @see InstallManagerService
     * @see <a href="https://issuetracker.google.com/issues/37091886">ACTION_INSTALL_PACKAGE Fails For Any Possible Uri</a>
     */
    public void installPackage(Uri localApkUri, Uri canonicalUri) {
        Uri sanitizedUri;

        try {
            sanitizedUri = ApkFileProvider.getSafeUri(context, localApkUri, apk);
        } catch (IOException e) {
            Utils.debugLog(TAG, e.getMessage(), e);
            sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED, e.getMessage());
            return;
        }

        try {
            // verify that permissions of the apk file match the ones from the apk object
            ApkVerifier apkVerifier = new ApkVerifier(context, localApkUri, apk);
            apkVerifier.verifyApk();
        } catch (ApkVerifier.ApkVerificationException e) {
            Utils.debugLog(TAG, e.getMessage(), e);
            sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED, e.getMessage());
            return;
        } catch (ApkVerifier.ApkPermissionUnequalException e) {
            // if permissions of apk are not the ones listed in the repo
            // and an unattended installer is used, a wrong permission screen
            // has been shown, thus fallback to AOSP DefaultInstaller!
            if (isUnattended()) {
                Log.e(TAG, e.getMessage(), e);
                Log.e(TAG, "Falling back to AOSP DefaultInstaller!");
                DefaultInstaller defaultInstaller = new DefaultInstaller(context, app, apk);
                defaultInstaller.installPackageInternal(sanitizedUri, canonicalUri);
                return;
            }
        }

        installPackageInternal(sanitizedUri, canonicalUri);
    }

    protected abstract void installPackageInternal(Uri localApkUri, Uri canonicalUri);

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
