package org.fdroid.fdroid.views.apps;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

/**
 * Used for search results or for category lists.
 * Shows an inline download button, and also (if appropriate):
 *  * Whether the app is incompatible.
 *  * Version that app can be upgraded to.
 *  * Installed version.
 */
public class StandardAppListItemController extends AppListItemController {
    public StandardAppListItemController(Activity activity, View itemView) {
        super(activity, itemView);
    }

    @NonNull
    @Override
    protected AppListItemState getCurrentViewState(
            @NonNull App app, @Nullable AppUpdateStatusManager.AppUpdateStatus appStatus) {

        return super.getCurrentViewState(app, appStatus)
                .setStatusText(getStatusText(app))
                .setShowInstallButton(shouldShowInstall(app));
    }

    @Nullable
    private CharSequence getStatusText(@NonNull App app) {
        if (!app.compatible) {
            return activity.getString(R.string.app_incompatible);
        } else if (app.isInstalled(activity.getApplicationContext())) {
            if (app.canAndWantToUpdate(activity)) {
                return activity.getString(R.string.app_version_x_available, app.getSuggestedVersionName());
            } else {
                return activity.getString(R.string.app_version_x_installed, app.installedVersionName);
            }
        }

        return null;
    }

    private boolean shouldShowInstall(@NonNull App app) {
        boolean installable = app.canAndWantToUpdate(activity) || !app.isInstalled(activity.getApplicationContext());
        boolean shouldAllow = app.compatible && !app.isFiltered();

        return installable && shouldAllow;
    }
}
