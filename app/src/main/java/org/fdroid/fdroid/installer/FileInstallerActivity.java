package org.fdroid.fdroid.installer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

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
        canonicalUri = intent.getParcelableExtra(org.fdroid.fdroid.net.Downloader.EXTRA_CANONICAL_URL);
        apk = intent.getParcelableExtra(Installer.EXTRA_APK);
        installer = new FileInstaller(this, apk);
        if (ACTION_INSTALL_FILE.equals(action)) {
            if (hasStoragePermission()) {
                installPackage(localApkUri, canonicalUri, apk);
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
                            installer.sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED);
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
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_STORAGE:
                // If request is cancelled, the result arrays are empty.
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
            Toast.makeText(this, String.format(this.getString(R.string.app_installed_media), path.toString()),
                    Toast.LENGTH_LONG).show();
            installer.sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_COMPLETE);
            postInstall(path);
        } else {
            installer.sendBroadcastInstall(canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED);
        }
        finish();
    }

    /**
     * Run any file-type-specific processes after the file has been copied into place.
     * <p>
     * When this was written, OsmAnd only supported importing OBF files via a
     * {@code file:///} URL, so this disables {@link android.os.FileUriExposedException}.
     */
    private void postInstall(File path) {
        if (path.getName().endsWith(".obf")) {
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                    m.invoke(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("obf");
            intent.setDataAndType(Uri.fromFile(path), mimeType);
            if (Build.VERSION.SDK_INT >= 23) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Log.i(TAG, "No Activity available to handle " + intent);
            }
        }
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
