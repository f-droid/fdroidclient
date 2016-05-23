package org.fdroid.fdroid.installer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.privileged.install.InstallExtensionDialogActivity;

import java.io.File;

public class DefaultInstaller extends Installer {

    private static final String TAG = "DefaultInstaller";

    DefaultInstaller(Context context) {
        super(context);
    }

    @Override
    protected void installPackage(Uri uri, Uri originatingUri, String packageName) {

        sendBroadcastInstall(uri, originatingUri, Installer.ACTION_INSTALL_STARTED);

        Utils.debugLog(TAG, "ACTION_INSTALL uri: " + uri + " file: " + new File(uri.getPath()));

        // TODO: rework for uri
        File sanitizedFile = null;
        try {
            sanitizedFile = Installer.prepareApkFile(mContext, new File(uri.getPath()), packageName);
        } catch (Installer.InstallFailedException e) {
            e.printStackTrace();
        }
        Uri sanitizedUri = Uri.fromFile(sanitizedFile);

        Utils.debugLog(TAG, "ACTION_INSTALL sanitizedUri: " + sanitizedUri);

        Intent installIntent;
        // special case: F-Droid Privileged Extension
        if (packageName != null && packageName.equals(PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME)) {

            // extension must be signed with the same public key as main F-Droid
            // NOTE: Disabled for debug builds to be able to use official extension from repo
            ApkSignatureVerifier signatureVerifier = new ApkSignatureVerifier(mContext);
            if (!BuildConfig.DEBUG && !signatureVerifier.hasFDroidSignature(sanitizedFile)) {
                throw new RuntimeException("APK signature of extension not correct!");
            }

            installIntent = new Intent(mContext, InstallExtensionDialogActivity.class);
            installIntent.setAction(InstallExtensionDialogActivity.ACTION_INSTALL);
            installIntent.putExtra(InstallExtensionDialogActivity.EXTRA_INSTALL_APK,
                    sanitizedFile.getAbsolutePath());
        } else {
            installIntent = new Intent(mContext, AndroidInstallerActivity.class);
            installIntent.setAction(AndroidInstallerActivity.ACTION_INSTALL_PACKAGE);
            installIntent.putExtra(AndroidInstallerActivity.EXTRA_ORIGINATING_URI, originatingUri);
            installIntent.setData(sanitizedUri);
        }

        PendingIntent installPendingIntent = PendingIntent.getActivity(
                mContext.getApplicationContext(),
                uri.hashCode(),
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastInstall(uri, originatingUri,
                Installer.ACTION_INSTALL_USER_INTERACTION, installPendingIntent);

    }

    @Override
    protected void uninstallPackage(String packageName) {
        sendBroadcastUninstall(packageName, Installer.ACTION_UNINSTALL_STARTED);

        Intent uninstallIntent;
        // special case: F-Droid Privileged Extension
        if (packageName != null && packageName.equals(PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME)) {
            uninstallIntent = new Intent(mContext, InstallExtensionDialogActivity.class);
            uninstallIntent.setAction(InstallExtensionDialogActivity.ACTION_UNINSTALL);
        } else {
            uninstallIntent = new Intent(mContext, AndroidInstallerActivity.class);
            uninstallIntent.setAction(AndroidInstallerActivity.ACTION_UNINSTALL_PACKAGE);
            uninstallIntent.putExtra(
                    AndroidInstallerActivity.EXTRA_UNINSTALL_PACKAGE_NAME, packageName);
        }
        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(
                mContext.getApplicationContext(),
                packageName.hashCode(),
                uninstallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        sendBroadcastUninstall(packageName,
                Installer.ACTION_UNINSTALL_USER_INTERACTION, uninstallPendingIntent);
    }
}
