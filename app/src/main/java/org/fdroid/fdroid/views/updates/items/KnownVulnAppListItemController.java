package org.fdroid.fdroid.views.updates.items;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
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
    public KnownVulnAppListItemController(Activity activity, View itemView) {
        super(activity, itemView);
    }

    @NonNull
    @Override
    protected AppListItemState getCurrentViewState(
            @NonNull App app, @Nullable AppUpdateStatusManager.AppUpdateStatus appStatus) {
        return new AppListItemState(app)
                .setMainText(activity.getString(R.string.updates__app_with_known_vulnerability__uninstall, app.name))
                .showActionButton(activity.getString(R.string.menu_uninstall));
    }

    @Override
    protected void onActionButtonPressed(@NonNull App app) {
        LocalBroadcastManager.getInstance(activity).registerReceiver(uninstallReceiver,
                Installer.getUninstallIntentFilter(app.packageName));
        InstallerService.uninstall(activity, app.getInstalledApk(activity));
    }

    private void unregisterUninstallReceiver() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(uninstallReceiver);
    }

    private final BroadcastReceiver uninstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_UNINSTALL_COMPLETE:
                    // This will cause the LoaderManager in UpdatesAdapter to automatically requery for the list of
                    // apps with known vulnerabilities (i.e. this app should no longer be in that list).
                    activity.getContentResolver().notifyChange(AppProvider.getInstalledWithKnownVulnsUri(), null);
                    unregisterUninstallReceiver();
                    break;

                case Installer.ACTION_UNINSTALL_INTERRUPTED:
                    unregisterUninstallReceiver();
                    break;

                case Installer.ACTION_UNINSTALL_USER_INTERACTION:
                    PendingIntent uninstallPendingIntent =
                            intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);

                    try {
                        uninstallPendingIntent.send();
                    } catch (PendingIntent.CanceledException ignored) { }
                    break;
            }
        }
    };
}
