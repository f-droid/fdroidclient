package org.fdroid.fdroid.installer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.ObjectsCompat;
import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.io.IOUtils;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.net.DownloaderService;

import java.io.InputStream;
import java.io.OutputStream;

public class SessionInstallManager extends BroadcastReceiver {

    private static final String TAG = "SessionInstallManager";
    private static final String INSTALLER_ACTION_INSTALL =
            "org.fdroid.fdroid.installer.SessionInstallManager.install";
    private static final String INSTALLER_ACTION_UNINSTALL =
            "org.fdroid.fdroid.installer.SessionInstallManager.uninstall";
    /**
     * An intent extra needed only due to a bug in Android 12 (#2599) where our App parcelable in the confirmation
     * intent causes a crash.
     * To prevent this, we wrap the App and Apk parcelables in this bundle.
     */
    private static final String EXTRA_BUNDLE =
            "org.fdroid.fdroid.installer.SessionInstallManager.bundle";

    private final Context context;

    public SessionInstallManager(Context context) {
        this.context = context;
        context.registerReceiver(this, new IntentFilter(INSTALLER_ACTION_INSTALL));
        context.registerReceiver(this, new IntentFilter(INSTALLER_ACTION_UNINSTALL));
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        // abandon old sessions, because there's a limit
        // that will throw IllegalStateException when we try to open new sessions
        Utils.runOffUiThread(() -> {
            for (PackageInstaller.SessionInfo session : installer.getMySessions()) {
                Utils.debugLog(TAG, "Abandon session " + session.getSessionId());
                installer.abandonSession(session.getSessionId());
            }
        });
    }

    @WorkerThread
    public void install(App app, Apk apk, Uri localApkUri, Uri canonicalUri) {
        DocumentFile documentFile = ObjectsCompat.requireNonNull(DocumentFile.fromSingleUri(context, localApkUri));
        long size = documentFile.length();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(app.packageName);
        params.setSize(size);
        if (Build.VERSION.SDK_INT >= 31) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            params.setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE);
        }
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        try {
            int sessionId = installer.createSession(params);
            ContentResolver contentResolver = context.getContentResolver();
            try (PackageInstaller.Session session = installer.openSession(sessionId)) {
                try (InputStream inputStream = contentResolver.openInputStream(localApkUri)) {
                    try (OutputStream outputStream = session.openWrite(app.packageName, 0, size)) {
                        IOUtils.copy(inputStream, outputStream);
                        session.fsync(outputStream);
                    }
                }
                session.commit(getInstallIntentSender(sessionId, app, apk, canonicalUri));
            }
        } catch (Exception e) {
            Log.e(TAG, "I/O Error during install session: ", e);
            Installer.sendBroadcastInstall(context, canonicalUri, Installer.ACTION_INSTALL_INTERRUPTED, app, apk,
                    null, e.getLocalizedMessage());
        }
    }

    @WorkerThread
    public void uninstall(String packageName) {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        installer.uninstall(packageName, getUninstallIntentSender(packageName));
    }

    private IntentSender getInstallIntentSender(int sessionId, App app, Apk apk, Uri canonicalUri) {
        Intent broadcastIntent = new Intent(INSTALLER_ACTION_INSTALL);
        broadcastIntent.setPackage(context.getPackageName());
        broadcastIntent.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Installer.EXTRA_APP, app);
        bundle.putParcelable(Installer.EXTRA_APK, apk);
        broadcastIntent.putExtra(EXTRA_BUNDLE, bundle);
        broadcastIntent.putExtra(DownloaderService.EXTRA_CANONICAL_URL, canonicalUri);
        // we are stuffing this intent pretty full, hopefully won't run into the size limit
        broadcastIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        // intent flag needs to be mutable, otherwise the intent has no extras
        int flags = Build.VERSION.SDK_INT >= 31 ?
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE :
                PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, broadcastIntent, flags);
        return pendingIntent.getIntentSender();
    }

    private IntentSender getUninstallIntentSender(String packageName) {
        Intent broadcastIntent = new Intent(INSTALLER_ACTION_UNINSTALL);
        broadcastIntent.setPackage(context.getPackageName());
        broadcastIntent.putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName);
        broadcastIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        // intent flag needs to be mutable, otherwise the intent has no extras
        int flags = Build.VERSION.SDK_INT >= 31 ?
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE :
                PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context, packageName.hashCode(), broadcastIntent, flags);
        return pendingIntent.getIntentSender();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (INSTALLER_ACTION_INSTALL.equals(intent.getAction())) {
            onInstallReceived(intent);
        } else if (INSTALLER_ACTION_UNINSTALL.equals(intent.getAction())) {
            onUninstallReceived(intent);
        } else {
            throw new IllegalStateException("Unsupported broadcast action: " + intent.getAction());
        }
    }

    private void onInstallReceived(Intent intent) {
        int sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
        Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);

        Bundle bundle = intent.getBundleExtra(EXTRA_BUNDLE);
        App app = bundle.getParcelable(Installer.EXTRA_APP);
        Apk apk = bundle.getParcelable(Installer.EXTRA_APK);
        Uri canonicalUri = intent.getParcelableExtra(DownloaderService.EXTRA_CANONICAL_URL);

        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Integer.MIN_VALUE);
        String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        Log.i(TAG, "Received install broadcast for " + app.packageName + " " + status + ": " + msg);

        if (status == PackageInstaller.STATUS_SUCCESS) {
            String action = Installer.ACTION_INSTALL_COMPLETE;
            Installer.sendBroadcastInstall(context, canonicalUri, action, app, apk, null, null);
        } else if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            int flags = Build.VERSION.SDK_INT >= 31 ?
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                    PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pendingIntent = PendingIntent.getActivity(context, sessionId, confirmIntent, flags);
            String action = Installer.ACTION_INSTALL_USER_INTERACTION;
            Installer.sendBroadcastInstall(context, canonicalUri, action, app, apk, pendingIntent, null);
        } else {
            String action = Installer.ACTION_INSTALL_INTERRUPTED;
            Installer.sendBroadcastInstall(context, canonicalUri, action, app, apk, null, msg);
        }
    }

    private void onUninstallReceived(Intent intent) {
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
        Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Integer.MIN_VALUE);
        String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        Log.i(TAG, "Received uninstall broadcast for " + packageName + " " + status + ": " + msg);

        if (status == PackageInstaller.STATUS_SUCCESS) {
            String action = Installer.ACTION_UNINSTALL_COMPLETE;
            sendBroadcastUninstall(packageName, action, null, null);
        } else if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            int flags = Build.VERSION.SDK_INT >= 31 ?
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                    PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(context, packageName.hashCode(), confirmIntent, flags);
            String action = Installer.ACTION_UNINSTALL_USER_INTERACTION;
            sendBroadcastUninstall(packageName, action, pendingIntent, null);
        } else {
            String action = Installer.ACTION_UNINSTALL_INTERRUPTED;
            sendBroadcastUninstall(packageName, action, null, msg);
        }
    }

    private void sendBroadcastUninstall(String packageName, String action, @Nullable PendingIntent pendingIntent,
                                        @Nullable String errorMessage) {
        App app = new App();
        app.packageName = packageName;
        Apk apk = new Apk();
        apk.packageName = packageName;
        Installer.sendBroadcastUninstall(context, app, apk, action, pendingIntent, errorMessage);
    }

    /**
     * Returns true if the {@link SessionInstaller} can be used on this device.
     */
    public static boolean canBeUsed(Context context) {
        // We could use the SessionInstaller also with the full flavor,
        // but for now we limit it to basic to limit potential damage.
        if (!BuildConfig.FLAVOR.equals("basic")) return false;
        // We could use the SessionInstaller also on lower versions,
        // but the benefit of unattended updates only starts with SDK 31.
        // Before the extra bugs it has aren't worth it.
        if (Build.VERSION.SDK_INT < 31) return false;
        // Xiaomi MIUI (at least in version 12) is known to break the PackageInstaller API in several ways.
        // Disabling MIUI "optimizations" in developer options fixes it, but we can't ask users to do this (bad UX).
        // Therefore, we have no choice, but to disable it completely for those devices.
        // See: https://github.com/vvb2060/PackageInstallerTest
        if (isXiaomiDevice()) return false;
        // We don't use SessionInstaller, if PrivilegedInstaller can be used instead.
        // This is the last check, because it is the most expensive one
        // getting PackageInfo and doing service binding.
        return !PrivilegedInstaller.isDefault(context);
    }

    private static boolean isXiaomiDevice() {
        return "Xiaomi".equalsIgnoreCase(Build.BRAND) || "Redmi".equalsIgnoreCase(Build.BRAND);
    }

    public static boolean isTargetSdkSupported(int targetSdk) {
        if (Build.VERSION.SDK_INT < 31) return false; // not supported below Android 12
        if (Build.VERSION.SDK_INT == 31 && targetSdk >= 29) return true;
        if (Build.VERSION.SDK_INT == 32 && targetSdk >= 29) return true;
        if (Build.VERSION.SDK_INT == 33 && targetSdk >= 30) return true;
        // This needs to be adjusted as new Android versions are released
        // https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)
        // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r42:frameworks/base/services/core/java/com/android/server/pm/PackageInstallerSession.java;l=2095;drc=6aba151873bfae198ef9eceb10f943e18b52d58c
        // TODO check targetSdk Android 14 sources have been published, just a guess so far
        return Build.VERSION.SDK_INT == 34 && targetSdk >= 31;
    }

}
