package org.fdroid.fdroid.installer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;

class SessionInstaller extends Installer {

    private final SessionInstallManager sessionInstallManager = FDroidApp.sessionInstallManager;

    SessionInstaller(Context context, @NonNull App app, @NonNull Apk apk) {
        super(context, app, apk);
    }

    @Override
    protected void installPackageInternal(Uri localApkUri, Uri canonicalUri) {
        sessionInstallManager.install(app, apk, localApkUri, canonicalUri);
    }

    @Override
    protected void uninstallPackage() {
        sessionInstallManager.uninstall(app.packageName);
    }

    @Override
    public Intent getUninstallScreen() {
        // we handle uninstall on our own, no need for special screen
        return null;
    }

    @Override
    protected boolean isUnattended() {
        // may not always be unattended, but no easy way to find out up-front
        return canBeUsed();
    }

    /**
     * Returns true if the {@link SessionInstaller} can be used on this device.
     */
    public static boolean canBeUsed() {
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
        return !isXiaomiDevice();
    }

    private static boolean isXiaomiDevice() {
        return "Xiaomi".equalsIgnoreCase(Build.BRAND) || "Redmi".equalsIgnoreCase(Build.BRAND);
    }
}
