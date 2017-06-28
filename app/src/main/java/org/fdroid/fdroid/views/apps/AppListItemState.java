package org.fdroid.fdroid.views.apps;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppPrefs;

public class AppListItemState {
    private final Context context;
    private final App app;
    private CharSequence mainText = null;
    private boolean showDownloadReady = false;
    private CharSequence actionButtonText = null;
    private int progressCurrent = -1;
    private int progressMax = -1;

    public AppListItemState(Context context, @NonNull App app) {
        this.app = app;
        this.context = context;
    }

    public AppListItemState setMainText(@NonNull CharSequence mainText) {
        this.mainText = mainText;
        return this;
    }

    public AppListItemState setShowDownloadReady() {
        this.showDownloadReady = true;
        return this;
    }

    public AppListItemState showActionButton(@NonNull CharSequence label) {
        actionButtonText = label;
        return this;
    }

    public AppListItemState setProgress(int progressCurrent, int progressMax) {
        this.progressCurrent = progressCurrent;
        this.progressMax = progressMax;
        return this;
    }

    @Nullable
    public CharSequence getMainText() {
        return mainText != null
                ? mainText
                : Utils.formatAppNameAndSummary(app.name, app.summary);
    }

    public boolean shouldShowInstall() {
        boolean installable = app.canAndWantToUpdate(context) || !app.isInstalled();
        boolean shouldAllow = app.compatible && !app.isFiltered();

        return installable && shouldAllow && !shouldShowActionButton() && !showProgress();
    }

    public boolean shouldShowDownloadReady() {
        return showDownloadReady;
    }

    public boolean shouldShowActionButton() {
        return actionButtonText != null;
    }

    public CharSequence getActionButtonText() {
        return actionButtonText;
    }

    public boolean showProgress() {
        return progressCurrent >= 0;
    }

    public boolean isProgressIndeterminate() {
        return progressMax <= 0;
    }

    public int getProgressCurrent() {
        return progressCurrent;
    }

    public int getProgressMax() {
        return progressMax;
    }

    /**
     * Sets the text/visibility of the {@link R.id#status} {@link TextView} based on whether the app:
     *  * Is compatible with the users device
     *  * Is installed
     *  * Can be updated
     */
    @Nullable
    public CharSequence getStatusText() {
        String statusText = null;
        if (!app.compatible) {
            statusText = context.getString(R.string.app_incompatible);
        } else if (app.isInstalled()) {
            if (app.canAndWantToUpdate(context)) {
                statusText = context.getString(R.string.app_version_x_available, app.getSuggestedVersionName());
            } else {
                statusText = context.getString(R.string.app_version_x_installed, app.installedVersionName);
            }
        }
        return statusText;
    }

    /**
     * Shows the currently installed version name, and whether or not it is the recommended version.
     */
    public CharSequence getInstalledVersionText() {
        int res = (app.suggestedVersionCode == app.installedVersionCode)
                ? R.string.app_recommended_version_installed
                : R.string.app_version_x_installed;

        return context.getString(res, app.installedVersionName);
    }

    /**
     * Shows whether the user has previously asked to ignore updates for this app entirely, or for a
     * specific version of this app. Binds to the {@link R.id#ignored_status} {@link TextView}.
     */
    @Nullable
    public CharSequence getIgnoredStatusText() {
        AppPrefs prefs = app.getPrefs(context);
        if (prefs.ignoreAllUpdates) {
            return context.getString(R.string.installed_app__updates_ignored);
        } else if (prefs.ignoreThisUpdate > 0 && prefs.ignoreThisUpdate == app.suggestedVersionCode) {
            return context.getString(
                    R.string.installed_app__updates_ignored_for_suggested_version,
                    app.getSuggestedVersionName());
        } else {
            return null;
        }
    }

}
