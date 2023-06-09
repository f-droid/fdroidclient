package org.fdroid.fdroid.views.updates.items;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerService;
import org.fdroid.fdroid.views.apps.AppListItemController;
import org.fdroid.fdroid.views.apps.AppListItemState;

/**
 * Tell the user that an app they have installed has a known vulnerability.
 * The role of this controller is to prompt the user what it is that should be done in response to this
 * (e.g. uninstall, update, disable).
 */
public class KnownVulnAppListItemController extends AppListItemController {
    private final Runnable refreshApps;

    KnownVulnAppListItemController(AppCompatActivity activity, Runnable refreshApps, View itemView) {
        super(activity, itemView);
        this.refreshApps = refreshApps;
    }

    @NonNull
    @Override
    protected AppListItemState getCurrentViewState(
            @NonNull App app, @Nullable AppUpdateStatusManager.AppUpdateStatus appStatus) {
        String mainText;
        String actionButtonText;

        if (shouldUpgradeInsteadOfUninstall(app)) {
            mainText = activity.getString(R.string.updates__app_with_known_vulnerability__prompt_upgrade, app.name);
            actionButtonText = activity.getString(R.string.menu_upgrade);
        } else {
            mainText = activity.getString(R.string.updates__app_with_known_vulnerability__prompt_uninstall, app.name);
            actionButtonText = activity.getString(R.string.menu_uninstall);
        }

        return new AppListItemState(app)
                .setMainText(mainText)
                .showActionButton(actionButtonText);
    }

    private boolean shouldUpgradeInsteadOfUninstall(@NonNull App app) {
        return app.installedVersionCode < app.autoInstallVersionCode;
    }

    @Override
    protected void onActionButtonPressed(@NonNull App app, Apk currentApk) {
        Apk installedApk = app.installedApk;
        if (installedApk == null) {
            throw new IllegalStateException(
                    "Tried to update or uninstall app with known vulnerability but it doesn't seem to be installed");
        }

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(activity);
        if (shouldUpgradeInsteadOfUninstall(app)) {
            manager.registerReceiver(installReceiver,
                    Installer.getInstallIntentFilter(currentApk.getCanonicalUrl()));
            InstallManagerService.queue(activity, app, currentApk);
        } else {
            manager.registerReceiver(installReceiver, Installer.getUninstallIntentFilter(app.packageName));
            InstallerService.uninstall(activity, app, installedApk);
        }
    }

    @Override
    public boolean canDismiss() {
        return false;
    }

    private void unregisterInstallReceiver() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(installReceiver);
    }

    /**
     * Trigger the LoaderManager in UpdatesAdapter to automatically requery for the list of
     * apps with known vulnerabilities (i.e. this app should no longer be in that list).
     */
    private void refreshUpdatesList() {
        refreshApps.run();
    }

    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_INSTALL_COMPLETE:
                case Installer.ACTION_UNINSTALL_COMPLETE:
                    refreshUpdatesList();
                    AppUpdateStatusManager.getInstance(context).checkForUpdates();
                    unregisterInstallReceiver();
                    break;

                case Installer.ACTION_INSTALL_INTERRUPTED:
                case Installer.ACTION_UNINSTALL_INTERRUPTED:
                    unregisterInstallReceiver();
                    break;

                case Installer.ACTION_INSTALL_USER_INTERACTION:
                case Installer.ACTION_UNINSTALL_USER_INTERACTION:
                    PendingIntent uninstallPendingIntent =
                            intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);

                    try {
                        uninstallPendingIntent.send();
                    } catch (PendingIntent.CanceledException ignored) {
                    }
                    break;
            }
        }
    };
}
