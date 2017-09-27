package org.fdroid.fdroid.views.updates.items;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppPrefs;
import org.fdroid.fdroid.data.AppPrefsProvider;
import org.fdroid.fdroid.views.apps.AppListItemController;
import org.fdroid.fdroid.views.apps.AppListItemState;
import org.fdroid.fdroid.views.updates.DismissResult;

/**
 * Very trimmed down list item. Only displays the app icon, name, and a download button.
 * We don't even need to show download progress, because the intention is that as soon as
 * we have started downloading the app, it is removed from the list (and replaced with an
 * {@link AppStatusListItemController}.
 */
public class UpdateableAppListItemController extends AppListItemController {
    public UpdateableAppListItemController(Activity activity, View itemView) {
        super(activity, itemView);
    }

    @NonNull
    @Override
    protected AppListItemState getCurrentViewState(
            @NonNull App app, @Nullable AppUpdateStatusManager.AppUpdateStatus appStatus) {
        return new AppListItemState(app)
                .setShowInstallButton(true);
    }

    @Override
    public boolean canDismiss() {
        return true;
    }

    @Override
    @NonNull
    protected DismissResult onDismissApp(@NonNull App app) {
        AppPrefs prefs = app.getPrefs(activity);
        prefs.ignoreThisUpdate = app.suggestedVersionCode;

        // The act of updating here will trigger a re-query of the "can update" apps, so no need to do anything else
        // to update the UI in response to this.
        AppPrefsProvider.Helper.update(activity, app, prefs);
        return new DismissResult(activity.getString(R.string.app_list__dismiss_app_update), false);
    }
}
