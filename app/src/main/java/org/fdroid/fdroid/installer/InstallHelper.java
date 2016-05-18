package org.fdroid.fdroid.installer;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.AndroidXMLDecompress;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.compat.FileCompat;
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.fdroid.privileged.install.InstallExtensionDialogActivity;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class InstallHelper {


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
    public static final String EXTRA_UNINSTALL_PACKAGE_NAME = "org.fdroid.fdroid.installer.InstallerService.extra.UNINSTALL_PACKAGE_NAME";

    public static final String EXTRA_USER_INTERACTION_PI = "org.fdroid.fdroid.installer.InstallerService.extra.USER_INTERACTION_PI";


    public static final String EXTRA_ERROR_MESSAGE = "org.fdroid.fdroid.net.Downloader.extra.ERROR_MESSAGE";



    public static SanitizedFile preparePackage(Context context, File apkFile, String packageName, String urlString)
            throws Installer.InstallFailedException {
        SanitizedFile apkToInstall;
        try {
            Map<String, Object> attributes = AndroidXMLDecompress.getManifestHeaderAttributes(apkFile.getAbsolutePath());

            /* This isn't really needed, but might as well since we have the data already */
//            if (attributes.containsKey("packageName") && !TextUtils.equals(packageName, (String) attributes.get("packageName"))) {
//                throw new Installer.InstallFailedException(apkFile + " has packageName that clashes with " + packageName);
//            }

            if (!attributes.containsKey("versionCode")) {
                throw new Installer.InstallFailedException(apkFile + " is missing versionCode!");
            }
            int versionCode = (Integer) attributes.get("versionCode");
//            Apk apk = ApkProvider.Helper.find(context, packageName, versionCode, new String[]{
//                    ApkProvider.DataColumns.HASH,
//                    ApkProvider.DataColumns.HASH_TYPE,
//            });
            /* Always copy the APK to the safe location inside of the protected area
             * of the app to prevent attacks based on other apps swapping the file
             * out during the install process. Most likely, apkFile was just downloaded,
             * so it should still be in the RAM disk cache */
            apkToInstall = SanitizedFile.knownSanitized(File.createTempFile("install-", ".apk", context.getFilesDir()));
            FileUtils.copyFile(apkFile, apkToInstall);
//            if (!verifyApkFile(apkToInstall, apk.hash, apk.hashType)) {
//                FileUtils.deleteQuietly(apkFile);
//                throw new Installer.InstallFailedException(apkFile + " failed to verify!");
//            }
            apkFile = null; // ensure this is not used now that its copied to apkToInstall

            // special case: F-Droid Privileged Extension
            if (packageName != null && packageName.equals(PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME)) {

                // extension must be signed with the same public key as main F-Droid
                // NOTE: Disabled for debug builds to be able to use official extension from repo
                ApkSignatureVerifier signatureVerifier = new ApkSignatureVerifier(context);
                if (!BuildConfig.DEBUG && !signatureVerifier.hasFDroidSignature(apkToInstall)) {
                    throw new Installer.InstallFailedException("APK signature of extension not correct!");
                }

                Activity activity = (Activity) context;
                Intent installIntent = new Intent(activity, InstallExtensionDialogActivity.class);
                installIntent.setAction(InstallExtensionDialogActivity.ACTION_INSTALL);
                installIntent.putExtra(InstallExtensionDialogActivity.EXTRA_INSTALL_APK, apkToInstall.getAbsolutePath());
                activity.startActivity(installIntent);
                return null;
            }

            // Need the apk to be world readable, so that the installer is able to read it.
            // Note that saving it into external storage for the purpose of letting the installer
            // have access is insecure, because apps with permission to write to the external
            // storage can overwrite the app between F-Droid asking for it to be installed and
            // the installer actually installing it.
            FileCompat.setReadable(apkToInstall, true);

            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(urlString.hashCode());
        } catch (NumberFormatException | IOException e) {
            throw new Installer.InstallFailedException(e);
        } catch (ClassCastException e) {
            throw new Installer.InstallFailedException("F-Droid Privileged can only be updated using an activity!");
        }

        return apkToInstall;
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
}
