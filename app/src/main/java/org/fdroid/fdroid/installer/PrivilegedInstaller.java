/*
 * Copyright (C) 2016 Blue Jay Wireless
 * Copyright (C) 2014-2016 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Daniel Martí <mvdan@mvdan.cc>
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.privileged.IPrivilegedCallback;
import org.fdroid.fdroid.privileged.IPrivilegedService;

import java.util.HashMap;

/**
 * Installer that only works if the "F-Droid Privileged
 * Extension" is installed as a privileged app.
 * <p/>
 * "F-Droid Privileged Extension" provides a service that exposes
 * internal Android APIs for install/uninstall which are protected
 * by INSTALL_PACKAGES, DELETE_PACKAGES permissions.
 * Both permissions are protected by systemOrSignature (in newer versions:
 * system|signature) and cannot be used directly by F-Droid.
 * <p/>
 * Instead, this installer binds to the service of
 * "F-Droid Privileged Extension" and then executes the appropriate methods
 * inside the privileged context of the privileged extension.
 * <p/>
 * This installer makes unattended installs/uninstalls possible.
 * Thus no PendingIntents are returned.
 *
 * @see <a href="https://groups.google.com/forum/#!msg/android-security-discuss/r7uL_OEMU5c/LijNHvxeV80J">
 * Sources for Android 4.4 change</a>
 * @see <a href="https://android.googlesource.com/platform/frameworks/base/+/ccbf84f44">
 * Commit that restricted "signatureOrSystem" permissions</a>
 */
public class PrivilegedInstaller extends Installer {

    private static final String TAG = "PrivilegedInstaller";

    private static final String PRIVILEGED_EXTENSION_SERVICE_INTENT
            = "org.fdroid.fdroid.privileged.IPrivilegedService";
    public static final String PRIVILEGED_EXTENSION_PACKAGE_NAME = BuildConfig.PRIVILEGED_EXTENSION_PACKAGE_NAME;

    private static final int IS_EXTENSION_INSTALLED_NO = 0;
    public static final int IS_EXTENSION_INSTALLED_YES = 1;
    private static final int IS_EXTENSION_INSTALLED_SIGNATURE_PROBLEM = 2;

    // From AOSP source code
    private static final int ACTION_INSTALL_REPLACE_EXISTING = 2;

    /**
     * Following return codes are copied from AOSP 5.1 source code
     */
    private static final int INSTALL_SUCCEEDED = 1;
    private static final int INSTALL_FAILED_ALREADY_EXISTS = -1;
    private static final int INSTALL_FAILED_INVALID_APK = -2;
    private static final int INSTALL_FAILED_INVALID_URI = -3;
    private static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;
    private static final int INSTALL_FAILED_DUPLICATE_PACKAGE = -5;
    private static final int INSTALL_FAILED_NO_SHARED_USER = -6;
    private static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;
    private static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;
    private static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;
    private static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;
    private static final int INSTALL_FAILED_DEXOPT = -11;
    private static final int INSTALL_FAILED_OLDER_SDK = -12;
    private static final int INSTALL_FAILED_CONFLICTING_PROVIDER = -13;
    private static final int INSTALL_FAILED_NEWER_SDK = -14;
    private static final int INSTALL_FAILED_TEST_ONLY = -15;
    private static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;
    private static final int INSTALL_FAILED_MISSING_FEATURE = -17;
    private static final int INSTALL_FAILED_CONTAINER_ERROR = -18;
    private static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;
    private static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;
    private static final int INSTALL_FAILED_VERIFICATION_TIMEOUT = -21;
    private static final int INSTALL_FAILED_VERIFICATION_FAILURE = -22;
    private static final int INSTALL_FAILED_PACKAGE_CHANGED = -23;
    private static final int INSTALL_FAILED_UID_CHANGED = -24;
    private static final int INSTALL_FAILED_VERSION_DOWNGRADE = -25;
    private static final int INSTALL_PARSE_FAILED_NOT_APK = -100;
    private static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;
    private static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;
    private static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;
    private static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;
    private static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;
    private static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;
    private static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;
    private static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;
    private static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;
    private static final int INSTALL_FAILED_INTERNAL_ERROR = -110;
    private static final int INSTALL_FAILED_USER_RESTRICTED = -111;
    private static final int INSTALL_FAILED_DUPLICATE_PERMISSION = -112;
    private static final int INSTALL_FAILED_NO_MATCHING_ABIS = -113;

    private static final HashMap<Integer, String> INSTALL_RETURN_CODES;

    static {
        // Descriptions extracted from the source code comments in AOSP
        INSTALL_RETURN_CODES = new HashMap<>();
        INSTALL_RETURN_CODES.put(INSTALL_SUCCEEDED,
                "Success");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_ALREADY_EXISTS,
                "Package is already installed.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_INVALID_APK,
                "The package archive file is invalid.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_INVALID_URI,
                "The URI passed in is invalid.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_INSUFFICIENT_STORAGE,
                "The package manager service found that the device didn't have enough " +
                        "storage space to install the app.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_DUPLICATE_PACKAGE,
                "A package is already installed with the same name.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_NO_SHARED_USER,
                "The requested shared user does not exist.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_UPDATE_INCOMPATIBLE,
                "A previously installed package of the same name has a different signature than " +
                        "the new package (and the old package's data was not removed).");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_SHARED_USER_INCOMPATIBLE,
                "The new package has requested a shared user which is already installed on " +
                        "the device and does not have matching signature.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_MISSING_SHARED_LIBRARY,
                "The new package uses a shared library that is not available.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_REPLACE_COULDNT_DELETE,
                "Unknown"); // wrong comment in source
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_DEXOPT,
                "The package failed while optimizing and validating its dex files, either " +
                        "because there was not enough storage or the validation failed.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_OLDER_SDK,
                "The new package failed because the current SDK version is older than that " +
                        "required by the package.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_CONFLICTING_PROVIDER,
                "The new package failed because it contains a content provider with the same " +
                        "authority as a provider already installed in the system.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_NEWER_SDK,
                "The new package failed because the current SDK version is newer than that " +
                        "required by the package.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_TEST_ONLY,
                "The new package failed because it has specified that it is a test-only package " +
                        "and the caller has not supplied the {@link #INSTALL_ALLOW_TEST} flag.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_CPU_ABI_INCOMPATIBLE,
                "The package being installed contains native code, but none that is compatible " +
                        "with the device's CPU_ABI.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_MISSING_FEATURE,
                "The new package uses a feature that is not available.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_CONTAINER_ERROR,
                "A secure container mount point couldn't be accessed on external media.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_INVALID_INSTALL_LOCATION,
                "The new package couldn't be installed in the specified install location.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_MEDIA_UNAVAILABLE,
                "The new package couldn't be installed in the specified install location " +
                        "because the media is not available.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_VERIFICATION_TIMEOUT,
                "The new package couldn't be installed because the verification timed out.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_VERIFICATION_FAILURE,
                "The new package couldn't be installed because the verification did not succeed.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_PACKAGE_CHANGED,
                "The package changed from what the calling program expected.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_UID_CHANGED,
                "The new package is assigned a different UID than it previously held.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_VERSION_DOWNGRADE,
                "The new package has an older version code than the currently installed package.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_NOT_APK,
                "The parser was given a path that is not a file, or does not end with the " +
                        "expected '.apk' extension.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_BAD_MANIFEST,
                "the parser was unable to retrieve the AndroidManifest.xml file.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION,
                "The parser encountered an unexpected exception.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_NO_CERTIFICATES,
                "The parser did not find any certificates in the .apk.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES,
                "The parser found inconsistent certificates on the files in the .apk.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING,
                "The parser encountered a CertificateEncodingException in one of the files in " +
                        "the .apk.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME,
                "The parser encountered a bad or missing package name in the manifest.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID,
                "The parser encountered a bad shared user id name in the manifest.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_MANIFEST_MALFORMED,
                "The parser encountered some structural problem in the manifest.");
        INSTALL_RETURN_CODES.put(INSTALL_PARSE_FAILED_MANIFEST_EMPTY,
                "The parser did not find any actionable tags (instrumentation or application) " +
                        "in the manifest.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_INTERNAL_ERROR,
                "The system failed to install the package because of system issues.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_USER_RESTRICTED,
                "The system failed to install the package because the user is restricted from " +
                        "installing apps.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_DUPLICATE_PERMISSION,
                "The system failed to install the package because it is attempting to define a " +
                        "permission that is already defined by some existing package.");
        INSTALL_RETURN_CODES.put(INSTALL_FAILED_NO_MATCHING_ABIS,
                "The system failed to install the package because its packaged native code did " +
                        "not match any of the ABIs supported by the system.");
    }

    private static final int DELETE_SUCCEEDED = 1;
    private static final int DELETE_FAILED_INTERNAL_ERROR = -1;
    private static final int DELETE_FAILED_DEVICE_POLICY_MANAGER = -2;
    private static final int DELETE_FAILED_USER_RESTRICTED = -3;
    private static final int DELETE_FAILED_OWNER_BLOCKED = -4;

    private static final HashMap<Integer, String> UNINSTALL_RETURN_CODES;

    static {
        // Descriptions extracted from the source code comments in AOSP
        UNINSTALL_RETURN_CODES = new HashMap<>();
        UNINSTALL_RETURN_CODES.put(DELETE_SUCCEEDED,
                "Success");
        UNINSTALL_RETURN_CODES.put(DELETE_FAILED_INTERNAL_ERROR,
                " the system failed to delete the package for an unspecified reason.");
        UNINSTALL_RETURN_CODES.put(DELETE_FAILED_DEVICE_POLICY_MANAGER,
                "the system failed to delete the package because it is the active " +
                        "DevicePolicy manager.");
        UNINSTALL_RETURN_CODES.put(DELETE_FAILED_USER_RESTRICTED,
                "the system failed to delete the package since the user is restricted.");
        UNINSTALL_RETURN_CODES.put(DELETE_FAILED_OWNER_BLOCKED,
                "the system failed to delete the package because a profile or " +
                        "device owner has marked the package as uninstallable.");
    }

    PrivilegedInstaller(Context context, App app, @NonNull Apk apk) {
        super(context, app, apk);
    }

    private static boolean isExtensionInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(PRIVILEGED_EXTENSION_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return pm.getApplicationInfo(PRIVILEGED_EXTENSION_PACKAGE_NAME, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static int isExtensionInstalledCorrectly(Context context) {
        // check if installed
        if (!isExtensionInstalled(context)) {
            return IS_EXTENSION_INSTALLED_NO;
        }

        ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent serviceIntent = new Intent(PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_EXTENSION_PACKAGE_NAME);

        // try to connect to check for signature
        try {
            context.getApplicationContext().bindService(serviceIntent, serviceConnection,
                    Context.BIND_AUTO_CREATE);
        } catch (SecurityException e) {
            Log.e(TAG, "IS_EXTENSION_INSTALLED_SIGNATURE_PROBLEM", e);
            return IS_EXTENSION_INSTALLED_SIGNATURE_PROBLEM;
        }

        return IS_EXTENSION_INSTALLED_YES;
    }

    /**
     * Extension has privileged permissions and preference is enabled?
     */
    public static boolean isDefault(Context context) {
        return Preferences.get().isPrivilegedInstallerEnabled()
                && isExtensionInstalledCorrectly(context) == IS_EXTENSION_INSTALLED_YES;
    }

    @Override
    protected void installPackageInternal(final Uri localApkUri, final Uri canonicalUri) {
        ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                IPrivilegedService privService = IPrivilegedService.Stub.asInterface(service);

                IPrivilegedCallback callback = new IPrivilegedCallback.Stub() {
                    @Override
                    public void handleResult(String packageName, int returnCode) {
                        if (returnCode == INSTALL_SUCCEEDED) {
                            sendBroadcastInstall(canonicalUri, ACTION_INSTALL_COMPLETE);
                        } else {
                            sendBroadcastInstall(canonicalUri, ACTION_INSTALL_INTERRUPTED,
                                    "Error " + returnCode + ": "
                                            + INSTALL_RETURN_CODES.get(returnCode));
                        }
                    }
                };

                try {
                    boolean hasPermissions = privService.hasPrivilegedPermissions();
                    if (!hasPermissions) {
                        sendBroadcastInstall(canonicalUri, ACTION_INSTALL_INTERRUPTED,
                                context.getString(R.string.system_install_denied_permissions));
                        return;
                    }

                    privService.installPackage(localApkUri, ACTION_INSTALL_REPLACE_EXISTING,
                            PRIVILEGED_EXTENSION_PACKAGE_NAME, callback);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException", e);
                    sendBroadcastInstall(canonicalUri, ACTION_INSTALL_INTERRUPTED,
                            "connecting to privileged service failed");
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };

        Intent serviceIntent = new Intent(PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_EXTENSION_PACKAGE_NAME);
        context.getApplicationContext().bindService(serviceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void uninstallPackage() {
        ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                IPrivilegedService privService = IPrivilegedService.Stub.asInterface(service);

                IPrivilegedCallback callback = new IPrivilegedCallback.Stub() {
                    @Override
                    public void handleResult(String packageName, int returnCode) {
                        if (returnCode == DELETE_SUCCEEDED) {
                            sendBroadcastUninstall(ACTION_UNINSTALL_COMPLETE);
                        } else {
                            sendBroadcastUninstall(ACTION_UNINSTALL_INTERRUPTED,
                                    "Error " + returnCode + ": "
                                            + UNINSTALL_RETURN_CODES.get(returnCode));
                        }
                    }
                };

                try {
                    boolean hasPermissions = privService.hasPrivilegedPermissions();
                    if (!hasPermissions) {
                        sendBroadcastUninstall(ACTION_UNINSTALL_INTERRUPTED,
                                context.getString(R.string.system_install_denied_permissions));
                        return;
                    }

                    privService.deletePackage(apk.packageName, 0, callback);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException", e);
                    sendBroadcastUninstall(ACTION_UNINSTALL_INTERRUPTED,
                            "connecting to privileged service failed");
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };

        Intent serviceIntent = new Intent(PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_EXTENSION_PACKAGE_NAME);
        context.getApplicationContext().bindService(serviceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected boolean isUnattended() {
        return true;
    }
}
