package org.fdroid.fdroid.installer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

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
        return SessionInstallManager.canBeUsed(context);
    }

}
