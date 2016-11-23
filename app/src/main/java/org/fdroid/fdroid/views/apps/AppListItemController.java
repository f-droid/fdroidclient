package org.fdroid.fdroid.views.apps;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.installer.InstallManagerService;

public class AppListItemController extends RecyclerView.ViewHolder {

    private final Activity activity;

    private final Button installButton;
    private final ImageView icon;
    private final TextView name;
    private final TextView status;
    private final DisplayImageOptions displayImageOptions;

    private App currentApp;

    public AppListItemController(Activity activity, View itemView) {
        super(itemView);
        this.activity = activity;

        installButton = (Button) itemView.findViewById(R.id.install);
        installButton.setOnClickListener(onInstallClicked);

        icon = (ImageView) itemView.findViewById(R.id.icon);
        name = (TextView) itemView.findViewById(R.id.app_name);
        status = (TextView) itemView.findViewById(R.id.status);

        displayImageOptions = Utils.getImageLoadingOptions().build();

        itemView.setOnClickListener(onAppClicked);
    }

    public void bindModel(@NonNull App app) {
        currentApp = app;
        name.setText(Utils.formatAppNameAndSummary(app.name, app.summary));

        ImageLoader.getInstance().displayImage(app.iconUrl, icon, displayImageOptions);

        configureStatusText(app);
        configureInstallButton(app);
    }

    /**
     * Sets the text/visibility of the {@link R.id#status} {@link TextView} based on whether the app:
     *  * Is compatible with the users device
     *  * Is installed
     *  * Can be updated
     *
     * TODO: This button also needs to be repurposed to support the "Downloaded but not installed" state.
     */
    private void configureStatusText(@NonNull App app) {
        if (status == null) {
            return;
        }

        if (!app.compatible) {
            status.setText(activity.getString(R.string.app_incompatible));
            status.setVisibility(View.VISIBLE);
        } else if (app.isInstalled()) {
            if (app.canAndWantToUpdate(activity)) {
                String upgradeFromTo = activity.getString(R.string.app_version_x_available, app.getSuggestedVersionName());
                status.setText(upgradeFromTo);
            } else {
                String installed = activity.getString(R.string.app_version_x_installed, app.installedVersionName);
                status.setText(installed);
            }

            status.setVisibility(View.VISIBLE);
        } else {
            status.setVisibility(View.INVISIBLE);
        }

    }

    /**
     * The install button is shown when an app:
     *  * Is compatible with the users device.
     *  * Has not been filtered due to anti-features/root/etc.
     *  * Is either not installed or installed but can be updated.
     *
     * TODO: This button also needs to be repurposed to support the "Downloaded but not installed" state.
     */
    private void configureInstallButton(@NonNull App app) {
        if (installButton == null) {
            return;
        }

        boolean installable = app.canAndWantToUpdate(activity) || !app.isInstalled();
        boolean shouldAllow = app.compatible && !app.isFiltered();

        if (shouldAllow && installable) {
            installButton.setVisibility(View.VISIBLE);
        } else {
            installButton.setVisibility(View.GONE);
        }
    }

    private final View.OnClickListener onAppClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentApp == null) {
                return;
            }

            Intent intent = new Intent(activity, AppDetails.class);
            intent.putExtra(AppDetails.EXTRA_APPID, currentApp.packageName);
            if (Build.VERSION.SDK_INT >= 21) {
                Pair<View, String> iconTransitionPair = Pair.create((View) icon, activity.getString(R.string.transition_app_item_icon));
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, iconTransitionPair).toBundle();
                activity.startActivity(intent, bundle);
            } else {
                activity.startActivity(intent);
            }
        }
    };

    private final View.OnClickListener onInstallClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentApp == null) {
                return;
            }

            InstallManagerService.queue(activity, currentApp, ApkProvider.Helper.findApkFromAnyRepo(activity, currentApp.packageName, currentApp.suggestedVersionCode));
        }
    };
}
