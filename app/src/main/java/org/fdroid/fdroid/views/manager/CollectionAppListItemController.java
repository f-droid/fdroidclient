package org.fdroid.fdroid.views.manager;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.apps.AppListItemController;
import org.fdroid.fdroid.views.apps.AppListItemState;

import java.text.DateFormat;
import java.util.Set;


public class CollectionAppListItemController extends AppListItemController {

    private boolean boxVisibly = false;

    public CollectionAppListItemController(Activity activity, View itemView) {
        super(activity, itemView);
    }

    @NonNull
    @Override
    protected AppListItemState getCurrentViewState(
            @NonNull App app, @Nullable AppUpdateStatusManager.AppUpdateStatus appStatus) {

        if (boxVisibly) {
            Set<String> selectedApps = Preferences.get().getPanicTmpSelectedSet();
            return new AppListItemState(app)
                    .setCheckBoxStatus(selectedApps.contains(app.packageName));
        } else {
            return new AppListItemState(app)
                    .setStatusText(getInstalledVersion(app))
                    .setSecondaryStatusText(getIgnoreStatus(app));
        }
    }


    private CharSequence getInstalledVersion(@NonNull App app) {
        if (app.suggestedVersionName != null) {
            return activity.getString(
                    R.string.app_version_x_installed,
                    app.suggestedVersionName
            );
        }

        return null;
    }

    @Nullable
    private CharSequence getIgnoreStatus(@NonNull App app) {

        if (app.collectionLastModified != null) {
            DateFormat df = DateFormat.getDateTimeInstance();
            return activity.getString(
                    R.string.installed_layout_last_modified,
                    df.format(app.collectionLastModified)
            );
        }

        return null;
    }

    public void setBoxVisibly(boolean boxVisibly) {
        this.boxVisibly = boxVisibly;
    }

}
