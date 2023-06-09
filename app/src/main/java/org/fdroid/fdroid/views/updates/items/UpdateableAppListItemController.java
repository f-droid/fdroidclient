package org.fdroid.fdroid.views.updates.items;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.material.snackbar.Snackbar;

import org.fdroid.database.AppPrefs;
import org.fdroid.database.AppPrefsDao;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.views.apps.AppListItemController;
import org.fdroid.fdroid.views.apps.AppListItemState;
import org.fdroid.fdroid.views.updates.UpdatesAdapter;

/**
 * Very trimmed down list item. Only displays the app icon, name, and a download button.
 * We don't even need to show download progress, because the intention is that as soon as
 * we have started downloading the app, it is removed from the list (and replaced with an
 * {@link AppStatusListItemController}.
 */
public class UpdateableAppListItemController extends AppListItemController {
    UpdateableAppListItemController(AppCompatActivity activity, View itemView) {
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
    protected void onDismissApp(@NonNull final App app, UpdatesAdapter adapter) {
        AppPrefsDao appPrefsDao = DBHelper.getDb(activity).getAppPrefsDao();
        LiveData<AppPrefs> liveData = appPrefsDao.getAppPrefs(app.packageName);
        liveData.observe(activity, new Observer<AppPrefs>() {
            @Override
            public void onChanged(org.fdroid.database.AppPrefs appPrefs) {
                Utils.runOffUiThread(() -> {
                    AppPrefs newPrefs = appPrefs.toggleIgnoreVersionCodeUpdate(app.autoInstallVersionCode);
                    appPrefsDao.update(newPrefs);
                    return newPrefs;
                }, newPrefs -> {
                    showUndoSnackBar(appPrefsDao, newPrefs);
                    AppUpdateStatusManager.getInstance(activity).checkForUpdates();
                });
                liveData.removeObserver(this);
            }
        });
    }

    private void showUndoSnackBar(AppPrefsDao appPrefsDao, AppPrefs appPrefs) {
        Snackbar.make(
                        itemView,
                        R.string.app_list__dismiss_app_update,
                        Snackbar.LENGTH_LONG
                )
                .setAction(R.string.undo, view -> Utils.runOffUiThread(() -> {
                    AppPrefs newPrefs = appPrefs.toggleIgnoreVersionCodeUpdate(0);
                    appPrefsDao.update(newPrefs);
                    return true;
                }, result -> AppUpdateStatusManager.getInstance(activity).checkForUpdates()))
                .show();
    }
}
