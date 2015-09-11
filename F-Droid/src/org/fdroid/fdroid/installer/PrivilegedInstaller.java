/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.privileged.IPrivilegedCallback;
import org.fdroid.fdroid.privileged.IPrivilegedService;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;
import org.fdroid.fdroid.privileged.views.InstallConfirmActivity;

import java.io.File;
import java.util.List;

/**
 * Installer based on using internal hidden APIs of the Android OS, which are
 * protected by the permissions
 * <ul>
 * <li>android.permission.INSTALL_PACKAGES</li>
 * <li>android.permission.DELETE_PACKAGES</li>
 * </ul>
 * <p/>
 * Both permissions are protected by systemOrSignature (in newer versions:
 * system|signature) and only granted on F-Droid's install in the following
 * cases:
 * <ul>
 * <li>On all Android versions if F-Droid is pre-deployed as a
 * system-application with the Rom</li>
 * <li>On Android < 4.4 also when moved into /system/app/</li>
 * <li>On Android >= 4.4 also when moved into /system/priv-app/</li>
 * </ul>
 * <p/>
 * Sources for Android 4.4 change:
 * https://groups.google.com/forum/#!msg/android-
 * security-discuss/r7uL_OEMU5c/LijNHvxeV80J
 * https://android.googlesource.com/platform
 * /frameworks/base/+/ccbf84f44c9e6a5ed3c08673614826bb237afc54
 */
public class PrivilegedInstaller extends Installer {

    private static final String TAG = "PrivilegedInstaller";

    private static final String PRIVILEGED_SERVICE_INTENT = "org.fdroid.fdroid.privileged.IPrivilegedService";
    public static final String PRIVILEGED_PACKAGE_NAME = "org.fdroid.fdroid.privileged";

    private Activity mActivity;

    public static final int REQUEST_CONFIRM_PERMS = 0;

    public PrivilegedInstaller(Activity activity, PackageManager pm,
                               InstallerCallback callback) throws AndroidNotCompatibleException {
        super(activity, pm, callback);
        this.mActivity = activity;
    }

    public static boolean isAvailable(Context context) {

        // check if installed
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(PRIVILEGED_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        // check if it has the privileged permissions granted
        final Object mutex = new Object();
        final Bundle returnBundle = new Bundle();
        ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                IPrivilegedService privService = IPrivilegedService.Stub.asInterface(service);

                try {
                    boolean hasPermissions = privService.hasPrivilegedPermissions();
                    returnBundle.putBoolean("has_permissions", hasPermissions);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException", e);
                }

                synchronized (mutex) {
                    mutex.notify();
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent serviceIntent = new Intent(PRIVILEGED_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_PACKAGE_NAME);
        context.getApplicationContext().bindService(serviceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE);

        synchronized (mutex) {
            try {
                mutex.wait(3000);
            } catch (InterruptedException e) {
                // don't care
            }
        }

        return (returnBundle.getBoolean("has_permission", false));
    }

    @Override
    protected void installPackageInternal(File apkFile) throws AndroidNotCompatibleException {
        Uri packageUri = Uri.fromFile(apkFile);
        int count = newPermissionCount(packageUri);
        if (count < 0) {
            mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                    InstallerCallback.ERROR_CODE_CANNOT_PARSE);
            return;
        }
        if (count > 0) {
            Intent intent = new Intent(mContext, InstallConfirmActivity.class);
            intent.setData(packageUri);
            mActivity.startActivityForResult(intent, REQUEST_CONFIRM_PERMS);
        } else {
            try {
                doInstallPackageInternal(packageUri);
            } catch (AndroidNotCompatibleException e) {
                mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                        InstallerCallback.ERROR_CODE_OTHER);
            }
        }
    }

    private int newPermissionCount(Uri packageUri) {
        AppDiff appDiff = new AppDiff(mContext.getPackageManager(), packageUri);
        if (appDiff.mPkgInfo == null) {
            // could not get diff because we couldn't parse the package
            return -1;
        }
        AppSecurityPermissions perms = new AppSecurityPermissions(mContext, appDiff.mPkgInfo);
        if (appDiff.mInstalledAppInfo != null) {
            // update to an existing app
            return perms.getPermissionCount(AppSecurityPermissions.WHICH_NEW);
        }
        // default: even if there aren't any permissions, we want to make the
        // user always confirm installing new apps
        return 1;
    }

    private void doInstallPackageInternal(final Uri packageURI) throws AndroidNotCompatibleException {
        ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                IPrivilegedService privService = IPrivilegedService.Stub.asInterface(service);

                IPrivilegedCallback callback = new IPrivilegedCallback() {
                    @Override
                    public void handleResult(String packageName, int returnCode) throws RemoteException {
                        // TODO: propagate other return codes?
                        if (returnCode == INSTALL_SUCCEEDED) {
                            Utils.debugLog(TAG, "Install succeeded");
                            mCallback.onSuccess(InstallerCallback.OPERATION_INSTALL);
                        } else {
                            Log.e(TAG, "Install failed with returnCode " + returnCode);
                            mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                                    InstallerCallback.ERROR_CODE_OTHER);
                        }
                    }

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }
                };

                try {
                    privService.installPackage(packageURI, INSTALL_REPLACE_EXISTING, null, callback);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException", e);
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };

        Intent serviceIntent = new Intent(PRIVILEGED_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_PACKAGE_NAME);
        mContext.getApplicationContext().bindService(serviceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void installPackageInternal(List<File> apkFiles)
            throws AndroidNotCompatibleException {
        // not used
    }

    @Override
    protected void deletePackageInternal(final String packageName)
            throws AndroidNotCompatibleException {
        ApplicationInfo appInfo;
        try {
            appInfo = mPm.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to get ApplicationInfo for uninstalling");
            return;
        }

        final boolean isSystem = ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        final boolean isUpdate = ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);

        if (isSystem && !isUpdate) {
            // Cannot remove system apps unless we're uninstalling updates
            mCallback.onError(InstallerCallback.OPERATION_DELETE,
                    InstallerCallback.ERROR_CODE_OTHER);
            return;
        }

        int messageId;
        if (isUpdate) {
            messageId = R.string.uninstall_update_confirm;
        } else {
            messageId = R.string.uninstall_confirm;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(appInfo.loadLabel(mPm));
        builder.setIcon(appInfo.loadIcon(mPm));
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            doDeletePackageInternal(packageName);
                        } catch (AndroidNotCompatibleException e) {
                            mCallback.onError(InstallerCallback.OPERATION_DELETE,
                                    InstallerCallback.ERROR_CODE_OTHER);
                        }
                    }
                });
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        mCallback.onError(InstallerCallback.OPERATION_DELETE,
                                InstallerCallback.ERROR_CODE_CANCELED);
                    }
                });
        builder.setMessage(messageId);
        builder.create().show();
    }

    private void doDeletePackageInternal(final String packageName)
            throws AndroidNotCompatibleException {
        ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                IPrivilegedService privService = IPrivilegedService.Stub.asInterface(service);

                IPrivilegedCallback callback = new IPrivilegedCallback() {
                    @Override
                    public void handleResult(String packageName, int returnCode) throws RemoteException {
                        // TODO: propagate other return codes?
                        if (returnCode == DELETE_SUCCEEDED) {
                            Utils.debugLog(TAG, "Delete succeeded");

                            mCallback.onSuccess(InstallerCallback.OPERATION_DELETE);
                        } else {
                            Log.e(TAG, "Delete failed with returnCode " + returnCode);
                            mCallback.onError(InstallerCallback.OPERATION_DELETE,
                                    InstallerCallback.ERROR_CODE_OTHER);
                        }
                    }

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }
                };

                try {
                    privService.deletePackage(packageName, 0, callback);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException", e);
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };

        Intent serviceIntent = new Intent(PRIVILEGED_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_PACKAGE_NAME);
        mContext.getApplicationContext().bindService(serviceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONFIRM_PERMS:
                if (resultCode == Activity.RESULT_OK) {
                    final Uri packageUri = data.getData();
                    try {
                        doInstallPackageInternal(packageUri);
                    } catch (AndroidNotCompatibleException e) {
                        mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                                InstallerCallback.ERROR_CODE_OTHER);
                    }
                } else if (resultCode == InstallConfirmActivity.RESULT_CANNOT_PARSE) {
                    mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                            InstallerCallback.ERROR_CODE_CANNOT_PARSE);
                } else { // Activity.RESULT_CANCELED
                    mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                            InstallerCallback.ERROR_CODE_CANCELED);
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean supportsUnattendedOperations() {
        return false;
    }

    public static final int INSTALL_REPLACE_EXISTING = 2;

    /**
     * Following return codes are copied from Android 5.1 source code
     */

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} on success.
     */
    public static final int INSTALL_SUCCEEDED = 1;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if the package is
     * already installed.
     */
    public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if the package archive
     * file is invalid.
     */
    public static final int INSTALL_FAILED_INVALID_APK = -2;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if the URI passed in
     * is invalid.
     */
    public static final int INSTALL_FAILED_INVALID_URI = -3;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if the package manager
     * service found that the device didn't have enough storage space to install the app.
     */
    public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if a
     * package is already installed with the same name.
     */
    public static final int INSTALL_FAILED_DUPLICATE_PACKAGE = -5;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the requested shared user does not exist.
     */
    public static final int INSTALL_FAILED_NO_SHARED_USER = -6;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * a previously installed package of the same name has a different signature
     * than the new package (and the old package's data was not removed).
     */
    public static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package is requested a shared user which is already installed on the
     * device and does not have matching signature.
     */
    public static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package uses a shared library that is not available.
     */
    public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package uses a shared library that is not available.
     */
    public static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package failed while optimizing and validating its dex files,
     * either because there was not enough storage or the validation failed.
     */
    public static final int INSTALL_FAILED_DEXOPT = -11;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package failed because the current SDK version is older than
     * that required by the package.
     */
    public static final int INSTALL_FAILED_OLDER_SDK = -12;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package failed because it contains a content provider with the
     * same authority as a provider already installed in the system.
     */
    public static final int INSTALL_FAILED_CONFLICTING_PROVIDER = -13;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package failed because the current SDK version is newer than
     * that required by the package.
     */
    public static final int INSTALL_FAILED_NEWER_SDK = -14;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package failed because it has specified that it is a test-only
     * package and the caller has not supplied the {@link #INSTALL_ALLOW_TEST}
     * flag.
     */
    public static final int INSTALL_FAILED_TEST_ONLY = -15;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the package being installed contains native code, but none that is
     * compatible with the device's CPU_ABI.
     */
    public static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package uses a feature that is not available.
     */
    public static final int INSTALL_FAILED_MISSING_FEATURE = -17;

    // ------ Errors related to sdcard
    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * a secure container mount point couldn't be accessed on external media.
     */
    public static final int INSTALL_FAILED_CONTAINER_ERROR = -18;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package couldn't be installed in the specified install
     * location.
     */
    public static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package couldn't be installed in the specified install
     * location because the media is not available.
     */
    public static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package couldn't be installed because the verification timed out.
     */
    public static final int INSTALL_FAILED_VERIFICATION_TIMEOUT = -21;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package couldn't be installed because the verification did not succeed.
     */
    public static final int INSTALL_FAILED_VERIFICATION_FAILURE = -22;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the package changed from what the calling program expected.
     */
    public static final int INSTALL_FAILED_PACKAGE_CHANGED = -23;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package is assigned a different UID than it previously held.
     */
    public static final int INSTALL_FAILED_UID_CHANGED = -24;

    /**
     * Installation return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)} if
     * the new package has an older version code than the currently installed package.
     */
    public static final int INSTALL_FAILED_VERSION_DOWNGRADE = -25;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser was given a path that is not a file, or does not end with the expected
     * '.apk' extension.
     */
    public static final int INSTALL_PARSE_FAILED_NOT_APK = -100;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser was unable to retrieve the AndroidManifest.xml file.
     */
    public static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser encountered an unexpected exception.
     */
    public static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser did not find any certificates in the .apk.
     */
    public static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser found inconsistent certificates on the files in the .apk.
     */
    public static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser encountered a CertificateEncodingException in one of the
     * files in the .apk.
     */
    public static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser encountered a bad or missing package name in the manifest.
     */
    public static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser encountered a bad shared user id name in the manifest.
     */
    public static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser encountered some structural problem in the manifest.
     */
    public static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;

    /**
     * Installation parse return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the parser did not find any actionable tags (instrumentation or application)
     * in the manifest.
     */
    public static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;

    /**
     * Installation failed return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the system failed to install the package because of system issues.
     */
    public static final int INSTALL_FAILED_INTERNAL_ERROR = -110;

    /**
     * Installation failed return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the system failed to install the package because the user is restricted from installing
     * apps.
     */
    public static final int INSTALL_FAILED_USER_RESTRICTED = -111;

    /**
     * Installation failed return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the system failed to install the package because it is attempting to define a
     * permission that is already defined by some existing package.
     * <p/>
     * <p>The package name of the app which has already defined the permission is passed to
     * a {@link PackageInstallObserver}, if any, as the {@link #EXTRA_EXISTING_PACKAGE}
     * string extra; and the name of the permission being redefined is passed in the
     * {@link #EXTRA_EXISTING_PERMISSION} string extra.
     */
    public static final int INSTALL_FAILED_DUPLICATE_PERMISSION = -112;

    /**
     * Installation failed return code: this is passed to the {@link IPackageInstallObserver} by
     * {@link #installPackage(android.net.Uri, IPackageInstallObserver, int)}
     * if the system failed to install the package because its packaged native code did not
     * match any of the ABIs supported by the system.
     */
    public static final int INSTALL_FAILED_NO_MATCHING_ABIS = -113;

    /**
     * Internal return code for NativeLibraryHelper methods to indicate that the package
     * being processed did not contain any native code. This is placed here only so that
     * it can belong to the same value space as the other install failure codes.
     */
    public static final int NO_NATIVE_LIBRARIES = -114;

    public static final int INSTALL_FAILED_ABORTED = -115;

    /**
     * Return code for when package deletion succeeds. This is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * succeeded in deleting the package.
     */
    public static final int DELETE_SUCCEEDED = 1;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * failed to delete the package for an unspecified reason.
     */
    public static final int DELETE_FAILED_INTERNAL_ERROR = -1;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * failed to delete the package because it is the active DevicePolicy
     * manager.
     */
    public static final int DELETE_FAILED_DEVICE_POLICY_MANAGER = -2;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * failed to delete the package since the user is restricted.
     */
    public static final int DELETE_FAILED_USER_RESTRICTED = -3;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * failed to delete the package because a profile
     * or device owner has marked the package as uninstallable.
     */
    public static final int DELETE_FAILED_OWNER_BLOCKED = -4;

    public static final int DELETE_FAILED_ABORTED = -5;

}
