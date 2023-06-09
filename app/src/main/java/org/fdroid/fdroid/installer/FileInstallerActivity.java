package org.fdroid.fdroid.installer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.net.DownloaderService;

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

    private App app;
    private Apk apk;
    private Uri localApkUri;
    /**
     * @see InstallManagerService
     */
    private Uri canonicalUri;

    private int act = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        Intent intent = getIntent();
        String action = intent.getAction();
        localApkUri = intent.getData();
        app = intent.getParcelableExtra(Installer.EXTRA_APP);
        apk = intent.getParcelableExtra(Installer.EXTRA_APK);
        installer = new FileInstaller(this, app, apk);
        if (ACTION_INSTALL_FILE.equals(action)) {
            canonicalUri = Uri.parse(intent.getStringExtra(DownloaderService.EXTRA_CANONICAL_URL));
            if (hasStoragePermission()) {
                installPackage(localApkUri, canonicalUri, apk);
            } else {
                requestPermission();
                act = 1;
            }
        } else if (ACTION_UNINSTALL_FILE.equals(action)) {
            canonicalUri = null;
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

        // pass the theme, it is not automatically applied due to activity's Theme.NoDisplay
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_App);
        builder.setMessage(R.string.app_permission_storage)
                .setPositiveButton(R.string.ok, (dialog, id) -> ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE))
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    if (act == 1) {
                        installer.sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED);
                    } else if (act == 2) {
                        installer.sendBroadcastUninstall(Installer.ACTION_UNINSTALL_INTERRUPTED);
                    }
                    finish();
                })
                .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) { // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (act == 1) {
                    installPackage(localApkUri, canonicalUri, apk);
                } else if (act == 2) {
                    uninstallPackage(apk);
                }
            } else {
                if (act == 1) {
                    installer.sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED);
                } else if (act == 2) {
                    installer.sendBroadcastUninstall(Installer.ACTION_UNINSTALL_INTERRUPTED);
                }
            }
            finish();
        }
    }

    private void installPackage(Uri localApkUri, Uri canonicalUri, Apk apk) {
        Utils.debugLog(TAG, "Installing: " + localApkUri.getPath());
        File path = apk.getInstalledMediaFile(activity.getApplicationContext());
        path.getParentFile().mkdirs();
        try {
            FileUtils.copyFile(new File(localApkUri.getPath()), path);
        } catch (IOException e) {
            Utils.debugLog(TAG, "Failed to copy: " + e.getMessage());
            installer.sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED);
        }
        if (apk.isMediaInstalled(activity.getApplicationContext())) { // Copying worked
            Utils.debugLog(TAG, "Copying worked: " + localApkUri.getPath());
            if (!postInstall(canonicalUri, apk, path)) {
                Toast.makeText(this, String.format(this.getString(R.string.app_installed_media), path),
                        Toast.LENGTH_LONG).show();
                installer.sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_COMPLETE);
            }
        } else {
            installer.sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED);
        }
        finish();
    }

    /**
     * Run any file-type-specific processes after the file has been copied into place.
     *
     * @return whether this handles sending the {@link Installer#ACTION_INSTALL_COMPLETE}
     * broadcast.
     */
    private boolean postInstall(Uri canonicalUri, Apk apk, File path) {
        if (path.getName().endsWith(".obf") || path.getName().endsWith(".obf.zip")) {
            ObfInstallerService.install(this, canonicalUri, app, apk, path);
            return true;
        }
        return false;
    }

    private void uninstallPackage(Apk apk) {
        if (apk.isMediaInstalled(activity.getApplicationContext())) {
            File file = apk.getInstalledMediaFile(activity.getApplicationContext());
            if (!file.delete()) {
                installer.sendBroadcastUninstall(Installer.ACTION_UNINSTALL_INTERRUPTED);
                return;
            }
        }
        installer.sendBroadcastUninstall(Installer.ACTION_UNINSTALL_COMPLETE);
        finish();
    }
}
