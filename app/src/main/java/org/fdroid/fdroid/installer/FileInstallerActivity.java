package org.fdroid.fdroid.installer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;

import java.io.File;
import java.io.IOException;

public class FileInstallerActivity extends FragmentActivity {

    private static final String TAG = "FileInstallerActivity";
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 1;

    static final String ACTION_INSTALL_FILE
            = "org.fdroid.fdroid.installer.FileInstaller.action.INSTALL_PACKAGE";
    static final String ACTION_UNINSTALL_FILE
            = "org.fdroid.fdroid.installer.FileInstaller.action.UNINSTALL_PACKAGE";

    private FileInstallerActivity activity;

    // for the broadcasts
    private FileInstaller installer;

    private Apk apk;
    private Uri localApkUri;
    private Uri downloadUri;

    private int act = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        Intent intent = getIntent();
        String action = intent.getAction();
        localApkUri = intent.getData();
        downloadUri = intent.getParcelableExtra(Installer.EXTRA_DOWNLOAD_URI);
        apk = intent.getParcelableExtra(Installer.EXTRA_APK);
        installer = new FileInstaller(this, apk);
        if (ACTION_INSTALL_FILE.equals(action)) {
            if (hasStoragePermission()) {
                installPackage(localApkUri, downloadUri, apk);
            } else {
                requestPermission();
                act = 1;
            }
        } else if (ACTION_UNINSTALL_FILE.equals(action)) {
            if (hasStoragePermission()) {
                uninstallPackage(apk);
            } else {
                requestPermission();
                act = 2;
            }
        } else {
            throw new IllegalStateException("Intent action not specified!");
        }

    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (!hasStoragePermission()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showDialog();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE);
            }
        }
    }

    private void showDialog() {

        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        final AlertDialog.Builder builder = new AlertDialog.Builder(theme);
        builder.setMessage(R.string.app_permission_storage)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_STORAGE);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (act == 1) {
                            installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED);
                        } else if (act == 2) {
                            installer.sendBroadcastUninstall(Installer.ACTION_UNINSTALL_INTERRUPTED);
                        }
                        finish();
                    }
                })
                .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (act == 1) {
                        installPackage(localApkUri, downloadUri, apk);
                    } else if (act == 2) {
                        uninstallPackage(apk);
                    }
                } else {
                    if (act == 1) {
                        installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED);
                    } else if (act == 2) {
                        installer.sendBroadcastUninstall(Installer.ACTION_UNINSTALL_INTERRUPTED);
                    }
                }
                finish();
        }
    }

    private void installPackage(Uri localApkUri, Uri downloadUri, Apk apk) {
        Utils.debugLog(TAG, "Installing: " + localApkUri.getPath());
        File path = apk.getMediaInstallPath(activity.getApplicationContext());
        path.mkdirs();
        try {
            FileUtils.copyFileToDirectory(new File(localApkUri.getPath()), path);
        } catch (IOException e) {
            Utils.debugLog(TAG, "Failed to copy: " + e.getMessage());
            installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED);
        }
        if (apk.isMediaInstalled(activity.getApplicationContext())) { // Copying worked
            Utils.debugLog(TAG, "Copying worked: " + localApkUri.getPath());
            Toast.makeText(this, String.format(this.getString(R.string.app_installed_media), path.toString()),
                    Toast.LENGTH_LONG).show();
            installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_COMPLETE);
        } else {
            installer.sendBroadcastInstall(downloadUri, Installer.ACTION_INSTALL_INTERRUPTED);
        }
        finish();
    }

    private void uninstallPackage(Apk apk) {
        if (apk.isMediaInstalled(activity.getApplicationContext())) {
            File file = new File(apk.getMediaInstallPath(activity.getApplicationContext()), apk.apkName);
            if (!file.delete()) {
                installer.sendBroadcastUninstall(Installer.ACTION_UNINSTALL_INTERRUPTED);
                return;
            }
        }
        installer.sendBroadcastUninstall(Installer.ACTION_UNINSTALL_COMPLETE);
        finish();
    }
}
