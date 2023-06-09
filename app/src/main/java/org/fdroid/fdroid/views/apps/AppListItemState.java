package org.fdroid.fdroid.views.apps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;

/**
 * A dumb model which is used to specify what should/should not be shown  in an {@link AppListItemController}.
 *
 * @see AppListItemController and its subclasses.
 */
public class AppListItemState {
    private final App app;
    private CharSequence mainText = null;
    private CharSequence actionButtonText = null;
    private CharSequence secondaryButtonText = null;
    private CharSequence statusText = null;
    private CharSequence secondaryStatusText = null;
    private int progressCurrent = -1;
    private int progressMax = -1;
    private boolean showInstallButton;
    private boolean showCheckBox;
    private boolean checkBoxChecked;

    public AppListItemState(@NonNull App app) {
        this.app = app;
    }

    public AppListItemState setMainText(CharSequence mainText) {
        this.mainText = mainText;
        return this;
    }

    public AppListItemState showActionButton(CharSequence label) {
        actionButtonText = label;
        return this;
    }

    public AppListItemState showSecondaryButton(CharSequence label) {
        secondaryButtonText = label;
        return this;
    }

    public AppListItemState setStatusText(CharSequence text) {
        this.statusText = text;
        return this;
    }

    public AppListItemState setSecondaryStatusText(CharSequence text) {
        this.secondaryStatusText = text;
        return this;
    }

    public AppListItemState setProgress(int progressCurrent, int progressMax) {
        this.progressCurrent = progressCurrent;
        this.progressMax = progressMax;
        return this;
    }

    public AppListItemState setShowInstallButton(boolean show) {
        this.showInstallButton = show;
        return this;
    }

    @Nullable
    public CharSequence getMainText() {
        if (showCheckBox) {
            return app.name;
        }
        return mainText != null
                ? mainText
                : Utils.formatAppNameAndSummary(app.name, app.summary);
    }

    public boolean shouldShowInstall() {
        return showInstallButton;
    }

    public boolean shouldShowActionButton() {
        return actionButtonText != null;
    }

    public CharSequence getActionButtonText() {
        return actionButtonText;
    }

    public boolean shouldShowSecondaryButton() {
        return secondaryButtonText != null;
    }

    public CharSequence getSecondaryButtonText() {
        return secondaryButtonText;
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

    @Nullable
    public CharSequence getStatusText() {
        return statusText;
    }

    @Nullable
    public CharSequence getSecondaryStatusText() {
        return secondaryStatusText;
    }

    public boolean shouldShowCheckBox() {
        return showCheckBox;
    }

    public boolean isCheckBoxChecked() {
        return checkBoxChecked;
    }

    /**
     * Enable the {@link android.widget.CheckBox} display and set the on/off status
     * e.g. {@link android.widget.CheckBox#isChecked()}
     */
    public AppListItemState setCheckBoxStatus(boolean checked) {
        this.showCheckBox = true;
        this.checkBoxChecked = checked;
        return this;
    }

}
