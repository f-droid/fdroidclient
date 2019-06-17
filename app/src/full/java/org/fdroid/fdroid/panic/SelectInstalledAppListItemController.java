package org.fdroid.fdroid.panic;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.apps.AppListItemState;
import org.fdroid.fdroid.views.installed.InstalledAppListItemController;

import java.util.Set;

/**
 * Shows the currently installed apps as a selectable list.
 */
public class SelectInstalledAppListItemController extends InstalledAppListItemController {

    private final Set<String> selectedApps;

    public SelectInstalledAppListItemController(Activity activity, View itemView, Set<String> selectedApps) {
        super(activity, itemView);
        this.selectedApps = selectedApps;
    }

    @NonNull
    @Override
    protected AppListItemState getCurrentViewState(
            @NonNull App app, @Nullable AppUpdateStatusManager.AppUpdateStatus appStatus) {
        return new AppListItemState(app).setCheckBoxStatus(selectedApps.contains(app.packageName));
    }

    @Override
    protected void onActionButtonPressed(App app) {
        super.onActionButtonPressed(app);
    }
}
