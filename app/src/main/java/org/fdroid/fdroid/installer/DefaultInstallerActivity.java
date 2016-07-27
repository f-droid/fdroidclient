/*
 * Copyright (C) 2014-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import org.fdroid.fdroid.R;

/**
 * A transparent activity as a wrapper around Android's PackageInstaller Intents
 */
public class DefaultInstallerActivity extends FragmentActivity {
    private static final String TAG = "AndroidInstallerAct";

    static final String ACTION_INSTALL_PACKAGE = "org.fdroid.fdroid.installer.DefaultInstaller.action.INSTALL_PACKAGE";
    static final String ACTION_UNINSTALL_PACKAGE = "org.fdroid.fdroid.installer.DefaultInstaller.action.UNINSTALL_PACKAGE";

    static final String EXTRA_UNINSTALL_PACKAGE_NAME = "org.fdroid.fdroid.installer.DefaultInstaller.extra.UNINSTALL_PACKAGE_NAME";

    private static final int REQUEST_CODE_INSTALL = 0;
    private static final int REQUEST_CODE_UNINSTALL = 1;

    private Uri downloadUri;
    private String uninstallPackageName;

    // for the broadcasts
    private DefaultInstaller installer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        installer = new DefaultInstaller(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (ACTION_INSTALL_PACKAGE.equals(action)) {
            Uri localApkUri = intent.getData();
            downloadUri = intent.getParcelableExtra(Installer.EXTRA_DOWNLOAD_URI);
            installPackage(localApkUri);
        } else if (ACTION_UNINSTALL_PACKAGE.equals(action)) {
            uninstallPackageName = intent.getStringExtra(EXTRA_UNINSTALL_PACKAGE_NAME);

            uninstallPackage(uninstallPackageName);
        } else {
            throw new IllegalStateException("Intent action not specified!");
        }
    }

    @SuppressLint("InlinedApi")
    private void installPackage(Uri uri) {
        if (uri == null) {
            throw new RuntimeException("Set the data uri to point to an apk location!");
        }
        // https://code.google.com/p/android/issues/detail?id=205827
        if ((Build.VERSION.SDK_INT < 24)
                && (!uri.getScheme().equals("file"))) {
            throw new RuntimeException("PackageInstaller < Android N only supports file scheme!");
        }
        if ((Build.VERSION.SDK_INT >= 24)
                && (!uri.getScheme().equals("content"))) {
            throw new RuntimeException("PackageInstaller >= Android N only supports content scheme!");
        }

        Intent intent = new Intent();

        // Note regarding EXTRA_NOT_UNKNOWN_SOURCE:
        // works only when being installed as system-app
        // https://code.google.com/p/android/issues/detail?id=42253

        if (Build.VERSION.SDK_INT < 14) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        } else if (Build.VERSION.SDK_INT < 16) {
            intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(uri);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.putExtra(Intent.EXTRA_ALLOW_REPLACE, true);
        } else if (Build.VERSION.SDK_INT < 24) {
            intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(uri);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        } else { // Android N
            intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(uri);
            // grant READ permission for this content Uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_INSTALL);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException", e);
            installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED,
                    "This Android rom does not support ACTION_INSTALL_PACKAGE!");
            finish();
        }
        installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_STARTED);
    }

    private void uninstallPackage(String packageName) {
        // check that the package is installed
        try {
            getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException", e);
            installer.sendBroadcastUninstall(packageName, Installer.ACTION_UNINSTALL_INTERRUPTED,
                    "Package that is scheduled for uninstall is not installed!");
            finish();
            return;
        }

        Uri uri = Uri.fromParts("package", packageName, null);
        Intent intent = new Intent();
        intent.setData(uri);

        if (Build.VERSION.SDK_INT < 14) {
            intent.setAction(Intent.ACTION_DELETE);
        } else {
            intent.setAction(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_UNINSTALL);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException", e);
            installer.sendBroadcastUninstall(packageName, Installer.ACTION_UNINSTALL_INTERRUPTED,
                    "This Android rom does not support ACTION_UNINSTALL_PACKAGE!");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_INSTALL:
                /**
                 * resultCode is always 0 on Android < 4.0. See
                 * com.android.packageinstaller.PackageInstallerActivity: setResult is
                 * never executed on Androids < 4.0
                 */
                if (Build.VERSION.SDK_INT < 14) {
                    installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_COMPLETE);
                    break;
                }

                switch (resultCode) {
                    case Activity.RESULT_OK:
                        installer.sendBroadcastInstall(downloadUri,
                                Installer.ACTION_INSTALL_COMPLETE);
                        break;
                    case Activity.RESULT_CANCELED:
                        installer.sendBroadcastInstall(downloadUri,
                                Installer.ACTION_INSTALL_INTERRUPTED);
                        break;
                    case Activity.RESULT_FIRST_USER:
                    default:
                        // AOSP returns Activity.RESULT_FIRST_USER on error
                        installer.sendBroadcastInstall(downloadUri,
                                Installer.ACTION_INSTALL_INTERRUPTED,
                                getString(R.string.install_error_unknown));
                        break;
                }

                break;
            case REQUEST_CODE_UNINSTALL:
                // resultCode is always 0 on Android < 4.0.
                if (Build.VERSION.SDK_INT < 14) {
                    installer.sendBroadcastUninstall(uninstallPackageName,
                            Installer.ACTION_UNINSTALL_COMPLETE);
                    break;
                }

                switch (resultCode) {
                    case Activity.RESULT_OK:
                        installer.sendBroadcastUninstall(uninstallPackageName,
                                Installer.ACTION_UNINSTALL_COMPLETE);
                        break;
                    case Activity.RESULT_CANCELED:
                        installer.sendBroadcastUninstall(uninstallPackageName,
                                Installer.ACTION_UNINSTALL_INTERRUPTED);
                        break;
                    case Activity.RESULT_FIRST_USER:
                    default:
                        // AOSP UninstallAppProgress returns RESULT_FIRST_USER on error
                        installer.sendBroadcastUninstall(uninstallPackageName,
                                Installer.ACTION_UNINSTALL_INTERRUPTED,
                                getString(R.string.uninstall_error_unknown));
                        break;
                }

                break;
            default:
                throw new RuntimeException("Invalid request code!");
        }

        // after doing the broadcasts, finish this transparent wrapper activity
        finish();
    }

}
