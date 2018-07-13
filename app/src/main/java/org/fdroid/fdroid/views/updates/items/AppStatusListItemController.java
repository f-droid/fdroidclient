package org.fdroid.fdroid.views.updates.items;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.AppUpdateStatusManager.AppUpdateStatus;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.apps.AppListItemController;
import org.fdroid.fdroid.views.apps.AppListItemState;
import org.fdroid.fdroid.views.updates.DismissResult;

/**
 * Shows apps which are:
 *  * In the process of being downloaded.
 *  * Downloaded and ready to install.
 *  * Recently installed and ready to run.
 */
public class AppStatusListItemController extends AppListItemController {
    public AppStatusListItemController(Activity activity, View itemView) {
        super(activity, itemView);
    }

    @NonNull
    @Override
    protected AppListItemState getCurrentViewState(@NonNull App app, @Nullable AppUpdateStatus appStatus) {

        return super.getCurrentViewState(app, appStatus)
                .setStatusText(getStatusText(appStatus));
    }

    @Nullable
    private CharSequence getStatusText(@Nullable AppUpdateStatus appStatus) {
        if (appStatus != null) {
            switch (appStatus.status) {
                case ReadyToInstall:
                    return activity.getString(R.string.app_list_download_ready);

                case Installed:
                    return activity.getString(R.string.notification_content_single_installed);
            }
        }

        return null;
    }

    @Override
    public boolean canDismiss() {
        return true;
    }

    @NonNull
    @Override
    protected DismissResult onDismissApp(@NonNull App app) {
        AppUpdateStatus status = getCurrentStatus();
        CharSequence message = null;
        if (status != null) {
            AppUpdateStatusManager manager = AppUpdateStatusManager.getInstance(activity);
            manager.removeApk(status.getUniqueKey());
            switch (status.status) {
                case Downloading:
                    cancelDownload();
                    message = activity.getString(R.string.app_list__dismiss_downloading_app);
                    break;
            }
        }

        return new DismissResult(message, true);
    }
}
